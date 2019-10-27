```java
// 1.8
// DelayQueue.take 
// 采用了 leader-follower 模式，避免多个线程在等待元素变为可获取时的不必要竞争。
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();
            if (first == null)
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();
                first = null; // don't retain ref while waiting
                // 自己是 follower 则无限等待
                if (leader != null)
                    available.await();
                else {
                    Thread thisThread = Thread.currentThread();
                    // 当前还没有 leader，尝试成为 leader。
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```