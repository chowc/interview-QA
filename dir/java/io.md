### IO
- 了解 BIO 和 NIO 的区别、了解多路复用机制

读取动作可以分为两个阶段：

1. 等待数据到达的过程；
2. 数据到达之后真正的读取过程，也就是下图中“将数据从内核拷贝到用户空间”的过程。

![image](../img/io_model.png)

其中：

1. BIO 在阶段 1、2 都会被阻塞；
2. NIO **在阶段 1 不被阻塞**，但是需要进行轮询（polling）来查看数据到达状态，在阶段 2 仍然是要被阻塞的；
3. 而 I/O 复用通过在阶段 1 调用一个 `select` 函数来阻塞自己，该函数**等待其监控的多个 socket 中的任意一个变为可读**，I/O 复用可以通过一个线程来管理多个 socket，从而提高了效率。

- 同步阻塞、同步非阻塞、异步的区别？

同步：指的是一个进程（线程）在进行了方法调用之后，需要等待调用结果返回才能继续别的任务（这里的等待可以是直接进入阻塞状态，或者不进入阻塞，而通过自身的不断轮询去查询调用结果）；

异步：指的是一个进程（线程）在进行了方法调用之后，可以马上继续别的任务，之后该方法调用的结果会通过回调的方式通知给调用进程（线程）；

阻塞：进程（线程）在进行了方法调用之后，自己会被设置为睡眠状态（不能被 CPU 调度运行）；

非阻塞：进程（线程）在进行了方法调用之后，不会被设置为睡眠状态（仍能被 CPU 调度运行）；

> synchronous generally means an activity that must wait for a reply before the thread can move forward. Blocking refers to the fact that the thread is placed in a wait state (generally meaning it will not be scheduled for execution until some event occurs). From here you can conclude that a synchronous call may involve blocking behavior or may not, depending on the underlying implementation (i.e. it may also be spinning, meaning that you are simulating synchronous behavior with asynchronous calls).
> 
> https://stackoverflow.com/questions/8416874/whats-the-differences-between-blocking-with-synchronous-nonblocking-and-asynch

因此，**阻塞 IO 一定是同步的；非阻塞 IO 也是同步的（因为需要进行不断的轮询）**。

- select、poll、eopll 的区别？

socket 对进程的唤醒

Linux通过socket睡眠队列来管理所有等待socket的某个事件的process，同时通过wakeup机制来异步唤醒整个睡眠队列上等待事件的process，通知process相关事件发生。通常情况，socket的事件发生的时候，其会顺序遍历socket睡眠队列上的每个process节点，调用每个process节点挂载的callback函数。在遍历的过程中，如果遇到某个节点是排他的，那么就终止遍历，总体上会涉及两大逻辑：（1）睡眠等待逻辑；（2）唤醒逻辑。

1. select

```c
int select(int maxfdp1, fd_set *readset, fd_set *writeset, fd_set *exceptset, nconst struct timeval *timeout)
返回值：就绪描述符的数目，超时返回 0，出错返回 -1
```

当用户 process 调用 select 的时候， select 会将需要监控的 readset、writeset 集合拷贝到内核空间（假设监控的仅仅是 socket 可读），然后遍历自己监控的 socket，挨个调用该 sk 的 poll 来收集发生的事件（可读/可写），遍历完所有的 socket 后，如果没有任何一个可读，那么 select 会调用 schedule_timeout 进入 schedule 循环，使得 process 进入睡眠。如果在 timeout 时间内某个 socket 上有数据可读了，或者等待 timeout 了，则调用 select 的 process 会被唤醒，接下来 select 就是遍历监控的 socket 集合，挨个收集可读事件并返回给用户了。
```java
// select 示例代码
for (sk in readfds) {
	// 检查每个 socket
    sk_event.evt = sk.poll();
    sk_event.sk = sk;
    ret_event_for_process;
}
```

select 的几大缺点：
	1. 每次调用 select，都需要把 fd_set 集合从用户态拷贝到内核态，这个开销在 fd_set 很多时会很大；
	2. 同时每次调用 select 都需要在内核遍历传递进来的所有 fd_set，这个开销在 fd_set 很多时也很大；事件发生时对 socket 的收集也需要进行全集合遍历；
	3. fd_set 的容量最大是 1024。

2. poll

poll 跟 select 的最大区别是传入的文件描述符集合只有一个。poll 只是解决了 select 的问题中 fds 集合最大为 1024 的限制问题。poll 改变了 fds 集合的描述方式，使用了 pollfd 结构而不是 select 的 fd_set 结构，使得 poll 支持的 fds 集合限制远大于 select 的 1024。但是，它并没改变大量描述符数组被整体复制于用户态和内核态的地址空间之间，以及个别描述符就绪触发整体描述符集合的遍历的低效问题。poll 随着监控的 socket 集合的增加性能线性下降，poll 不适合用于大并发场景。

```c
int poll(struct pollfd fds[], nfds_t nfds, int timeout);
```

3. epoll

epoll 提供了三个函数，epoll_create, epoll_ctl 和 epoll_wait，epoll_create 是创建一个 epoll 句柄；epoll_ctl 是注册要监听的事件类型；epoll_wait 则是等待事件的产生。


拷贝问题的解决：

epoll 引入了 epoll_ctl 系统调用，将高频调用的 epoll_wait 和低频的 epoll_ctl 隔离开。同时，epoll_ctl 通过 (EPOLL_CTL_ADD、EPOLL_CTL_MOD、EPOLL_CTL_DEL) 三个操作来分散对需要监控的 fds 集合的修改，做到了有变化才变更，将 select 或 poll 高频、大块内存拷贝(集中处理)变成 epoll_ctl 的低频、小块内存的拷贝(分散处理)，避免了大量的内存拷贝。同时，对于高频 epoll_wait 的可读就绪的 fd 集合返回的拷贝问题（**返回时也需要进行拷贝？还需要进行用户态拷贝到核心态吗？**），epoll 通过内核与用户空间 mmap（内存映射）同一块内存来解决。mmap 将用户空间的一块地址和内核空间的一块地址同时映射到相同的一块物理内存地址（不管是用户空间还是内核空间都是虚拟地址，最终要通过地址映射映射到物理地址），使得这块物理内存对内核和对用户均可见，减少用户态和内核态之间的数据交换。

另外，epoll 通过 epoll_ctl 来对监控的 fds 集合来进行增、删、改，那么必须涉及到 fd 的快速查找问题，于是，一个低时间复杂度的增、删、改、查的数据结构来组织被监控的 fds 集合是必不可少的了。在 linux 2.6.8 之前的内核，epoll 使用 hash 来组织 fds 集合，于是在创建 epoll fd 的时候，epoll 需要初始化 hash 的大小。于是 epoll_create(int size) 有一个参数 size，以便内核根据 size 的大小来分配 hash 的大小。在 linux 2.6.8 以后的内核中，epoll 使用红黑树来组织监控的 fds 集合，于是 epoll_create(int size) 的参数 size 实际上已经没有意义了。

遍历问题的解决：

socket 唤醒睡眠在其睡眠队列的 wait_entry(process) 的时候会调用 wait_entry 的回调函数 callback，并且，我们可以在 callback 中做任何事情。为了做到只遍历就绪的 fd，我们需要有个地方来组织那些已经就绪的 fd。为此，epoll 引入了一个中间层，一个双向链表 (ready_list)，一个单独的睡眠队列 (single_epoll_wait_list)，并且，与 select 或 poll 不同的是，epoll 的 process 不需要同时插入到多路复用的 socket 集合的所有睡眠队列中，相反 process 只是插入到中间层的 epoll 的单独睡眠队列中，process 睡眠在 epoll 的单独队列上，等待事件的发生。同时，引入一个中间的 wait_entry_sk，它与某个 socket sk 密切相关，wait_entry_sk 睡眠在 sk 的睡眠队列上，其 callback 函数逻辑是将当前 sk 加入到 epoll 的 ready_list 中，并唤醒 epoll 的 single_epoll_wait_list。而 single_epoll_wait_list 上睡眠的 process 的回调函数则负责遍历 ready_list 上的所有 sk，挨个调用 sk 的 poll 函数收集事件，然后唤醒 process 从 epoll_wait 返回。


相比 select、poll，epoll 的优势在于：

	1. epoll 没有文件描述符的数量限制；
	2. epoll 不是通过轮询方式来查看某个文件描述符是否就绪，而是通过回调；
	3. 不需要在每次调用的时候都将参数从用户空间拷贝到内核空间。

当监听的 socket 不是很多，且大多数都是活跃的情况下，epoll 性能就不一定能够优于 select。

*TODO: epoll 的详细机制*。

- [NIO 中 poll/epoll 导致 CPU 占用 100% 的 bug](https://www.jianshu.com/p/3ec120ca46b2)

在某些情况下，socket 的中断会导致 `selector.select()` 调用返回，但 selectedKeys 为空，使得代码进入无限循环中。

解决方法是：当发现进入 bug 模式中时，新建一个 selector，並将中断 socket 剔除。

#### Java 的 NIO（Non-blocking I/O，在 Java 领域，也称为 New I/O）

> ... channels and buffers. You could think of it as a coal mine; the channel is the
mine containing the seam of coal (the data), and the buffer is the cart that you send into the
mine. The cart comes back full of coal, and you get the coal from the cart. That is, you don’t
interact directly with the channel; you interact with the buffer and send the buffer into the
channel. The channel either pulls data from the buffer, or puts data into the buffer.

核心组件主要是 3 个：

1. Channel：A channel represents an open connection to an entity such as a hardware device, a file, a network socket, or a program component that is capable of performing one or more distinct I/O operations, for example reading or writing；
2. ByteBuffer：读写缓冲区；
3. Selector：当 Channel 使用的是非阻塞模式（如：ServerSocketChannel 中），通过 Selector 来监听多个 channel。

channel 的类型、阻塞（FileChannel）非阻塞（SocketChannel）；
Buffer 的结构：capacity、limit、position；操作：flip、compact、clear；
Selector 的事件类型、阻塞等待、返回的 channel 集合处理。


```java
// channel 示例代码

// selector 示例代码
Selector selector = Selector.open();
channel.configureBlocking(false);
SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
```

- reactor 线程模型是什么?

1. 单请求单线程
2. 线程池
3. reactor

![image](../img/reactor_model.png)

---
参考：
- [select/poll/epoll 最详细讲解](https://cloud.tencent.com/developer/article/1005481)
- 《Thinking in Java》New IO
- [一文理解 Java NIO 核心组件](https://www.cnblogs.com/lfs2640666960/p/9970353.html)