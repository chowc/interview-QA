#### 主流消息中间件的比较

---
### RabbitMQ

#### 基本概念

基本概念: virtual host，exchange，queue，binding，binding key，routing key

![image](rabbitmq_msgsend.png)

- exchange 交换机类型

1. fanout：把发送到该交换器的消息路由到所有与该交换器绑定的队列上；
2. direct：把消息路由到 RoutingKey 与 BindingKey 完全一致的队列上；
3. topic：把消息路由到 RoutingKey 匹配了 BindingKey 的队列上，如 RoutingKey “com.rabbitmq.com" 匹配到 "*.rabbitmq.*"，* 用于表示一个单词，# 用于表示任意个单词，单词之间通过 . 分隔；
4. headers：根据消息内容中的 headers 属性进行匹配，性能差，不实用。

- Connection、channel

> 一个 Connection 代表到 MQ 服务器的一条 TCP 连接，一旦 TCP 连接建立起来，客户端紧接着可以创建一个 AMQP 信道 (Channel) ，每个信道都会被指派一个唯一的 ID。信道是建立在 Connection 之上的虚拟连接， RabbitMQ 处理的每条 AMQP 指令都是通过信道完成的。
>
> 我们完全可以直接使用 Connection 就能完成信道的工作，为什么还要引入信道呢?试想这样一个场景， 一个应用程序中有很多个线程需要从 RabbitMQ 中消费消息，或者生产消息，那么必然需要建立很多个 Connection，也就是许多个 TCP 连接。然而对于操作系统而言，建立和销毁 TCP 连接是非常昂贵的开销，如果遇到使用高峰，性能瓶颈也随之显现。 RabbitMQ 采用类似 NIO 的做法，选择 TCP 连接复用，不仅可以减少性能开销，同时也便于管理。
>
> *每个线程把持一个信道*（channel 不是线程安全的，即使某些方法是线程安全的，即多个线程共用一个 channel 的话需要进行额外加锁，所以一般推荐对一个 channel 建立一个线程，一个 channel 就代表了一个消息的消费者），所以信道复用了 Connection 的 TCP 连接。同时 RabbitMQ 可以确保每个线程的私密性，就像拥有独立的连接一样。当每个信道的流量不是很大时，复用单一的 Connection 可以在产生性能瓶颈的情况下有效地节省 TCP 连接资源。但是当信道本身的流量很大时，这时候多个信道复用一个 Connection 就会产生性能瓶颈，进而使整体的流量被限制了。此时就需要开辟多个 Connection，将这些信道均摊到这些 Connection 中。

- queue

队列属性：

1. durable
2. exclusive
3. autodelete

- `channel.basicConsume` 是客户端拉取还是 MQ 服务器的推送？

消费的两种模式：推和拉

> Basic.Consume 将信道 (Channel) 直为接收模式，直到取消队列的订阅为止。在接收模式期间， RabbitMQ 会不断地推送消息给消费者，当然推送消息的个数还是会受到 Basic.Qos 的限制.如果只想从队列获得单条消息而不是持续订阅，建议还是使用 Basic.Get 进行消费.但是不能将 Basic.Get 放在一个循环里来代替 Basic.Consume ，这样做会严重影响 RabbitMQ 的性能.如果要实现高吞吐量，消费者理应使用 Basic.Consume 方法。

#### 发送方消息确认

默认情况下发生消息的操作时不会返回任何消息给生产者的，也就是默认情况下生产者是不知道消息有没有正确地到达服务器。如果在消息到达服务器之前已经丢失，持久化操作也解决不了这个问题。

RabbitMQ 提供了两种解决方式：

1. 通过事务机制实现
2. 通过发送方确认（publisher confirm）机制实现

- 事务机制

事务机制在一条消息发送之后会使发送方阻塞，以等待 RabbitMQ 的回应，之后才能继续发送下一条消息。
**使用事务机制会严重降低 RabbitMQ 的消息吞吐量。**

- 发送方确认机制

生产者将信道设置成 confirm 模式，一旦信道进入 confirm 模式，所有在该信道上面发布的消息都会被指派一个唯一的 ID（从 1 开始），一旦消息被投递到所有匹配的队列之后，RabbitMQ 就会发送一个确认（`Basci.ACK`）给生产者（包含消息的唯一 ID），这就使得生产者知晓消息已经正确地到达了目的地了。

发送方确认的优势在于并不一定需要同步确认，可以有以下两种确认方式：

    1. 批量确认：每发送一批消息后，调用 `channel.waitForConfirms` 方法，等待服务器的确认返回（此时是阻塞的）；
    2. 异步确认：提供一个回调方法，服务端确认了一条或者多条消息后客户端会回调这个方法进行处理。

对于批量确认，如果出现返回 `Basic.Nack` 或者超时情况时，客户端需要将这一批次的消息全部重发，这会带来明显的重复消息数量，并且当消息经常丢失时，批量确认的性能是不升反降的。

消息发送确认模式开启之后，每条消息都会基于同一个信道下新增一个投递标签(deliveryTag)属性，deliveryTag 属性是从 1 开始递增的整数，*每个 channel 有自己的 deliverTag 下标标识（nextPublishSeqNo），新建的 channel 其 deliverTag 初始值是 0*，在调用 `confirmSelect()` 方法时判断如果 deliverTag 是 0 的话，就将它置为 1，即可用的 deliverTag 是从 1 开始的。这个消息投递标签和消息消费中的信封(Envelope)中的 deliveryTag 不是同一个属性，后者虽然也是从 1 开始递增，但是它是基于队列而不是信道。

#### 消费确认

消费者客户端可以通过推或者拉模式的方式来获取并消费消息，当消费者处理完业务逻辑需要手动确认消息已被接收，这样 RabbitMQ 才能把当前消息从队列中清除。如果消费者由于某些原因无法处理当前接收到的消息，可以通过 `channel.basciNack` 或者 `channel.basicReject` 来拒绝掉。

这里并没有用到超时机制，RabbitMQ 仅通过 Consumer 的连接中断来确认是否需要重新发送消息。也就是说，只要连接不中断，RabbitMQ 给了 Consumer 足够长的时间来处理消息。

下面罗列几种特殊情况：

1. 如果消费者接收到消息，在确认之前断开了连接或取消订阅，RabbitMQ 会认为消息没有被分发，然后重新分发给下一个订阅的消费者。（可能存在消息重复消费的隐患，需要根据 bizId 去重）；
2. 如果消费者接收到消息却没有确认消息，连接也未断开，则 RabbitMQ 认为该消费者繁忙，将不会给该消费者分发更多的消息。

- 消息如何分发？

若该队列至少有一个消费者订阅，消息将以循环（round-robin）的方式发送给消费者。每条消息只会分发给一个订阅的消费者（前提是消费者能够正常处理消息并进行确认）。

#### 生产者-消费者模式与发布-订阅模式

生产者消费者模式中，同一条消息只能被一个消费者消费到（不考虑重发的情况下），而发布订阅模式中，同一条消息会被多个订阅者消费；

因为同一个队列中的一条消息只能被一个消费方（可能是消费者，也可以是订阅者）消费，所以发布订阅模式是通过将一条消息路由到多个队列来实现多分发的。

#### 消费的线程模式

消息接收线程、业务处理线程、业务队列。

#### 死信

- 消息变成死信的原因：

    1. 消息被拒绝（`Basic.Reject/Basic.Nack`），并且设置 requeue 参数为 false；
    2. 消息过期；
    3. 队列达到最大长度。
    
- 采用死信队列做延时队列

死信交换器（Dead-Letter-Exchange，DLX）也是一个正常的交换器，和一般的交换器没有区别，它能在任何的队列上被指定，实际上就是设置某个队列的属性。当这个队列中存在死信时，RabbitMQ 就会自动将这个消息重新发布到设置的 DLX 上去，进而被路由到另一个队列，即死信队列。

假设一个应用中需要将每条消息都设置为 10 秒的延迟，生产者通过 `exchange.normal` 这个交换器将发生的消息存储在 `queue.normal` 这个队列中。消费者订阅的并非是 `queue.normal` 这个队列，而是 `queue.dlx` 这个队列。当消息从 `queue.normal` 这个队列中过期之后被存入 `queue.dlx` 队列中，消费者就恰巧消费到了延迟 10 秒的这条消息。

#### 消息持久化

需要同时持久化交换器、队列以及消息。

1. 设置交换器持久化：非持久化的交换器在服务重启时会丢失
```java
exchangeDeclare(String exchange, String type, boolean durable);
```
2. 设置队列持久化：非持久化的队列在服务重启时会丢失

```java
Connection connection = connectionFactory.newConnection();
Channel channel = connection.createChannel();
// Queue.DeclareOk queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
                                 Map<String, Object> arguments) throws IOException;
channel.queueDeclare("queue.persistent.name", true, false, false, null);
```

3. 设置消息模式

```java
AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
// deliverMode：1，不持久化；2，持久化。
builder.deliveryMode(2);
AMQP.BasicProperties properties = builder.build();
channel.basicPublish("exchange.persistent", "persistent",properties, "persistent_test_message".getBytes());
// 或者方便点的方法
channel.basicPublish("exchange.persistent", "persistent", MessageProperties.PERSISTENT_TEXT_PLAIN, "persistent_test_message".getBytes());
```

RabbitMQ 确保持久性消息能从服务器重启中恢复的方式是，将它们写入磁盘上的一个持久化日志文件，当发布一条持久性消息到持久交换器上时，RabbitMQ 会在消息提交到日志文件后才发送响应（如果消息路由到了非持久队列，它会自动从持久化日志中移除）。一旦消费者从持久队列中消费了一条持久化消息，RabbitMQ 会在持久化日志中把这条消息标记为等待垃圾收集。如果持久化消息在被消费之前 RabbitMQ 重启，那么会自动重建交换器和队列（以及绑定），并重播持久化日志文件中的消息到合适的队列或者交换器上。

- 消息什么时候刷到磁盘？

写入文件前会有一个 Buffer，大小为 1M，数据在写入文件时，首先会写入到这个 Buffer，如果 Buffer 已满，则会将 Buffer 写入到文件（未必刷到磁盘）。
有个固定的刷盘时间：25ms，也就是不管 Buffer 满不满，每个 25ms，Buffer 里的数据及未刷新到磁盘的文件内容必定会刷到磁盘。
每次消息写入后，如果没有后续写入请求，则会直接将已写入的消息刷到磁盘：使用 Erlang 的 receive x after 0 实现，只要进程的信箱里没有消息，则产生一个 timeout 消息，而 timeout 会触发刷盘操作。

当开启发送确认模式后，RabbitMQ 会在消息写入磁盘之后返回确认。

#### 高可用

- 集群模式

在 RabbitMQ 集群中创建队列，集群只会在单个节点而不是在所有节点上创建队列的进程並包含完整的队列信息（元数据、状态、内容）。这样只有队列的宿主节点，即所有者节点知道队列的所有信息，所有其他非所有者节点只知道队列的元数据和指向该队列存在的那个节点的指针。因此当集群节点崩溃时，该节点的队列进程和关联的绑定都会消失。附加在那些队列上的消费者也会丢失其所订阅的信息，并且任何匹配该队列绑定信息的新消息也都会消失。

如果关闭了集群中的所有节点，则需要确保在启动的时候最后关闭的节点是第一个启动的。如果第一个启动的不是最后关闭的节点，那么这个节点会等待最后关闭的节点启动。这个等待时间是 30s，如果没有等到，那么这个最先启动的节点也会失败。在最新的版本里会有重试机制，默认重试 10 次 30s 以等待最后关闭的节点启动。

- 镜像队列

集群模式下节点崩溃会导致在该节点上的队列不可用，且消息丢失。镜像队列会将消息复制到多个节点上，实现冗余。

---
- 参考资料

1. 《RabbitMQ 实战指南》
2. https://blog.csdn.net/u013256816/article/details/60875666