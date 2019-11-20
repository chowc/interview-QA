#### Redis 中的数据结构
1. string：类似 arraylist 的结构，允许对其进行修改和追加等；
2. hash：map；
3. list；
4. set；
5. sorted set：使用 skip list 的方式实现排序。

- 为什么 sorted set 使用 skip list 实现而不是平衡树？

sorted set 经常要按照范围返回元素。
> A sorted set is often target of many ZRANGE or ZREVRANGE operations, that is, traversing the skip list as a linked list. 

https://stackoverflow.com/questions/45115047/why-redis-sortedset-uses-skip-list-instead-of-balanced-tree

- Redis 清除过期键的方式

1. 被动删除：当读/写一个已经过期的key时，会触发惰性删除策略，直接删除掉这个过期 key。

只有key被操作时(如GET)，REDIS才会被动检查该key是否过期，如果过期则删除之并且返回NIL。
1、这种删除策略对CPU是友好的，删除操作只有在不得不的情况下才会进行，不会对其他的expire key上浪费无谓的CPU时间。
2、但是这种策略对内存不友好，一个key已经过期，但是在它被操作之前不会被删除，仍然占据内存空间。如果有大量的过期键存在但是又很少被访问到，那会造成大量的内存空间浪费。

但仅是这样是不够的，因为可能存在一些key永远不会被再次访问到，这些设置了过期时间的key也是需要在过期后被删除的，我们甚至可以将这种情况看作是一种内存泄露—-无用的垃圾数据占用了大量的内存，而服务器却不会自己去释放它们，这对于运行状态非常依赖于内存的Redis服务器来说，肯定不是一个好消息。

2. 主动删除：由于惰性删除策略无法保证冷数据被及时删掉，所以Redis会定期主动淘汰一批已过期的 key

在一个定期执行的 cron job（对应 serverCron 执行文件） 中执行删除动作。在 Redis 2.6 版本中， 程序规定 cron job 每秒运行 10 次， 平均每 100 毫秒运行一次。 从 Redis 2.8 开始， 用户可以通过修改 hz选项来调整 cron job 的每秒执行次数

Redis会周期性的随机测试一批设置了过期时间的key并进行处理。测试到的已过期的key将被删除。典型的方式为,Redis每秒做10次如下的步骤：

	1. 随机测试100个设置了过期时间的key
	2. 删除所有发现的已过期的key
	3. 若删除的key超过25个则重复步骤1

3. 当前已用内存超过maxmemory限定时，触发主动清理策略

清理策略有：
	1. volatile-lru：在设置了过期时间的键集合中按照 LRU 删除（默认值）；
	2. allkeys-lru：在所有键集合中按照 LRU 删除；
	3. volatile-random：在设置了过期时间的键集合中，随机删除；
	4. allkeys-random：在所有键集合中随机删除；
	5. volatile-ttl：在设置了过期时间的键集合中，优先回收存活时间 TTL 较小的键； 
	6. noeviction：永不过期，返回错误（DEL 指令和几个例外）。

当mem_used内存已经超过maxmemory的设定，对于所有的读写请求，都会触发redis.c/freeMemoryIfNeeded(void)函数以清理超出的内存。注意这个清理过程是阻塞的，直到清理出足够的内存空间。所以如果在达到maxmemory并且调用方还在不断写入的情况下，可能会反复触发主动清理策略，导致请求会有一定的延迟。 

清理时会根据用户配置的maxmemory-policy来做适当的清理（一般是LRU或TTL），这里的LRU或TTL策略并不是针对redis的所有key，而是以配置文件中的maxmemory-samples个key作为样本池进行抽样清理。（随机选择若干个键执行清理策略，maxmemory-samples 在 redis-3.0.0 中的默认配置为 5）。

- 主从复制中的清除策略

为了获得正确的行为而不至于导致一致性问题，当一个key过期时DEL操作将被记录在AOF文件并传递到所有相关的slave。也即过期删除操作统一在master实例中进行并向下传递，而不是各salve各自掌控。这样一来便不会出现数据不一致的情形。当slave连接到master后并不能立即清理已过期的key（需要等待由master传递过来的DEL操作），slave仍需对数据集中的过期状态进行管理维护以便于在slave被提升为master会能像master一样独立的进行过期处理。

#### 持久化的方式

- RDB

原理
缺点

- AOF

原理

缺点

- 是否会丢失数据？

#### 主从的原理
#### Redis 集群模式

- 哨兵
- 集群

---
参考：

- [stackoverflow: why-redis-sortedset-uses-skip-list-instead-of-balanced-tree](https://stackoverflow.com/questions/45115047/why-redis-sortedset-uses-skip-list-instead-of-balanced-tree)
- [redis 数据过期策略](https://www.cnblogs.com/chenpingzhao/p/5022467.html)