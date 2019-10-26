- Mybatis DAO 接口为什么不需要实现类？

Mybatis 通过 JDK 动态代理提供了 Mapper 接口的代理对象，在执行 Mapper 接口方法时，实际执行的是其代理对象，代理对象在 invoke 方法内获取 Mapper “接口类全名+方法全名”作为 statement 的 ID，然后通过 ID 去匹配注册的 SQL，然后使用 SqlSession 执行这个 SQL。

所以，这也解释了为什么 Mybatis 映射文件需要 namespace 和 id，前者是类全名，后者是方法名。

通过 `MapperProxyFactory` 为接口生成代理类，並传入 `MapperProxy` 处理代理逻辑。

```java
public class MapperProxyFactory<T> {
    private final Class<T> mapperInterface;
    private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap();

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public Class<T> getMapperInterface() {
        return this.mapperInterface;
    }

    public Map<Method, MapperMethod> getMethodCache() {
        return this.methodCache;
    }

    protected T newInstance(MapperProxy<T> mapperProxy) {
        return Proxy.newProxyInstance(this.mapperInterface.getClassLoader(), new Class[]{this.mapperInterface}, mapperProxy);
    }

    public T newInstance(SqlSession sqlSession) {
        MapperProxy<T> mapperProxy = new MapperProxy(sqlSession, this.mapperInterface, this.methodCache);
        return this.newInstance(mapperProxy);
    }
}


public class MapperProxy<T> implements InvocationHandler, Serializable {
    private static final long serialVersionUID = -6424540398559729838L;
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<Method, MapperMethod> methodCache;

    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    	// 不是接口类就直接调用对象的方法
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else {
        	// 接口类先缓存，再调用 
            MapperMethod mapperMethod = this.cachedMapperMethod(method);
            return mapperMethod.execute(this.sqlSession, args);
        }
    }

    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = (MapperMethod)this.methodCache.get(method);
        if (mapperMethod == null) {
            mapperMethod = new MapperMethod(this.mapperInterface, method, this.sqlSession.getConfiguration());
            this.methodCache.put(method, mapperMethod);
        }

        return mapperMethod;
    }
}
```

- $ 和 # 的区别

1. 使用 ${} 方式传入的参数，mybatis 不会对它进行特殊处理，而使用 #{} 传进来的参数，mybatis 默认会将其当成字符串而加上双引号；**所以对于传入表名、字段名（分组字段或者排序字段），应使用${}**；
```sql
selec * from #{table};
# table 传入 test
select * from "test";

select * from ${table};
# table 传入 test
select * from test;
```
2. # 和 $ 在预编译处理中是不一样的。# 类似 jdbc 中的 PreparedStatement，对于传入的参数，在预处理阶段会使用 ? 代替，比如：
`select * from student where id = ?;`，待真正查询的时候即在数据库管理系统中（DBMS）才会代入参数。
而 ${} 则是简单的替换，如：`select * from student where id = 2;`

*占位符只能占位 sql 语句中的普通值，决不能占位表名、列名、sql 关键字（select、insert等）；因为 PreparedStatement 的 sql 语句是要预编译的，如果关键字、列名、表名等被占位那就直接代表该 sql 语句语法错误而无法编译，会直接抛出异常，因此只有不影响编译的部分可用占位符占位*。

因此，能使用 #{} 的地方应尽量使用 #{}；#{} 可以有效防止 sql 注入，${} 则可能导致 sql 注入成功。

- 防止 sql 注入的几种方式

1. 在页面输入参数时也进行字符串检测和提交时进行参数检查，同样可以使用正则表达式，不允许特殊符号出现；
2. 在程序代码中使用正则表达式过滤参数。使用正则表达式过滤可能造成注入的符号，如单引号等；
3. jdbc 使用 PreparedStatement 代替 Statement，PreparedStatement  不仅提高了代码的可读性和可维护性.而且也提高了安全性，有效防止 sql 注入。

---

### jdbc

- Statement 和 PrepareStatement 的区别

PrepareStatement 是 Statement 接口的子接口，继承了 Statement 接口的所有功能。

1. 执行方式的不同：Statement 是直接将 sql 语句整句执行，而 PrepareStatement 是抽离参数，通过占位符的方式传入执行参数；因此 PrepareStatement 可读性更好；
```java
// Statement 需要用单引号拼接参数
String sql = "select * from users where  username= '"+username+"' and userpwd='"+userpwd+"'";
stmt = conn.createStatement();
// 将 sql 语句作为 execute 的参数
rs = stmt.executeQuery(sql);
// PrepareStatement 支持占位符
String sql = "select * from users where  username=? and userpwd=?";
pstmt = conn.prepareStatement(sql);
pstmt.setString(1, username);
pstmt.setString(2, userpwd);
rs = pstmt.executeQuery();
```
2. 效率：

Statement 的 execute 系列方法直接将 sql 语句作为参数传入并提交给数据库执行，也就是说每提交一次都需要先经过编译然后再执行；

使用 PrepareStatement 时会直接将该 sql 语句提交给数据库进行编译，得到的 PreparedStatement 句柄其实是一个预编译好的 sql 语句；之后调用 PreparedStatement 的 execute 方法（其 execute 系列方法都是无参的），就直接将该预编译的语句提交给数据库直接运行而不需要再编译一次了。

但对于只执行一次的语句，因为 PrepareStatement 的创建成本比 Statement 高，因此适合使用 Statement。

> 在使用 PreparedStatement 执行 sql 命令时，命令会带着占位符被数据库进行编译和解析，并**放到命令缓冲区**。然后，每当执行同一个 PreparedStatement 语句的时候，由于在缓冲区中可以发现预编译的命令，虽然会被再解析一次，但不会被再次编译。而 sql 注入只对编译过程有破坏作用，执行阶段只是把输入串作为数据处理（为字符串参数添加双引号），不需要再对 sql 语句进行解析，因此解决了注入问题。因为 sql 语句编译阶段是进行词法分析、语法分析、语义分析等过程的，也就是说编译过程识别了关键字、执行逻辑之类的东西，编译结束了这条 sql 语句能干什么就定了。而在编译之后加入注入的部分，就已经没办法改变执行逻辑了，这部分就只能是相当于输入字符串被处理。

3. 安全：Statement 会导致 sql 注入的问题。
---
参考：

- [mybatis 中 # 与 $ 的区别](https://blog.csdn.net/zymx14/article/details/78067452)
- [预编译为什么能防止 sql 注入-知乎](https://www.zhihu.com/question/43581628/answer/153847199
)