### Spring

1. Spring MVC 的请求处理流程

![image](spring_mvc_request.png)

2. Spring IOC 的实现

IOC 有三种实现方式（《Spring 揭秘》2.2）：构造方法注入、setter 方法注入、以及接口注入。

- 构造方法注入

被注入对象可以通过在其构造方法中声明依赖对象的参数列表，让外部（通常是 IOC 容器）知道它需要哪些依赖对象。

- setter 方法注入

当前对象只要为其依赖对象所对应的属性添加 setter 方法，就可以通过 setter 方法将相应的依赖对象设置到被注入对象中。

- 接口注入

被注入对象如果想要 IOC 容器为其注入依赖对象，就需要实现某个接口。这个接口提供一个方法，用来为其注入依赖对象。

3. Spring AOP 的实现

代理的实现可以分为编译时的字节码增强或者是运行时代理，前者例如 AspectJ，后者则包括 Java 中的动态代理以及 cglib。

[Spring AOP 的实现主要是使用了 Java 动态代理以及 cglib](https://docs.spring.io/spring/docs/2.5.x/reference/aop.html)，因为 Java 动态代理需要目标类实现了某个接口，因此对于实现了接口的代理类采用的是 Java 动态代理，而没有实现接口的类则通过 cglib 生成代理类的子类，因此对于 final 修饰的类无法通过 cglib 生成代理类。

- Java 动态代理的实现

通过 `java.lang.reflect.Proxy.newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h)` 来动态生成代理类，代理类本身实现了传入的接口类并且继承了 Proxy，对被代理对象的实际方法调用是通过  `InvocationHandler.invoke(Object proxy, Method method, Object[] args)` 完成的。

- Java 动态代理和 cglib 动态代理的区别（经常结合 spring 一起问所以就放这里了）

1. 实现的方式不同，Java 动态代理是通过动态生成一个实现了被代理接口的类，并且通过 `InvocationHandler.invoke(Object proxy, Method method, Object[] args)` 进行委托调用的；而 cglib 是通过动态生成被代理对象的子类；
2. Java 动态代理的对象需要实现某个接口，而 cglib 的代理对象不需要；但是 cglib 代理的对象类不能是 final 的，代理方法不能是 final 的。

- spring 中 bean 的生命周期是怎样的？

当 IOC 容器是 ApplicationContext 时，singleton bean 的生命周期如下图，其中，如果使用的不是 ApplicationContext 而是 BeanFactory 的话，则 “调用 ApplicationContextAware 的 setApplicationContext 方法”这一阶段不会出现，另外这一阶段实际上是在 ApplicationContextAwareProcessor 这个 BeanPostProcessor 的 `postProcessBeforeInitialization` 方法中执行的。

![image](../img/spring_bean_lifecycle.jpg)

- setter 方法注入和构造器注入哪种会有循环依赖的问题？

构造器注入的方式会有循环依赖的问题。换成 setter 方法注入即可解决这个问题。因为 setter 方法注入会首先调用默认构造函数来实例化对象，然后再调用 setter 实现依赖注入。这样在对象实例化的阶段就没有了任何依赖。

- spring 的事务传播以及回滚

7 种事务传播设置：

1. PROPAGATION_REQUIRED：支持当前事务，如果当前没有事务，就新建一个事务。默认选项。
2. PROPAGATION_SUPPORTS：支持当前事务，如果当前没有事务，就以非事务方式执行。
3. PROPAGATION_MANDATORY：支持当前事务，如果当前没有事务，就抛出异常。 
4. PROPAGATION_REQUIRES_NEW：新建事务，如果当前存在事务，把当前事务挂起。 
5. PROPAGATION_NOT_SUPPORTED：以非事务方式执行操作，如果当前存在事务，就把当前事务挂起。 
6. PROPAGATION_NEVER：以非事务方式执行，如果当前存在事务，则抛出异常。
7. PROPAGATION_NESTED：如果当前存在事务，则在当前事务的一个*嵌套事务*中执行；如果当前没有事务，就创建新的事务。

如果在 Spring 使用嵌套事务，需要满足以下 3 点：

1. 数据库支持，嵌套事务是使用数据库的 SavePoint(事务保存点)。

可以使用以下代码来判断数据库是否支持。
```java
Connection.getMetaData().supportsSavepoints();
```
2. JDK 1.4 才支持 `java.sql.Savepoint`。所以 JDK 必须在 1.4 及以上。

3. 需要 TransactionManager 的支持。

- Spring 中的只读事务如何配置？如何提升性能的？针对 MySQL/Oracle？

通过 `@Transactional(readOnly = true)` 配置只读事务。

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

