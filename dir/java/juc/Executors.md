### 线程池 Executors

- 线程池参数

Executors 作为一个工厂类，实际调用的是 ThreadPoolExecutor 的构造方法，例如：

```java
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) 
```

- 当向线程池提交一个任务时会发生什么？

1. 当线程数 < corePoolSize 的时候，会创建一个新的线程来执行该任务；
2. 当线程数 >= corePoolSize 的时候，如果：
    1. 任务队列未满（或者是无限队列），将任务入队列，等待被执行；
    2. 任务队列已满，如果：
        1.  线程数 < maximumPoolSize：创建新的线程来执行该任务；
        2.  否则，根据 RejectPolicy 来处理该任务。

- 任务拒绝

出现场景：

1. 线程池已被关闭；
2. 使用了有限队列以及 maximumPoolSize 不为 MAX_VALUE，且都已达到容量最大值；

通过调用 ThreadPoolExecutor 的 `RejectedExecutionHandler` 对象的 `rejectedExecution` 方法来拒绝一个任务。

拒绝策略有以下几种：

1. AbortPolicy：抛出异常，**默认**；
2. CallerRunsPolicy：使用调用 execute 方法的线程来执行该任务，从而降低新任务的提交速度（因为提交任务的线程已经被占用来处理任务了）；
3. DiscardPolicy：直接丢弃该任务；
4. DiscardOldestPolicy：丢弃队列头的任务，也就是等待了最久时间的任务，然后重新尝试提交当前任务。

- 线程池创建一个线程的流程是怎样的？

线程池在新的任务被提交时决定是否增加新的线程来处理该任务。

线程池内部通过 Worker 类来封装一个线程，同时 Worker 也实现了 Runnable 接口。

在 addWorker 方法中初始化一个 Worker，而 Worker 在初始化的时候会通过 ThreadFactory 创建一个新的线程，同时将自己作为 Runnable 对象传入。

当该线程的 start 方法被调用的时候，就会执行 Worker 的 run() 方法。该 run 方法的逻辑就是不断从任务队列获取任务，並执行。

- 线程池中的哪些线程会被在什么时候被回收？是如何实现的？

1. 非 core 线程：等待任务时间超过 `keepAliveTime` 的线程会被回收； 
2. core 线程默认不进行回收，但也可以通过方法 `allowCoreThreadTimeOut(boolean value)` 设置进行 core 线程的回收，等待时间一样使用 `keepAliveTime`。