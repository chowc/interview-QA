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
  // 关联的 sqlSession 对象
  private final SqlSession sqlSession;
  // 目标接口，即 Mapper 接口对应的 class 对象
  private final Class<T> mapperInterface;
  // 方法缓存，用于缓存 MapperMethod对象，key 为 Mapper 接口中对应方法的 Method 对象，value 则是对应的 MapperMethod，MapperMethod 会完成参数的转换和 SQL 的执行功能
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }
  
  // 代理对象执行的方法，代理以后，所有 Mapper 的方法调用时，都会调用这个invoke方法
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
     // 并不是每个方法都需要调用代理对象进行执行，如果这个方法是Object中通用的方法，则无需执行
     if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      // 如果是默认方法，则执行默认方法，Java 8 提供了默认方法
      } else if (isDefaultMethod(method)) {
        return invokeDefaultMethod(proxy, method, args);
      }
     // 从缓存中获取 MapperMethod 对象，如果缓存中没有，则创建一个，并添加到缓存中
     final MapperMethod mapperMethod = cachedMapperMethod(method);
     // 执行方法对应的 SQL 语句
     return mapperMethod.execute(sqlSession, args);
  }
  // 缓存 MapperMethod 
  private MapperMethod cachedMapperMethod(Method method) {
    MapperMethod mapperMethod = methodCache.get(method);
    if (mapperMethod == null) {
      mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
      methodCache.put(method, mapperMethod);
    }
    return mapperMethod;
  }
}
```
- mybatis 中接口中可以有方法重载吗？

xml 文件中如果存在 id 相同的两个元素，会抛出异常，因为相同 namespace+id 需要是唯一的。

语法上允许重载，但是因为 mybatis 中实现方法调用时是使用 “类全限定名+方法名” 作为 sql id，去 xml 中查找对应 id 的 MappedStatement，重载后就会导致两个方法映射到了同一个 sql 上，因此单纯的重载是不行的。

在 Java 8 中，可以通过接口的 default 方法实现方法重载，对于 mapper 接口中的默认方法，mybatis 会直接执行它而不是调用代理对象（MapperProxy 的 invoke 逻辑）。
```java
// 1.8
// mapper 接口代码
default User findUser(Integer id) {
  return findUser(id, null);
}
User findUser(Integer id, String name);
```
```xml
// 对应的 xml
<select id="findUser" resultType="User">
  select from users where id = #{id}
  <if test="name != null">
    and name = #{name}
  </if>
</select>
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
2. \# 和 $ 在预编译处理中是不一样的。# 类似 jdbc 中的 PreparedStatement，对于传入的参数，在预处理阶段会使用 ? 代替，比如：
`select * from student where id = ?;`，待真正查询的时候即在数据库管理系统中（DBMS）才会代入参数。
而 ${} 则是简单的替换，如：`select * from student where id = 2;`

*占位符只能占位 sql 语句中的普通值，决不能占位表名、列名、sql 关键字（select、insert等）；因为 PreparedStatement 的 sql 语句是要预编译的，如果关键字、列名、表名等被占位那就直接代表该 sql 语句语法错误而无法编译，会直接抛出异常，因此只有不影响编译的部分可用占位符占位*。

因此，能使用 #{} 的地方应尽量使用 #{}；#{} 可以有效防止 sql 注入，${} 则可能导致 sql 注入成功。

- association、colletion 标签的区别

两者都是用于实现 SELECT 中表关联时的字段映射，association 是一对一关联关系，而 collection 则是一对多关联。

一对一的情况：
```java
// User 类中有一个 Card 的属性
public class User {
    private Integer userId;
    private String userName;
    private Integer age;
    private Card card;//一个人一张身份证,1对1
}

public class Card {
    private Integer cardId;
    private String cardNum;//身份证号
    private String address;//地址
}
```

```xml
<resultMap type="Card" id="cardMap">
      <id property="cardId" column="card_id"/>
      <result property="cardNum" column="card_num"/>
      <result property="address" column="address"/>
</resultMap>

<resultMap type="User" id="userMap">
     <result property="userName" column="user_name"/>
     <result property="age" column="age"/>
     <association property="card" resultMap="cardMap"/>
</resultMap>

// User 的查询被映射到 userMap
<select id="queryById" parameterType="int" resultMap="userMap">
    SELECT u.user_name,u.age,c.card_id,c.card_num,c.address
    FROM tb_user u,tb_card c
    WHERE u.card_id=c.card_id
    AND
    u.user_id=#{userId}
</select>
```

一对多的情况：
```java
public class User{
    private Integer userId;
    private String userName;
    private Integer age;
    private List<MobilePhone> mobilePhone;//土豪,多个手机,1对多
}
```

```xml
<resultMap type="MobilePhone" id="mobilephoneMap">
         <id column="mobile_phone_id" property="mobilePhoneId"/>
         <result column="brand" property="brand" />
         <result column="price" property="price" />
</resultMap>

<resultMap type="User" id="userMap">
        <result property="userName" column="user_name"/>
        <result property="age" column="age"/>
        <collection property="mobilePhone" resultMap="mobilephoneMap"/>
</resultMap>

<select id="queryById" parameterType="int" resultMap="userMap">
    SELECT u.user_name,u.age,m.brand,m.price
    FROM tb_user u,tb_mobile_phone m
    WHERE m.user_id=u.user_id
    AND
    u.user_id=#{userId}
</select>
```


- 防止 sql 注入的几种方式

1. 在页面输入参数时也进行字符串检测和提交时进行参数检查，同样可以使用正则表达式，不允许特殊符号出现；
2. 在程序代码中使用正则表达式过滤参数。使用正则表达式过滤可能造成注入的符号，如单引号等；
3. jdbc 使用 PreparedStatement 代替 Statement，PreparedStatement  不仅提高了代码的可读性和可维护性.而且也提高了安全性，有效防止 sql 注入。

- mybatis 动态 sql 是做什么的？都有哪些动态 sql？简述一下动态 sql 的执行原理？

1. mybatis 动态 sql 可以让我们在 xml 映射文件内，以标签的形式编写动态 sql，完成逻辑判断和动态拼接 sql 的功能；
2. mybatis 提供了 9 种动态 sql 标签：`trim|where|set|foreach|if|choose|when|otherwise|bind`；
3. 其执行原理为，使用 OGNL 从 sql 参数对象中计算表达式的值，根据表达式的值动态拼接 sql，以此来完成动态 sql 的功能。

- 当实体类中的属性名和表中的字段名不一样，怎么办？
1. 通过 sql 别名，列名不区分大小写，mybatis 会忽略列名大小写：
```xml
<select id=”selectorder” parametertype=”int” resultetype=”me.gacl.domain.order”> 
   select order_id id, order_no orderno ,order_price price form orders where order_id=#{id}; 
</select> 
```
2. 通过 `resultMap` 进行字段映射：
```xml
<select id="getOrder" parameterType="int" resultMap="orderresultmap">
    select * from orders where order_id=#{id}
</select>
<resultMap type=”me.gacl.domain.order” id=”orderresultmap”> 
    <!– order_id 列映射到 id 字段–> 
    <id property=”id” column=”order_id”> 
    <result property = “orderno” column =”order_no”/> 
    <result property=”price” column=”order_price” /> 
</reslutMap>
```
- 如何获取自动生成的(主)键值？

通过 `selectKey` 标签。

1. 自增主键的获取
```xml
<insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
    <!--selectKey  会将 SELECT LAST_INSERT_ID() 的结果放入到传入的 model 的主键，
        keyProperty 对应 model 的主键属性名，这里是 user 中的 id，因为它跟数据库的主键对应。
        order=AFTER 表示 SELECT LAST_INSERT_ID() 在 insert 执行之后执行,多用与自增主键，
              BEFORE 表示 SELECT LAST_INSERT_ID() 在 insert 执行之前执行，这样的话就拿不到主键了，适合那种主键不是自增的类型。
        resultType 主键类型 -->
    <selectKey keyProperty="id" order="AFTER" resultType="java.lang.Integer">
        SELECT LAST_INSERT_ID()
    </selectKey>
    insert into t_user (username,password,create_date) values(#{username},#{password},#{createDate})
</insert>
```
```java
// 这里返回的 int 是被修改的数据行数，生成的主键为 user.id。
int insertAndGeyKey(User user)
```
2. 非自增主键的获取
```xml
<insert id="insert" parameterType="com.soft.mybatis.model.Customer">
    <!-- 跟自增主键方式相比，这里的不同之处只有两点
                1. insert 语句需要写 id 字段了，并且 values 里面也不能省略；
                2. selectKey 的 order 属性需要写成 BEFORE 因为这样才能将 uuid 生成的主键放入到 model 中，后面的 insert 的 values 里面的 id 才不会获取为空。
    -->
    <selectKey keyProperty="id" order="BEFORE" resultType="String">
        select uuid()
    </selectKey>
    insert into t_customer (id,c_name,c_sex,c_ceroNo,c_ceroType,c_age) values (#{id},#{name},#{sex},#{ceroNo},#{ceroType},#{age})
</insert>
```
- 在 mapper 中如何传递多个参数？
1. #{0}，#{1} 方式
```xml
// 对应的xml,#{0}代表接收的是dao层中的第一个参数，#{1}代表dao层中第二参数，更多参数一致往后加即可。
<select id="selectUser"resultMap="BaseResultMap">  
    select *  fromuser_user_t   whereuser_name = #{0} anduser_area=#{1}  
</select>
```
2. `@param` 注解方式
```java
public interface Usermapper { 
    User selectUser(@param("username") String username, @param("hashedpassword") String hashedpassword); 
}
```
```xml
<select id=”selectUser” resultType=”User”> 
     select id, username, hashedpassword 
     from some_table 
     where username = #{username} 
     and hashedpassword = #{hashedpassword} 
</select>
```
3. 传入 map 集合作为参数

- mybatis 是如何分页的？

- 
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
- [常见的 mybatis 面试题](https://my.oschina.net/u/3777556/blog/1633503)
- [常见的 mybatis 面试题](https://zhuanlan.zhihu.com/p/44412964)
- [insert 获取生成的主键](https://blog.csdn.net/xu1916659422/article/details/77921912)