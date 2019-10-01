### CountDownLatch（基于 1.8）

用于控制多个线程在某个条件满足（即 state=0）之前一直进行等待。线程会通过调用 `await` 方法来等待条件满足：当 state=0 的时候，说明条件已经满足，对 await 的调用会立即返回；当 await>0 的时候表示条件未满足，线程会阻塞。通过调用 `countDown()` 来修改 state，如果 state>0，会使得 state-1；否则 state=0，此时可以唤醒所有在 await 上等待的线程。

**当 state 减到 0 的时候，不会重置为初始化的值。且 state 初始值不能为负值。**

CountDownLatch 内部只有一个 AQS 的子类：
```java
private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 4982264981922014374L;

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }

    protected int tryAcquireShared(int acquires) {
        // 实际上是非公平策略，但因为一调用 await 即阻塞，线程都在 sync 队列中等待。
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // Decrement count; signal when transition to zero
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c-1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}
```