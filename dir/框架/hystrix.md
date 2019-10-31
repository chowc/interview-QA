### hystrix

hystrix 在分布式系统中提供了服务熔断、降级、缓存、调用合并等诸多功能，用于防止单个服务的宕机而引起的级联异常。

hystrix 主要使用了命令模式，将方法的调用封装为 `HystrixCommand` 或 `HystrixObservableCommand`，再由这些 Command 对象执行对远程服务的调用。

- 启用
```java
@SpringBootApplication
@EnableDiscoveryClient
// 添加注解
@EnableHystrix
public class MyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyApplication.class, args);
	}

	@Bean
	@LoadBalanced
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
```

为服务添加：
```java
@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;

    // 配置熔断方法
    @HystrixCommand(fallbackMethod = "hiError")
    public String hiService(String name) {
        return restTemplate.getForObject("http://SERVICE-HI/hi?name="+name,String.class);
    }

    public String hiError(String name) {
        return "hi,"+name+",sorry,error!";
    }
}
```
- hystrix 的处理流程

![image](../img/how_hystrix_works.jpeg)

1. 构造一个 HystrixCommand 或 HystrixObservableCommand 对象，用于封装请求，并在构造方法配置请求被执行需要的参数；
2. 执行命令，hystrix 提供了 4 种执行命令的方法：`execute/queue/observe/toObservable`；
3. 判断是否使用缓存响应请求，若启用了缓存，且缓存可用，直接使用缓存响应请求；hystrix 支持请求缓存，但需要用户自定义启动；
4. 判断熔断器是否打开，如果打开，跳到第 8 步，不计入监控信息中；
5. 判断线程池/队列/信号量是否已满，已满则跳到第 8 步；
6. 执行 `HystrixObservableCommand.construct()` 或 `HystrixCommand.run()`，如果执行失败或者超时，跳到第 8 步；否则，跳到第 9 步；
7. 统计熔断器监控信息；
8. 执行 fallback 逻辑；
9. 返回请求响应。

Command 的请求方式，execute() 和 queue() 适用于 HystrixCommand 对象，而 observe() 和 toObservable() 适用于 HystrixObservableCommand 对象。

1. execute：阻塞；
2. queue：非阻塞，返回一个 Future 对象；
3. observe：`call(Subscriber<? super String> subscriber)` 方法在调用 `command.observe()` 的时候就开始执行，通过 ReplayObject 使得后续加入的 Subscriber 不会丢失 `Observer.onNext` 的结果，通过在 call 里面调用多次 onNext 来返回多个结果；
4. toObservable：`call(Subscriber<? super String> subscriber)` 方法在调用 `observable.subscribe` 的时候才开始执行，通过在 call 里面调用多次 onNext 来返回多个结果。

execute/queue：run 方法和 execute/queue 方法在不同的线程中执行，通过 Future 来获取结果或者阻塞；

observer/toObservable：和 call 方法在同一个线程内执行。

#### hystrix 的缓存

- 开启缓存
```java
public class CommandUsingRequestCache extends HystrixCommand<Boolean> {

    private final int value;

    protected CommandUsingRequestCache(int value) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.value = value;
    }

    @Override
    protected Boolean run() {
        return value == 0 || value % 2 == 0;
    }

    // 通过覆盖 getCacheKey 来定义缓存的 key，如果缓存的 key 返回 null，则不进行缓存
    @Override
    protected String getCacheKey() {
        return String.valueOf(value);
    }
}
```

hystrix 通过 `com.netflix.hystrix.HystrixRequestCache` 实现请求的缓存，对于新创建的 HystrixRequestContext 实例，之前的缓存会被清空（会在 `context.shutdown()` 中清空 HystrixRequestVariableDefault	）。


在同一用户请求的上下文中，相同依赖服务的返回数据始终保持一致。在当次请求内对同一个依赖进行重复调用，只会真实调用一次。在当次请求内数据可以保证一致性。
```java
@RestController
public class ConsumerController {
    @Autowired
    private  RestTemplate restTemplate;

    @RequestMapping("/consumer")
    public String helloConsumer() throws ExecutionException, InterruptedException {
    	// 在一个 tomcat 请求的上下文中对外部同一接口进行多次调用，可以使用缓存。
		HystrixRequestContext.initializeContext();
		HelloServiceCommand command = new HelloServiceCommand("hello",restTemplate);
		String execute = command.execute();
		HelloServiceCommand command1 = new HelloServiceCommand("hello",restTemplate);
		String execute1 = command1.execute();
		//清理缓存 
		// HystrixRequestCache.getInstance("hello").clear();
 　　	return null; 
	}
}
```

- 在数据更新时删除缓存

2. 缓存的是什么信息？
3. 如何删除缓存？

#### 断路器

`static class HystrixCircuitBreakerImpl implements HystrixCircuitBreaker`

HystrixCircuitBreakerImpl 通过 HealthCounts 记录了一个滚动时间窗内的请求信息快照，默认时间窗为 10 秒。

触发的条件：如果请求总数超出 circuitBreakerRequestVolumeThreshold（默认值为 20）且错误百分比（包括调用失败+超时+线程池拒绝+信号量拒绝）大于 circuitBreakerErrorThresholdPercentage（默认值 50），就将断路器打开。之后的请求都会被拒绝而调用 fallback。

断路器打开的持续时间为 circuitBreakerSleepWindowInMilliseconds（默认为 5s），当断路器的打开时间已经超过这个时长之后，允许尝试一次请求，此时断路器为半开状态，如果该次请求成功，则将断路器关闭，否则继续等待该时长。

- 熔断器的配置参数
```
// 是否启用熔断器，默认是TRUE
circuitBreaker.enabled
// 熔断器强制打开，始终保持打开状态，不关注熔断开关的实际状态。默认值FLASE
circuitBreaker.forceOpen
// 熔断器强制关闭，始终保持关闭状态，不关注熔断开关的实际状态。默认值FLASE
circuitBreaker.forceClosed
// 失败阈值，默认值50%
circuitBreaker.errorThresholdPercentage
// 允许失败总数，默认 20
circuitBreaker.requestVolumeThreshold
// 断路器关闭持续时间，超过这个时间后允许进行试探请求
circuitBreaker.sleepWindowInMilliseconds
```

断路器监控数据的生成：事件流以及统计。

- command name、group 以及线程池的划分

hystrix 通过 Command.threadPoolKey 为 key，创建线程池 `HystrixThreadPool`，当没有指定 threadPoolKey 的时候就使用 groupKey，线程池大小默认为 10（core=max=10），队列默认使用 SynchronusQueue。

配置 groupKey 和 threadPoolKey：
```java
@HystrixCommand(commandKey="commandKey", groupKey="groupKey", threadPoolKey="threadPoolKey")
public User getUserByid(Long id) {
	// do something
}
```
groupKey 默认取的是类名 `getClass().getSimpleName();`，commandKey 默认取方法名。

线程池的配置：
```
// 配置队列大小，默认为 -1，即使用 SynchronousQueue
hystrix.threadpool.default.maxQueueSize=-1
hystrix.threadpool.default.coreSize=10
// 需要设置 allowMaximumSizeToDivergeFromCoreSize，此配置才会生效
// default=false
hystrix.threadpool.default.allowMaximumSizeToDivergeFromCoreSize=false
hystrix.threadpool.default.maximumSize=10
// keep alive time，单位分钟
hystrix.threadpool.default.keepAliveTimeMinutes=1
// 最大排队数，不是任务队列的大小，即使任务队列未满或者线程池未达 maximumSize，只要排队请求大于这个数就会被拒绝。用于通过动态配置调整排队数，因为线程池的队列大小不能动态修改。
hystrix.threadpool.default.queueSizeRejectionThreshold=5
```

通过线程池进行隔离的方式对于大多数场景都是适合的，但线程池的增加会导致响应时间的轻微加大，因此对于响应时间本来就很小的服务可以使用信号量的方式，但是信号量不能支持异步以及超时调用。

配置使用信号量进行服务隔离：

```
execution.isolation.strategy=SEMAPHORE
// 信号量的数量，默认为 10，当并发请求大于 10 时会拒绝后续请求。
execution.isolation.semaphore.maxConcurrentRequests=10
```

信号量会在两种场景中使用：

1. 命令执行：当配置了使用信号量来控制请求并发数时；
2. 降级逻辑：hystrix 会在降级逻辑中使用信号量（**作用是？**）。

#### 降级发生的条件

以下任一条件均会触发 fallback：
1. 断路器是开启状态且未到请求尝试时间；
2. 线程池/队列/信号量达到上限；
3. 请求超时；
4. 执行过程发生异常（除 HystrixBadRequestException 外）

自定义不触发 fallback 的异常：
```java
@HystrixCommand(ignoreExceptions = {MyException.class})
public User getUserByid(Long id) {
	// do something
}
```