- RPC 的三个关注点
1. 通信方式
2. 序列化和反序列化
3. 寻址

- 常见的 RPC 有哪些？对应的区别和性能比较？

RPC 的目的是让你在本地调用远程的方法，而对你来说这个调用是透明的，你并不知道这个调用的方法部署在哪里。

1. Java RMI：Java 原生提供的方式，采用 Java 序列化机制。
2. Dubbo：支持多种序列化方式（hessian、json、Java 序列化）、多种调用方式（TCP、HTTP）、不同的注册中心等。
3. Hessian
4. gRPC：多语言，采用 ProtoBuf 序列化方式，效率高。
5. thrift
6. Spring Cloud：全套解决方案（配置管理，服务发现，熔断，路由，微代理，消息总线等）。

- RPC 和 RESTful 的比较