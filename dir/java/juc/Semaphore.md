### Semaphore

用于限制能够访问资源的线程数，用 permits 来表示允许的线程数。调用 `acquire` 来获取许可，会使 permits-1，如果 permits<=0 则阻塞当前线程；调用 `release` 来释放当前持有的许可，会使 permits+1。

提供了构造方法 `Semaphore(int permits, boolean fair)`  用于指定允许通过的线程数以及采用的公平策略。当 permits 设置为 1 的时候，就类似于一个互斥锁。默认采用非公平策略。

**permits 可以设置为负值。**

- AQS 子类

Semaphore 内部实现了两个 AQS 的子类：FairSync、NonfairSync，分别代表信号量的公平与非公平获取策略。

- acquire

acquire 实际上是通过调用 Sync 的 `tryAcquireShared(int acquires)` 来获取资源许可的。

```java
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
// FairSync
protected int tryAcquireShared(int acquires) {
    for (;;) {
        // 公平策略
        if (hasQueuedPredecessors())
            return -1;
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```
- release 

release 实际上是通过调用 Sync 的 `tryReleaseShared(int releases)` 方法来释放持有的资源许可的。需要注意的是，*执行 release 的线程不需要是之前通过 acquire 的线程，也就是说在 Semaphore 中，没有锁持有者的概念。一个线程可以一直进行 release 调用，经过多次 release，permits 的值将超过初始值。*

```java
public void release() {
    sync.releaseShared(1);
}
// Sync
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```