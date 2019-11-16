### ReentrantReadWriteLock（基于 1.8）

实现了 ReadWriteLock 接口。

1. ReentrantReadWriteLock 中已经获取到 writeLock 的时候可以获取 readLock，但是已经获取到 readLock 的情况下不允许同一线程再获取 writeLock，需要先释放 readLock；
2. 只有 writeLock 支持条件变量，readLock 是不支持条件变量的，readLock 调用 `newCondition()` 会抛出 `UnsupportedOperationException`。

在初始化时设置是否采用公平锁：`public ReentrantReadWriteLock(boolean fair)`，*默认使用非公平锁*。

通过将 int 类型的 state 划分为两部分来分别代表读锁和写锁的状态（低 16 位表示写锁的持有数，高 16 位为读锁的持有数） 
```java
static final int SHARED_SHIFT   = 16;
static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

/** 将 state 右移 16 位得到读锁计数  */
static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
/** 返回低 16 位为写锁计数  */
static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
```

内部实现了 AQS 的子类 Sync，並提供了 FairSync 和 NonfairSync 两种策略类。另外提供实现了 Lock 接口的 ReadLock 和 WriteLock 作为读锁和写锁的实现类。

通过 `reentrantReadWriteLock.readLock()` 来获取关联的 ReadLock 实例，通过 `reentrantReadWriteLock.writeLock()` 来获取关联的 WriteLock 实例。两者都是通过调用 `lock()` 来加锁，调用 `unlock()` 释放持有的锁。

当尝试加读锁，如果此时有*其他*线程正持有写锁，加读锁的线程会阻塞直到写锁被释放；

当尝试加写锁时，如果此时有*其他*线程正持有写锁，当前线程会阻塞直至该写锁被释放；或者此时其他线程正持有读锁（**即使是当前线程持有读锁**），当前线程会阻塞直到该读锁被释放。

允许持有写锁的线程多次获取同一写锁（可重入）；允许多个线程同时获取读锁（不论是不是已经获取过读锁的线程）。

另外，**为了防止写锁在多次读锁之后的饥饿现象，当使用非公平策略，在加一个读锁时，如果当前第一个排队线程是要加写锁的话，就会将加读锁线程加入排队**。

- 加读锁操作调用的是 AQS 的 acquireShared，也就是调用了子类的 tryAcquireShared
```java
/**
 * The hold count of the last thread to successfully acquire
 * readLock. This saves ThreadLocal lookup in the common case
 * where the next thread to release is the last one to
 * acquire. This is non-volatile since it is just used
 * as a heuristic, and would be great for threads to cache.
 *
 * <p>Can outlive the Thread for which it is caching the read
 * hold count, but avoids garbage retention by not retaining a
 * reference to the Thread.
 *
 * <p>Accessed via a benign data race; relies on the memory
 * model's final field and out-of-thin-air guarantees.
 */
private transient HoldCounter cachedHoldCounter;
/**
 * The number of reentrant read locks held by current thread.
 * Initialized only in constructor and readObject.
 * Removed whenever a thread's read hold count drops to 0.
 */
private transient ThreadLocalHoldCounter readHolds;

protected final int tryAcquireShared(int unused) {
    /*
     * Walkthrough:
     * 1. If write lock held by another thread, fail.
     * 2. Otherwise, this thread is eligible for
     *    lock wrt state, so ask if it should block
     *    because of queue policy. If not, try
     *    to grant by CASing state and updating count.
     *    Note that step does not check for reentrant
     *    acquires, which is postponed to full version
     *    to avoid having to check hold count in
     *    the more typical non-reentrant case.
     * 3. If step 2 fails either because thread
     *    apparently not eligible or CAS fails or count
     *    saturated, chain to version with full retry loop.
     */
    Thread current = Thread.currentThread();
    int c = getState();
    // 当前写锁被持有且不是自己。
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    int r = sharedCount(c);
    // readerShouldBlock 用于实现不同的竞争策略：公平/非公平
    // 给读锁数+1
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        // 如果是第一个获取读锁的线程，将其记录到 firstReader，将其获取读锁数记录到 firstReaderHoldCount
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            // 后续获取读锁的线程，通过 ThreadLocal 来记录他们的读锁持有锁
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                // 如果 readHolds 里没有当前线程的记录，则会调用 initValue 方法，因为没有调用过 set() 方法。
                cachedHoldCounter = rh = readHolds.get();
            // 当前线程没有占有读锁的话，count 为 0。
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    // 循环重试
    return fullTryAcquireShared(current);
}

// 释放读锁
protected final boolean tryReleaseShared(int unused) {
    // 如果当前线程是第一个持有读锁的线程，则修改 firstReader 和 firstReaderHoldCount
    Thread current = Thread.currentThread();
    if (firstReader == current) {
        // assert firstReaderHoldCount > 0;
        if (firstReaderHoldCount == 1)
            firstReader = null;
        else
            firstReaderHoldCount--;
    } else {
        // 否则去修改 HoldCounter
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current))
            rh = readHolds.get();
        int count = rh.count;
        if (count <= 1) {
            readHolds.remove();
            if (count <= 0)
                throw unmatchedUnlockException();
        }
        --rh.count;
    }
    // 为 shareCount-1
    for (;;) {
        int c = getState();
        int nextc = c - SHARED_UNIT;
        if (compareAndSetState(c, nextc))
            // Releasing the read lock has no effect on readers,
            // but it may allow waiting writers to proceed if
            // both read and write locks are now free.
            return nextc == 0;
    }
}


// 公平策略
/**
 * Fair version of Sync
 */
static final class FairSync extends Sync {
    private static final long serialVersionUID = -2274990926593161451L;
    final boolean writerShouldBlock() {
        return hasQueuedPredecessors();
    }
    final boolean readerShouldBlock() {
        return hasQueuedPredecessors();
    }
}
// 非公平策略：为了避免加写锁线程的“饥饿”
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -8159625535654395037L;
    // 加写锁的不需要等待读锁
    final boolean writerShouldBlock() {
        return false; // writers can always barge
    }
    final boolean readerShouldBlock() {
        /* As a heuristic to avoid indefinite writer starvation,
         * block if the thread that momentarily appears to be head
         * of queue, if one exists, is a waiting writer.  This is
         * only a probabilistic effect since a new reader will not
         * block if there is a waiting writer behind other enabled
         * readers that have not yet drained from the queue.
         */
        // 如果当前有加写锁线程在排队，则加读锁的要阻塞。
        return apparentlyFirstQueuedIsExclusive();
    }
}
```


- 加写锁调用的是 AQS 的 acquire 方法，也就是子类的 tryAcquire
```java
protected final boolean tryAcquire(int acquires) {
    /*
     * Walkthrough:
     * 1. If read count nonzero or write count nonzero
     *    and owner is a different thread, fail.
     * 2. If count would saturate, fail. (This can only
     *    happen if count is already nonzero.)
     * 3. Otherwise, this thread is eligible for lock if
     *    it is either a reentrant acquire or
     *    queue policy allows it. If so, update state
     *    and set owner.
     */
    Thread current = Thread.currentThread();
    int c = getState();
    int w = exclusiveCount(c);
    if (c != 0) {
        // 当前读锁被持有或者写锁持有者不是本线程
        // (Note: if c != 0 and w == 0 then shared count != 0)
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        setState(c + acquires);
        return true;
    }
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```