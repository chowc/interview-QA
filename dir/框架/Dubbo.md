了解一个常用 RPC 框架如 Dubbo 的实现：服务发现、路由、异步调用、限流降级、失败重试
#### RPC 的实现

1. 客户端在执行 API 时，将接口类、方法名、参数类型、参数转换成字节流，传输给服务端；

2. 服务端代码要监听端口，解析字节流，拿到数据后解析出接口名、方法名、参数类型、参数后执行函数体，并将返回值回写入 socket，返回给客户端。

- 多个线程使用同一个 IO 连接

发起调用后，线程会进入等待队列，每次调用都有一个唯一的调用 ID，服务端返回结果时会将调用 ID 也返回，消费者根据调用 ID 找到对应的调用线程，唤醒它（设置 Future 的值）。

#### Dubbo 调用失败的容错策略

通过不同的 `org.apache.dubbo.rpc.cluster.Cluster` 实现类来实现不同的容错策略。

1. failover

默认策略。失败自动切换，当出现失败，重试其它服务器。通常用于读操作，但重试会带来更长延迟。可通过 `retries="2"` 来设置重试次数（不含第一次）。

```
<dubbo:service retries="2" />
<dubbo:reference retries="2" />
<dubbo:reference> <dubbo:method name="findFoo" retries="2" /> </dubbo:reference>
```

2. failfast

快速失败，只发起一次调用，失败立即报错。通常用于非幂等性的写操作，比如新增记录。

3. failsafe

出现异常时，直接忽略。通常用于写入审计日志等操作。

4. failback

失败自动恢复，后台记录失败请求，定时重发。通常用于消息通知操作。

5. forking

并行调用多个服务器，只要一个成功即返回。通常用于实时性要求较高的读操作，但需要浪费更多服务资源。可通过 `forks="2"` 来设置最大并行数。

6. broadcast

广播调用所有提供者，逐个调用，任意一台报错则报错 。通常用于通知所有提供者更新缓存或日志等本地资源信息。

#### [Dubbo 如何做负载均衡？](http://dubbo.apache.org/zh-cn/docs/source_code_guide/loadbalance.html)

Dubbo 提供了4种负载均衡实现，分别是基于权重随机算法的 RandomLoadBalance、基于最少活跃调用数算法的 LeastActiveLoadBalance、基于一致性哈希的 ConsistentHashLoadBalance，以及基于加权轮询算法的 RoundRobinLoadBalance。

- RandomLoadBalance

默认策略。随机，按权重设置随机概率。在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重。

- LeastActiveLoadBalance

在具体实现中，每个服务提供者对应一个活跃数 active。初始情况下，所有服务提供者活跃数均为 0。每收到一个请求，活跃数加 1，完成请求后则将活跃数减 1。在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求、这就是最小活跃数负载均衡算法的基本思想。

- ConsistentHashLoadBalance

当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。

*缺省只对第一个参数 Hash*，如果要修改，请配置 `<dubbo:parameter key="hash.arguments" value="0,1" />`；

缺省用 160 份虚拟节点，如果要修改，请配置 `<dubbo:parameter key="hash.nodes" value="320" />`。

- RoundRobinLoadBalance

存在慢的提供者累积请求的问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上。

**平滑加权轮询负载均衡**。

因为加权负载均衡算法在某些情况下选出的服务器序列不够均匀。比如，服务器 [A, B, C] 对应权重 [5, 1, 1]。进行 7 次负载均衡后，选择出来的序列为 [A, A, A, A, A, B, C]。前 5 个请求全部都落在了服务器 A 上，这将会使服务器 A 短时间内接收大量的请求，压力陡增。而 B 和 C 此时无请求，处于空闲状态。而我们期望的结果是这样的 [A, A, B, A, C, A, A]，不同服务器可以穿插获取请求。

#### Dubbo 如何做限流降级？

Dubbo中的限流通过 `org.apache.dubbo.rpc.filter.TpsLimitFilter`来实现，会在 invoker 执行实际业务逻辑前进行拦截，判断单位时间请求数是否超过上限，如果超过，抛出异常阻断调用。

#### [Dubbo 如何优雅的下线服务？](https://dubbo.apache.org/zh-cn/blog/dubbo-gracefully-shutdown.html)

优雅停机本质上是 JVM 即将关闭前执行的一些额外的处理代码。Dubbo 在加载类 `org.apache.dubbo.config.AbstractConfig` 时，通过 `org.apache.dubbo.config.DubboShutdownHook` 向 JVM 注册 ShutdownHook。

Provider在接收到停机指令后

1. 从注册中心上注销所有服务；
2. 从配置中心取消监听动态配置；
3. 向所有连接的客户端发送只读事件，停止接收新请求；
4. 等待一段时间以处理已到达的请求，然后关闭请求处理线程池；
5. 断开所有客户端连接。

Consumer在接收到停机指令后

1. 拒绝新到请求，直接返回调用异常；
2. 等待当前已发送请求执行完毕，如果响应超时则强制关闭连接。

#### [Dubbo 如何实现异步调用的？](http://dubbo.apache.org/zh-cn/blog/dubbo-invoke.html)

其实，Dubbo 的底层 IO 操作都是异步的。Consumer 端发起调用后，得到一个 Future 对象。

1. 对于同步调用，业务线程通过 `Future.get(timeout)`，阻塞等待 Provider 端将结果返回；timeout 则是 Consumer 端定义的超时时间。当结果返回后，会设置到此 Future，并唤醒阻塞的业务线程；当超时时间到结果还未返回时，业务线程将会异常返回；
2. 对于异步调用，DubboInvoker 不会直接调用 Future.get 方法，而是将 Future 对象添加到 [RpcContext.getContext()](https://www.cnblogs.com/java-zhao/p/8424019.html) 里，之后由业务线程自己在适当的时机去获取这个 Future，並执行它的 get 方法；
3. 对于回调方法的实现：Provider 端是通过调用 Consumer 端自动导出的 Callback Service（**待展开**）；
4. 对于事件通知：实际上就是配置 oninvoke、onreturn、onthrow 三个事件对应的 Callback 方法，然后 Provider 端去调用。


#### 服务导出的过程

Dubbo 会在 Spring 实例化完 bean 之后，在刷新容器最后一步发布 ContextRefreshEvent 事件的时候，通知实现了 ApplicationListener 的 ServiceBean 类进行回调 onApplicationEvent 事件方法，Dubbo 会在这个方法中调用 ServiceBean 父类 ServiceConfig 的 export 方法，而该方法真正实现了服务的（异步或者非异步）发布。

- [RpcContext 与链路追踪](https://dubbo.apache.org/zh-cn/blog/dubbo-context-information.html)

上下文信息是一次 RPC 调用过程中附带的环境信息，如方法名、参数类型、真实参数、本端/对端地址等。这些数据仅属于一次调用，作用于 Consumer 到 Provider 调用的整个流程。

提供上下文信息是 RPC 框架很重要的一个功能，使用上下文不仅可以为单次调用指定不同配置，还能在此基础上提供强大的上层功能，如分布式链路追踪。其实现原理就是在上下文中维护一个span_id，Consumer 和 Provider 通过传递span_id来连接一次RPC调用，分别上报日志后可以在追踪系统中串联并展示完整的调用流程。这样可以更方便地发现异常，定位问题。