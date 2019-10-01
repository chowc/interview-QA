### CyclicBarrier（基于 1.8）

用在多个线程互相等待的场景中。

通过 `CyclicBarrier(int parties, Runnable barrierAction)` 实例化一个对象，并指定了一个回调对象，parties 表示线程组的数量，不允许为负值。
```java
public CyclicBarrier(int parties, Runnable barrierAction) {
    if (parties <= 0) throw new IllegalArgumentException();
    this.parties = parties;
    this.count = parties;
    this.barrierCommand = barrierAction;
}
```
通过调用 `await()` 来等待其他线程，如果调用线程不是最后一个线程，则会阻塞，否则会先执行 barrierAction，再唤醒其他线程。**在线程 await 过程中中断线程会抛出异常，所有进入屏障的线程都将被释放。**线程等待超时也会释放所有进入屏障的线程。

CyclicBarrier 并不像其他并发类在内部实现了 AQS 的子类，而是通过使用 ReentrantLock 来对 parties 的修改进行加锁。

- doWait 是 CyclicBarrier 中的核心方法
```java
private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
    final ReentrantLock lock = this.lock;
    // 使用 ReentrantLock 来对 count 的修改做保护
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }
        // 每次有一个线程调用 await，就将 count-1，如果 count 减为 0，就使用当前线程执行 barrierCommand，然后重置 barrier。
        int index = --count;
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                // 重置
                nextGeneration();
                return 0;
            } finally {
                // 执行回调的 barrierCommand 失败了。
                if (!ranAction)
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                // 被中断唤醒
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // We're about to finish waiting even if we had not
                    // been interrupted, so this interrupt is deemed to
                    // "belong" to subsequent execution.
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                // 不是因为条件满足而被唤醒，而是因为 barrier 被 break 了。
                throw new BrokenBarrierException();

            if (g != generation)
                // 条件满足，被正常唤醒，返回该线程调用 await 的顺序，0 表示是最后一个线程。
                return index;
            // 等待超时
            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
// 非正常唤醒（被中断或超时）
private void breakBarrier() {
    generation.broken = true;
    count = parties;
    trip.signalAll();
}
// 条件已经满足，唤醒所有等待的线程，并且重置 barrier。
private void nextGeneration() {
    // signal completion of last generation
    trip.signalAll();
    // set up next generation
    count = parties;
    generation = new Generation();
}
```