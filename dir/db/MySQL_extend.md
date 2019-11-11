### [MySQL 中的类型转换](https://dev.mysql.com/doc/refman/5.7/en/type-conversion.html)

- 两行 SQL 返回什么结果？
```sql
create table t (id varchar(10), name varchar(10));
insert into t values ('1', 'ming'), ('1a','kong'),('21', 'user');

select * from t where id=1;# ('1', 'ming'), ('1a','kong')
select * from t where id='1'; # ('1', 'ming')
```

> For example, a comparison of string and numeric operands takes places as a comparison of floating-point numbers.

字符串与数字做比较，会将字符串转换为数字，而 '1', 1a', ' 1' 都会转换成 1，因此第一条查询会返回两条结果，**这也是为什么当索引列使用了函数之后索引会失效的原因，因为多了很多符合条件的数据，而这些数据跟符合条件的索引节点並不在一起（'1', ' 1' 不在同一个节点）。**

---
### MySQL 读写分离后，如何保证一个事务的读能够访问到同一事务写入的数据？

一个思路：事务查询走主库，非事务查询走从库（即不添加 `@Transactional` 注解，虽然数据库上运行仍然是开启了事务的），对于只读的查询来说，开不开启事务並没有什么区别。
