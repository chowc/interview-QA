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

#### state

- state 的状态划分

是一个 `volatile int state` 的内部变量。由继承 AQS 的子类自行定义各个状态，例如ReentrantLock中用state表示线程重入的次数，Semaphore表示可用的许可的数量等。同时在加锁/解锁的过程中对状态做相应的修改。主要涉及的方法有：

1. getState()：获取当前同步器状态
2. setState(int newState)：设置同步器状态
3. compareAndSetState(int expect, int update)；

当继承 AQS 的时候，需要实现以下几个方法，AQS 的其余方法是 final 的，也就不允许覆盖：

1. tryAcquire：实现独占锁时需要实现该方法，用于获取资源；

该方法是被 `acquire(int arg)` 调用的，用于独占式获取资源。若获取失败，则将当前线程入队。
```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```
2. tryRelease：实现独占锁时需要实现该方法，用于释放资源；

该方法是被 `release(int arg)` 调用的，用于释放独占资源，释放成功返回 true，否则返回 false。
```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```
3. tryAcquireShared：实现共享锁时需要实现该方法，用于获取锁。若获取失败，则将当前线程入队并返回负值。若返回 0，说明获取成功但后续获取会失败；若返回正值，说明后续获取仍可成功；

该方法是被 `acquireShared(int arg)` 调用的，用于共享式获取资源。

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```
4. tryReleaseShared：实现共享锁时需要实现该方法，用于释放锁；

该方法是被 `releaseShared(int arg)` 调用的，用于释放资源，释放成功返回 true，否则返回 false。
```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```
5. isHeldExclusively()：判断当前线程是否独占式地拥有资源，可以通过 `getState()==x` 来进行判断。

#### 队列
```java
class Node {
    ...
    private Thread thread;
    ...
}

```
- 队列中 Node 的状态划分

1. CANCELLED，值为 1，唯一大于 0 的状态，表示当前的线程被取消，该节点可以被删除（例如在 shouldParkAfterFailedAcquire 方法中）；
2. SIGNAL，值为 -1，表示需要唤醒后节点，也就是 unpark；在 acquire 的时候表示后节点可以进入 park（shouldParkAfterFailedAcquire 返回 true）；
3. CONDITION，值为 -2，表示当前节点在等待 condition，也就是在 condition 队列中；
4. PROPAGATE，值为 -3，表示当前场景下后续的 acquireShared 能够得以执行；
5. 值为 0，初始值，表示当前节点在等待队列中，等待着获取锁。

- head、tail

两个都是 `volatile Node` 的内部变量。其中 head/tail 初始化为空节点（`new Node()`），之后随着队列中的线程获取资源成功，head 变为当前获取资源的线程，是 sync 队列的头，tail 是队列的末尾元素，当新的线程入队时将其设置为 tail，当队列中的节点获取资源成功时，将其设置为 head。

每个节点都将其自身状态委托给前节点，例如当执行 release 的时候如果当前节点状态是 SIGNAL，表明需要唤醒下一节点。

==非公平锁情况下，head 节点有可能不是持有锁的节点，一个节点只有在其前节点状态为 SINGAL(-1) 的时候才会进入 park。而 release 动作会对 SIGNAL 状态的 head 唤醒其后节点。==

- Node 根据 Precessor 状态所做的动作

另外为了减少不必要的自旋，只有**当前节点是 head 的时候才会尝试去竞争资源**。

- 如何唤醒下一节点

因为节点的入队不是原子性的，

```java
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                // 在这个时间点，其它线程可以看到一个 next==null 但不是 tail 的节点，但是 prev 是可见的。
                t.next = node;
                return t;
            }
        }
    }
}
```

所以在唤醒下一节点的时候，当发现 next==null 需要由后往前进行查找：

```java
private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try
     * to clear in anticipation of signalling.  It is OK if this
     * fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
     * Thread to unpark is held in successor, which is normally
     * just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual
     * non-cancelled successor.
     */
    Node s = node.next;
    // 即使 next 是空的，也不能说明是 tail。从 tail 往前才是可靠的，因为 enqueue 先写 prev 再写 next。
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

- PROPAGATE 状态的使用

在 shouldParkAfterFailedAcquire 中会被转变成 SIGNAL。

#### ConditionObject

```java
public class ConditionObject implements Condition, java.io.Serializable {
    /** First node of condition queue. */
    private transient Node firstWaiter;
    /** Last node of condition queue. */
    private transient Node lastWaiter;
}
```
**condition 队列中的节点只有两种状态：CONDITION 以及其他（即取消）**，firstWaiter 是队列头，lastWaiter 是队列尾。

- 如何到 condition 队列

通过调用 `ConditonObject.await`，会先释放资源，新建一个 Node，並加入 condition 队列。因为能够调用 await 方法的线程必然持有锁，也就是 head 节点或者不在队列中（竞争时获得锁未入队）。
```java
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    // 添加到 condition 队列尾
    Node node = addConditionWaiter();
    // 尝试释放所持有的锁，若失败，将 node 设置为 CANCELED 状态
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    // 等待被 signal，也就是 node 进入到 sync 队列
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        // 检查中断状态，若在 condition 队列中被中断则抛出异常（即在 signal 之前被中断）；否则 selfInterrupt（表示在 sync 队列中被中断，与 acquire 语义保持一致）
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        int savedState = getState();
        if (release(savedState)) {
            failed = false;
            return savedState;
        } else {
            // 当前线程並没有持有锁而调用 await，所以 release 返回 false。
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED;
    }
}
// 发生中断时确保 node 进入 sync 队列
final boolean transferAfterCancelledWait(Node node) {
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        // 上一行返回 true 说明在被 interrupt（也就是 cancelldWait） 之前 node 还没在 sync 队列，也就是还没被 signal
        enq(node);
        return true;
    }
    /*
     * If we lost out to a signal(), then we can't proceed
     * until it finishes its enq().  Cancelling during an
     * incomplete transfer is both rare and transient, so just
     * spin.
     */
    while (!isOnSyncQueue(node))
        Thread.yield();
    // 表示 signal 发生在 interrupt 之前
    return false;
}
```

- 如何从 condition 队列到 sync 队列

通过调用 `ConditionObject.signal`，会将 condition 队列中第一个 `waitStatus=Node.CONDITION` 的节点从 condition 队列移除，並入队到 sync 队列。 

```java
public final void signal() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}
private void doSignal(Node first) {
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}

// 如果是在 wait 过程中被取消的节点（即 waitStatus != Node.CONDITION），则返回 false；否则返回 true。
final boolean transferForSignal(Node node) {
    /*
     * If cannot change waitStatus, the node has been cancelled.
     */
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;

    /*
     * Splice onto queue and try to set waitStatus of predecessor to
     * indicate that thread is (probably) waiting. If cancelled or
     * attempt to set waitStatus fails, wake up to resync (in which
     * case the waitStatus can be transiently and harmlessly wrong).
     */
    Node p = enq(node);
    int ws = p.waitStatus;
    // 将前节点设为 SIGNAL，表示当前节点需要被唤醒。或者前节点已取消（ws>0）、或设置失败，则直接唤醒该线程。
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        LockSupport.unpark(node.thread);
    return true;
}
```
- Condition.await、Condition.signal 和 Object.wait、Object.notify 的区别

1. wait/notify 需要在对应 Object 的同步块或同步方法中调用，而 await/signal 需要获取到 Condition 对应的锁对象（如：ReentrantLock）才能调用；
2. wait/notify 方法是 Object 的方法， 而 await/signal 方法是接口 Condition 的方法；
3. 一个 Lock 对象可以通过 `Lock.newCondition` 获得多个 Condition 对象，从而拥有多个等待队列；而使用 wait/signal 的时候，只能通过使用多个不同的 Object 来达到一样的效果。

- AQS 使用了模板方法设计模式

---
参考：

- [AQS](https://liuzhengyang.github.io/2017/05/12/aqs/)