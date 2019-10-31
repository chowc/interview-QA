### Spring Cloud

- spring cloud 组件有哪些

服务注册中心 Eureka，客户端负载均衡 Ribbon，远程调用 Feign、断路器 Hystrix、配置中心 Config、路由及权限控制、负载均衡的 Zuul、消息总线 Bus。

- [ribbon](ribbon.md)
- [hystrix](hystrix.md)
- [feign](feign.md)

---
### eureka

服务注册：每个服务单元向注册中心登记自己提供的服务，注册的信息含括主机与端口号、版本号、通信协议等。服务中心会维护一个服务清单，同时使用心跳的方式检测清单中的服务是否可用，若不可用则需要从服务清单中剔除，以达到排除故障服务的效果。（注册、续约、下线）

eureka server 在启动的时候会创建一个定时任务，每 60s 将失效的服务实例剔除。

eureka client 和 eureka server 之间，多个 eureka server 之间的通讯使用 RESTful 方式，eureka server 也提供了 JSON/XML 的接口用于兼容不同语言的客户端。

服务消费：拉取服务注册信息，每 30 秒拉取一次更新信息应用到本地。

> Eureka server itself fires up a Eureka Client that it uses to find other Eureka Servers. Therefore, you need to first configure the Eureka Client for the Eureka Server as you would do with any other clients that connect to the Eureka service. The Eureka Server will use its Eureka Client configuration to identify peer eureka server that have the same name (ie) eureka.name.

> Eureka clients fetches the registry information from the server and caches it locally. After that, the clients use that information to find other services. This information is updated periodically (every 30 seconds) by getting the delta updates between the last fetch cycle and the current one. The delta information is held longer (for about 3 mins) in the server, hence the delta fetches may return the same instances again. The Eureka client automatically handles the duplicate information.
>
> After getting the deltas, Eureka client reconciles the information with the server by comparing the instance counts returned by the server and if the information does not match for some reason, the whole registry information is fetched again. Eureka server caches the compressed payload of the deltas, whole registry and also per application as well as the uncompressed information of the same. The payload also supports both JSON/XML formats. Eureka client gets the information in compressed JSON format using jersey apache client.

```java
// eureka client
com.netflix.discovery.DiscoveryManager;
// 初始化 -> START 状态
DiscoveryManager.getInstance().initComponent(new MyDataCenterInstanceConfig(), 
	new DefaultEurekaClientConfig());
// 发送心跳：RENEW，通过
private class com.netflix.discovery.DiscoveryClient.HeartbeatThread implements Runnable {

    public void run() {
        if (renew()) {
            lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
        }
    }
}

// 关闭服务，注销服务信息，状态被置为 CANCEL
DiscoveryManager.getInstance().shutdownComponent();

// 对 eureka client 获取的服务信息做负载，默认使用轮询。
InstanceInfo nextServerInfo = DiscoveryManager.getInstance()
                .getDiscoveryClient()
                .getNextServerFromEureka(vipAddress, false);

// eureka server
com.netflix.appinfo.ApplicationInfoManager
// 将应用状态置为 UP
ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.UP);

```
提供者和消费者注册的流程是一样的吗？

eureka 节点之间的数据同步：同一个服务的两个实例如果注册到不同的服务中心实例上，由于服务注册中心之间互相注册为服务，所以服务中心之间会互相转发注册请求服务给集群中的其他服务注册中心，从而实现服务注册中心之间的服务同步。

> It is important to note that Eureka client cleans up the HTTP connections that have been idle for over 30 seconds that it created for the purpose of server communication. 心跳？

eureka 注册信息更新、一级二级缓存、自我保护机制 

- eureka 注册信息的存储结构
注册信息存储在内存中，以处理高并发的访问请求：

```java
// com.netflix.eureka.registry.AbstractInstanceRegistry
// 其中，第一层的 key 是 App name，第二层的 key 是 instance id，而 Lease 维护了每个服务的最后续约时间。
private final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry = new ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>();

// 
```

同时采用了二级缓存，尽可能保证了内存注册表数据不会出现频繁的读写冲突问题。

1. ReadOnlyCacheMap 提供只读的注册信息查询；
2. ReadWriteCacheMap 作为二级缓存，在注册信息变更时过期对应的缓存，並定时同步到 ReadOnlyCacheMap（后台线程 30s 检查一次 ReadWriteCacheMap 是否为空，如果是清空 ReadOnlyCacheMap）；

在服务消费者获取注册信息时（单个服务还是全部服务？），

1. 先查看 ReadOnlyCacheMap，若有则直接返回；没有则查看 ReadWriteCacheMap；
2. 若 ReadWriteCacheMap 有则将其加入 ReadOnlyCacheMap 然后返回；没有则查看 

- Eureka 互相注册可以吗

**Eureka Server 默认模式为多节点**，可以通过 `eureka.client.registerWithEureka=false` 来关闭此行为，以避免日志警告和启动时的节点查找。
服务启动后向 Eureka 注册，Eureka Server 之间相互注册，Eureka Server 会将注册信息向其他 Eureka Server 进行同步，当服务消费者要调用服务提供者，则向服务注册中心获取服务提供者地址，然后会将服务提供者地址缓存在本地，下次再调用时，则直接从本地缓存中取，完成一次调用。

客户端启动时，只需要指定其中一个 Eureka 的地址，该服务的信息就会由 Eureka 自动同步到多个 server 上。**客户端在什么时候得到其他 server 的地址，以在指定的 server 挂了的时候进行切换？**

- Eureka 如何多个通讯交换信息，一个服务可以注册到多个注册中心吗？

可以注册到多个，但没必要，因为注册中心之间会互相通讯，同步注册信息。

> In the case, where the server is not able get the registry information from the neighboring node, it waits for a few minutes (5 mins) so that the clients can register their information. The server tries hard not to provide partial information to the clients there by skewing traffic only to a group of instances and causing capacity issues.

- Eureka 之间的同步机制是怎样的？服务消费者缓存在本地的信息包括哪些？

当 eureka server 启动的时候，会去连接其他的 eureka server，获取服务注册信息，如果从某个 eureka 节点，则尝试从下一个节点获取，直到尝试了所有节点。

当服务提供者发送注册请求到一个服务注册中心时，它会将该请求转发给集群中相连的其他注册中心，从而实现注册中心之间的服务同步。通过服务同步，两个服务提供者的服务信息就可以通过多台服务注册中心的任意一台获取。

服务消费者启动的时候会发送一个 REST 请求给注册中心，获取已注册的服务清单。
为了性能考虑，Eureka Server 会维护一份只读的服务清单来返回给客户端，同时该缓存清单会隔三十秒刷新一次。


`eureka.client.fetch-registry`：获取服务，默认为 true；
`eureka.client.registry-fetch-interval-seconds`：缓存清单的更新时间，默认三十秒。

- Eureka 的自我保护

如果在 15 分钟内，当前注册的服务实例超过 15% 没有续约，并且也没有调用服务注销接口，那么 Eureka 就认为客户端与注册中心出现了网络故障，如果此时 Eureka 仍将这些节点移除並与其他 Eureka Server 进行同步，会导致大部分服务的下线。此时会出现以下几种情况：

1. Eureka 不再从注册列表中移除因为长时间没收到心跳而应该过期的服务；
2. Eureka 仍然能够接受新服务的注册和查询请求，但是不会被同步到其它节点上(即保证当前节点依然可用)；

当服务重新续约，使得无续约实例比例小于 15% 之后，会关闭自我保护。

```
// 配置引发自我保护的节点下线比率
eureka.renewalPercentThreshold=[0.0, 1.0]
// 配置是否启用自我保护，默认为 true
eureka.enableSelfPreservation=false
```
- 为什么使用 eureka 不使用 zk？

zk 主要是实现注册信息在多个节点之间的同步，但是 eureka 的功能除了注册信息同步之外，还有：

1. 提供 RESTful 接口用于与客户端交互；
2. 可以处理 client 与 server 之间，不同 server 节点之间的网络故障；
3. 通过心跳更新注册信息。

- Eureka client 断开一段时间注册信息会怎

client 的状态转换为：STARTING->UP->DOWN。一开始的状态是 STARTING，以给客户端一段时间用于完成自身的初始化工作。

client 默认 30 秒发送一次心跳，如果 Eureka 超过 90 秒没有接收到 client 的心跳，则将 client 状态修改为 DOWN。

- 如何新增 eureka server 节点？

可以通过配置的动态更新（例如通过配置中心），来使得集群中的 eureka 节点感知到新加入的节点。

- Ribbon 和 Zuul 的区别

Zuul 是负责外部调用内部服务的时候进行统一的鉴权、流量过滤、服务路由等；而 Ribbon 是负责内部服务之后相互调用时基于负载均衡算法选择一个可用服务提供者。

---
参考：
- [hystrix 文档](https://github.com/Netflix/Hystrix/wiki/How-it-Works)
- [hystrix 配置项](https://github.com/Netflix/Hystrix/wiki/Configuration)
