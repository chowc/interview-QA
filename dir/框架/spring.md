### Spring

1. Spring MVC 的请求处理流程
2. Spring IOC 的实现
3. Spring AOP 的实现


#### Spring Boot

- springboot启动方式/启动流程

1. 运行启动类的 main 方法；
2. 打包成 Jar 包后运行；
3. 通过 `mvn spring-boot:run` 运行。


#### Spring Cloud

- springcloud 组件有哪些

服务注册中心 eureka，客户端负载均衡 Ribbon，远程调用 Feign、断路器 Hystrix、配置中心 Config、路由及权限控制、负载均衡的 Zuul、消息总线 Bus。

- eureka互相注册可以吗

服务启动后向Eureka注册，Eureka Server 之间相互注册，Eureka Server会将注册信息向其他Eureka Server进行同步，当服务消费者要调用服务提供者，则向服务注册中心获取服务提供者地址，然后会将服务提供者地址缓存在本地，下次再调用时，则直接从本地缓存中取，完成一次调用。

- eureka如何多个通讯交换信息，一个服务可以注册到多个注册中心吗

可以注册到多个，但没必要，因为注册中心之间会互相通讯，同步注册信息。

- Eureka 之间的同步机制是怎样的？服务消费者缓存在本地的信息包括哪些？

- 熔断降级如何做的

- eureka断开一段时间注册信息会怎样

Eureka 客户端的状态变成：STARTING->UP->DOWN。客户端默认 30 秒发送一次心跳，如果 Eureka 超过 90 秒没有接收到客户端的心跳，则认为客户端已经 DOWN 了。

