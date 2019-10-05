#### 参考

- 《高性能MySQL》: 附录D
- [MySQL explain-output](https://dev.mysql.com/doc/refman/5.7/en/explain-output.html)
---
explain 的输出如下：

```txt
mysql> explain select * from servers;
+----+-------------+---------+------+---------------+------+---------+------+------+-------+
| id | select_type | table   | type | possible_keys | key  | key_len | ref  | rows | Extra |
+----+-------------+---------+------+---------------+------+---------+------+------+-------+
|  1 | SIMPLE      | servers | ALL  | NULL          | NULL | NULL    | NULL |    1 | NULL  |
+----+-------------+---------+------+---------------+------+---------+------+------+-------+
row in set (0.03 sec)
```

- id

1. id 相同时，执行顺序由上至下；不同 id 执行顺序由大到小；

2. 如果是子查询，id 的序号会递增，id 值越大优先级越高，越先被执行；

- select_type：表示查询中每个 SELECT 子句的类型

1. SIMPLE（简单 SELECT,不使用 UNION 或子查询等)
2. PRIMARY(查询中若包含任何复杂的子部分,最外层的 SELECT 被标记为 PRIMARY)
3. UNION(UNION 中的第二个或后面的 SELECT 语句)
4. DEPENDENT UNION(UNION 中的第二个或后面的 SELECT 语句，取决于外面的查询)
5. UNION RESULT(UNION 的结果)
6. SUBQUERY(子查询中的第一个 SELECT)
7. DEPENDENT SUBQUERY(子查询中的第一个 SELECT，取决于外面的查询)
8. DERIVED(派生表的 SELECT)
9. UNCACHEABLE SUBQUERY(一个子查询的结果不能被缓存，必须重新评估外链接的每一行)

- table：显示这一行的数据是关于哪张表的，有时不是真实的表名字，而是派生表 derivedx，表示是 id=x 的语句执行生成的派生表；

- type：这一列表示关联类型或访问类型，即 MySQL 决定如何查找表中的行。

依次从最优到最差分别为：system > const > eq_ref > ref > fulltext > ref_or_null > index_merge > unique_subquery > index_subquery > range > index > ALL。

ALL：Full Table Scan， MySQL 将遍历全表以找到匹配的行。（这里也有个例外，例如在查询里使用了 LIMIT，或者在 Extra 列里显示 “Using Distinct/not exists”）

index：Full Index Scan，这个跟全表扫描一样，只是 MySQL 扫描表时按索引次序进行而不是行。它的主要优点是避免了排序。**如果 Extra 列是 Using Index，则是索引覆盖；否则表示按索引顺序做全表扫描（随机 I/O）**。

> If the index is a covering index for the queries and can be used to satisfy all data required from the table, only the index tree is scanned. In this case, the Extra column says Using index. An index-only scan usually is faster than ALL because the size of the index usually is smaller than the table data.
>
> A full table scan is performed using reads from the index to look up data rows in index order. Uses index does not appear in the Extra column.

range：只检索给定范围的行，使用一个索引来选择行

ref：相比 eq_ref，不使用唯一索引，而是使用普通索引或者唯一性索引的部分前缀，索引要和某个值相比较，可能会找到多个符合条件的行。

eq_ref：类似 ref，区别就在使用的索引是唯一索引，对于每个索引键值，表中只有一条记录匹配，简单来说，就是多表连接中使用 primary key 或者 unique key 作为关联条件

const、system：当 MySQL 对查询某部分进行优化，并转换为一个常量时，使用这些类型访问。如将主键置于 where 列表中，MySQL 就能将该查询转换为一个常量，system 是 const 类型的特例，当查询的表只有一行的情况下，使用 system；

NULL：MySQL 在优化过程中分解语句，执行时甚至不用访问表或索引，例如从一个索引列里选取最小值可以通过单独索引查找完成。

- possible_keys：这一列显示查询可能使用哪些索引来查找。 

explain 时可能出现 possible_keys 有列，而 key 显示 NULL 的情况，这种情况是因为表中数据不多，MySQL 认为索引对此查询帮助不大，选择了全表查询。 

如果该列是 NULL，则没有相关的索引。在这种情况下，可以通过检查 where 子句看是否可以创造一个适当的索引来提高查询性能，然后用 explain 查看效果。

- key：显示 MySQL 实际决定使用的索引

如果没有选择索引，该列是 NULL。要想强制 MySQL 使用或忽视 possible_keys 列中的索引，在查询中使用 FORCE INDEX、USE INDEX 或者 IGNORE INDEX。

- key_len：这一列显示了 MySQL 在索引里使用的字节数，通过这个值可以算出具体使用了索引中的哪些列。 

举例来说，film_actor 的联合索引 idx_film_actor_id 由 film_id 和 actor_id 两个 int 列组成，并且每个 int 是4字节。通过结果中的 key_len=4 可推断出查询使用了第一个列：film_id 列来执行索引查找。*如果 key 列是 NULL，此列也为 NULL。*对于允许为 NULL 的列，key_len 的值是列长度+1。

- ref

这一列显示了在 key 列记录的索引中，表查找值所用到的列或常量，常见的有：const（常量）、func、NULL、字段名（例：film.id）。

- rows

这一列是 MySQL 估计要读取并检测的行数，注意这个不是结果集里的行数。对于 InnoDB 引擎的表来说，该值只是估计的。

- Extra：这一列展示的是额外信息。常见的重要值如下： 

Using index：这发生在对表的请求列都是同一索引的部分的时候，返回的列数据只使用了索引中的信息，而没有再去访问表中的行记录。是性能高的表现。

Using where：MySQL 服务器将在存储引擎检索行后再进行过滤。

Using temporary：MySQL 需要创建一张临时表来处理查询，常见于排序、分组查询、DISTINCT。出现这种情况一般是要进行优化的，首先是想到用索引来优化。

Using filesort：MySQL 会对结果使用一个外部索引排序。这种情况下一般也是要考虑使用索引来优化的。

