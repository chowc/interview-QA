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