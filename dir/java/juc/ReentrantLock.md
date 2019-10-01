### ReentrantLock（基于 1.8）

实现了 Lock 接口，提供了阻塞获取锁 lock()，非阻塞获取锁 tryLock() 等方法。ReentrantLock 实际上是通过委托 AQS 的实现类 FairSync 和 NonFairSync 来实现的。

另外即使使用的是公平锁，tryLock 方法也是非公平的。

- ReentrantLock 如何实现公平和非公平锁是如何实现？

如果一个锁是公平的，那么锁的获取顺序就应该符合请求上的时间顺序，满足 FIFO。

ReentrantLock 在实例化时可以指定是否使用公平锁。公平锁使用 FairSync 实现，非公平锁则使用 NonFairSync 实现。它们都继承了 AQS 並对 tryAcquire 方法做了不同的实现。

```java
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

1. FairSync 的 tryAcquire 方法
```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 会考虑当前是否有线程正在等待，因此是公平锁
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```
2. NonFairSync 的 tryAcquire 方法 
```java
protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}

final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 直接竞争锁而不考虑之前的线程
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```
- 可重入在加锁、释放锁的操作和判断

当持有锁的线程再次获取锁的时候，会对 state 做加法；当释放锁时，对 state 对减法，如果 state 减为 0，表示当前线程已不持有该锁，此时 tryRelease 返回 true，会执行唤醒排队线程的逻辑；如果 state 仍大于 0，则表示当前线程还持有锁，无需唤醒排队线程。

- 公平锁和非公平锁的比较

1. 公平锁每次获取到锁为同步队列中的第一个节点，保证请求资源时间上的绝对顺序，而非公平锁有可能刚释放锁的线程下次继续获取该锁，则有可能导致其他线程永远无法获取到锁，造成“饥饿”现象。
2. 公平锁为了保证时间上的绝对顺序，需要频繁的上下文切换，而非公平锁会降低一定的上下文切换，降低性能开销。因此，ReentrantLock默认选择的是非公平锁，则是为了减少一部分上下文切换，保证了系统更大的吞吐量。