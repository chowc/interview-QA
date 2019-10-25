- netty 的多种 Reactor 线程模型
- netty 的零拷贝是什么？
- netty 为什么需要 IO 线程池和业务线程池分开与如何实现

在 Reactor 模型中，一个线程需要处理多个 socket，这个线程就叫做 Reactor，Reactor 通过同步多路选择器（Synchrouns Event Demultiplexer，linux 中的 `epoll()`）来阻塞在它关注的多个 socket 上，当某个 socket 产生了 Reactor 关注的事件，阻塞就会返回，Reactor 可以针对不同的事件进行不同的处理，也就是 EventHandler。
```c
// Reactor 示例代码
void Reactor::handle_events(){
  //通过同步事件多路选择器提供的
  //select()方法监听网络事件
  select(handlers);
  //处理网络事件
  for(h in handlers){
    h.handle_event();
  }
}
// 在主程序中启动事件循环
while (true) {
  handle_events();
```

在 Netty 中，EventLoop 充当了 Reactor 的角色，一个线程对应一个 EventLoop，一个 EventLoop 管理多个 socket，Selector 充当了多路选择器。

Netty 中还有一个核心概念是 EventLoopGroup，顾名思义，一个 EventLoopGroup 由一组 EventLoop 组成。实际使用中，一般都会创建两个 EventLoopGroup，一个称为 bossGroup，一个称为 workerGroup。

为什么会有两个 EventLoopGroup 呢？这个和 socket 处理网络请求的机制有关，socket 处理 TCP 网络连接请求，是在一个独立的 socket 中，每当有一个 TCP 连接成功建立，都会创建一个新的 socket，之后对 TCP 连接的读写都是由新创建处理的 socket 完成的。也就是说**处理 TCP 连接请求和读写请求是通过两个不同的 socket 完成的。在 Netty 中，bossGroup 就用来处理连接请求的，而 workerGroup 是用来处理读写请求的。bossGroup 处理完连接请求后，会将这个连接提交给 workerGroup 来处理**， workerGroup 里面有多个 EventLoop，那新的连接会交给哪个 EventLoop 来处理呢？这就需要一个负载均衡算法，Netty 中目前使用的是轮询算法。

1. 如果 NettybossGroup 只监听一个端口，那 bossGroup 只需要 1 个 EventLoop 就可以了，多了纯属浪费。
2. 默认情况下，Netty 会创建 “2*CPU 核数”个 EventLoop，由于网络连接与 EventLoop 有稳定的关系，所以事件处理器在处理网络事件的时候是不能有阻塞操作的，否则很容易导致请求大面积超时。如果实在无法避免使用阻塞操作，那可以通过线程池来异步处理。

```java
// netty echo server 示例代码
//事件处理器
final EchoServerHandler serverHandler = new EchoServerHandler();
//boss线程组  
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//worker线程组  
EventLoopGroup workerGroup = new NioEventLoopGroup();
try {
	ServerBootstrap b = new ServerBootstrap();
	b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) {
					ch.pipeline().addLast(serverHandler);
				}
			});
	//bind服务端端口  
	ChannelFuture f = b.bind(9090).sync();
	f.channel().closeFuture().sync();
} finally {
	//终止工作线程组
	workerGroup.shutdownGracefully();
	//终止boss线程组
	bossGroup.shutdownGracefully();
}

//socket连接处理器
class EchoServerHandler extends
		ChannelInboundHandlerAdapter {
	//处理读事件  
	@Override
	public void channelRead(
			ChannelHandlerContext ctx, Object msg) {
		ctx.write(msg);
	}

	//处理读完成事件
	@Override
	public void channelReadComplete(
			ChannelHandlerContext ctx) {
		ctx.flush();
	}

	//处理异常事件
	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
```

---
参考资料：
- [极客时间 Java 并发编程实战 第 39 讲](https://time.geekbang.org/column/article/97622)