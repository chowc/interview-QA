#### feign

feign 整合了 ribbon 和 hystrix，feign 具有如下特性：

1. 可插拔的注解支持，包括 feign 注解和 JAX-RS 注解；
2. 支持可插拔的 HTTP 编码器和解码器；
3. 支持 hystrix 和它的 fallback；
4. 支持 ribbon 的负载均衡；
5. 支持 HTTP 请求和响应的压缩。

- feign 使用 ribbon
```java
public class Example {
  public static void main(String[] args) {
    MyService api = Feign.builder()
          .client(RibbonClient.create())
          .target(MyService.class, "https://myAppProd");
  }
}
```

当为一个接口添加 `@FeignClient` 注解的时候，如果当前项目引用了 ribbon，则 feignclient 会自动使用 ribbon 的负载均衡。

配置 ribbon：
```
// 配置全局的 ribbon 超时时间，注意 hystrix 的超时时间需要大于 ribbon 的超时时间，不然不会触发重试。
ribbon.connectTimeout=500
// 为特定服务配置超时：hello-service 即为 @FeignClient 注解中的 value。
hello-service.ribbon.connectTimeout=500
```

- feign 使用 hystrix
```java
public class Example {
  public static void main(String[] args) {
    MyService api = HystrixFeign.builder().target(MyService.class, "https://myAppProd");
  }
}
```

在默认情况下，spring cloud feign 会将所有 feign client 的方法都封装到 hystrix 命令中进行服务保护。
在 spring cloud 中的配置：

1. 配置文件开启：`feign.hystrix.enabled=true`

2. feign client 添加配置
```java
// 指定熔断的类，方法熔断时会调用熔断类实现的同一方法
@FeignClient(value = "hello-service", fallback = HelloServiceHystric.class)
public interface HelloService {
    @RequestMapping(value = "/hi",method = RequestMethod.GET)
    String sayHiFromClientOne(@RequestParam(value = "name") String name);
}

// 熔断类实现 feign client 注解的接口
@Component
public class HelloServiceHystric implements HelloService {
    @Override
    public String sayHiFromClientOne(String name) {
        return "sorry "+name;
    }
}
```