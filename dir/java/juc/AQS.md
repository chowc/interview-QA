AQS 作为 juc 中许多同步类的基础类，封装了诸如获取同步状态、资源抢占、资源释放、线程排队、唤醒等代码，方便了其他同步类的开发。

- AQS 是如何唤醒下一个线程的？

当调用释放资源的方法，如 `Lock.unlock()` 的时候，会先释放资源，然后唤醒 head 节点的后续节点。只有获取资源成功才能成为 head 节点，对于是 head 节点线程释放资源的情况，head 节点唤醒自己的后续节点。对于不是 sync 队列中的线程释放资源的情况（非公平模式下，head 在释放资源之后，新的线程在与 head.successor 竞争时赢了，因此是一个不在 sync 队列中的线程获得了资源），也是唤醒 head.successor，因为此时 head 节点线程已经释放了资源，head.successor 可以尝试竞争成为 head。

- 一个线程加入等待队列的过程

如果调用子类的 `tryAcquire`（或者 `tryAcquireShare`） 方法返回 false，说明获取资源失败，此时会通过 `addWaiter` 方法将当前线程加入到等待队列中。

在加入等待队列时，会先尝试一次能够将队列的 tail CAS 为当前线程代表的节点。如果成功，则加入队列成功。如果失败的话，就在一个无限循环中尝试上述操作（如果 tail 是空的话，就将当前节点 CAS 为队列 head）。

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

- 一个线程从一开始到进入队列，会尝试多少次获取资源？

1. 一开始，通过 tryAcquire 尝试获取，如果返回 true，获取成功；否则继续下一步；
2. 当前线程先加入队列，做为 tail 节点，入队成功之后调用 acquireQueued 方法；
3. acquireQueued 会判断当前节点的前继是不是队列 head。如果是，再一次尝试 tryAcquire，获取成功，则当前节点成为 head，结束；否则，调用 shouldParkAfterFailedAcquire 检查前继节点状态（跳过被取消的节点），当前继节点状态是 SIGNAL 时，将自己进入休眠，否则将前继节点置为 SIGNAL 状态，並重复步骤 3；
4. 当前节点被唤醒时，继续重复 3 的操作，直到被中断、取消或者获取到资源。

```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                // 休眠期间被中断
                interrupted = true;
        }
    } finally {
    	// 发生异常，取消当前节点。
        if (failed)
            cancelAcquire(node);
    }
}
```

- Condition.await、Condition.signal 和 Object.wait、Object.notify 的区别

1. wait/notify 需要在对应 Object 的同步块或同步方法中调用，而 await/signal 需要获取到 Condition 对应的锁对象（如：ReentrantLock）才能调用；
2. wait/notify 方法是 Object 的方法， 而 await/signal 方法是接口 Condition 的方法；
3. 一个 Lock 对象可以通过 `Lock.newCondition` 获得多个 Condition 对象，从而拥有多个等待队列；而使用 wait/signal 的时候，只能通过使用多个不同的 Object 来达到一样的效果。