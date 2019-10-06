### Java 基础

#### 类和对象

- 接口和抽象类的区别

1. 接口和抽象类都不能被实例化；
2. 抽象类可以提供非抽象的方法，而接口中的成员方法都是 `public abstract`（**1.8 后接口可以提供 default 方法实现非抽象的默认方法**，默认方法不能够重写 Object 中的方法，却可以重载 Object 中的方法。）；
3. 抽象类中的成员变量可以是各种类型的，而接口中的变量只能是 `public static final` 类型的；
4. 接口中不能含有静态代码块（**1.8 开始可以实现静态方法**），而抽象类可以有静态代码块和静态方法；
5. 一个类只能继承一个抽象类，而一个类却可以实现多个接口；
6. 抽象类是对一种事物的抽象，即对类抽象，而接口是对行为的抽象。

#### Object

- Object 类中的方法有哪些？

1. `Object()`：构造方法:

2. 线程相关：`wait()`、`wait(long timeout)`、`wait(long timeout, int nanos)`、`notify()`、`notifyAll()`，这些方法都只能被持有该对象锁的线程调用，**都是 final 方法，不允许被覆盖**；

3. `equals(Object obj)`：用于比较给定的对象是否与 this 相等；

4. `hashCode()`：返回一个整型数，用于表示该对象的哈希码，该方法主要用于哈希集合中。该方法遵循以下规范：

    1. 如果两个对象相等（equals() 方法返回 true），那么这两个对象调用 hashCode() 返回的哈希码也必须相等；
    2. 反之，两个对象调用 hasCode() 返回的哈希码相等，这两个对象不一定相等。

5. `toString()`：获取该对象的字符串表示形式，默认由该对象的类名以及哈希码的 16 进制表示。

```java
public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
}
```

6. `getClass()`：返回该对象的类对象，**final 方法，不允许被覆盖**；

7. `clone()`：native 方法，用于返回一个该对象的拷贝。**覆盖此方法的同时还需要实现 Cloneable 接口**，否则在调用时会抛出 CloneNotSupportedException。

8. `finalize()`：与 GC 有关。当一个对象被 GC 线程判定为可回收时，会将该对象放到一个队列中，然后依次去执行队列里对象的 finalize 方法。

9. `registerNatives()`：静态方法，用于注册本地方法。

#### Integer

- 可以用 Integer 实例作为加锁对象吗？

不可以。因为 Integer 对象是可变的，每次对 Integer 对象进行加减操作都会创建一个新的对象，从而导致 num 的引用发生变化。另外，因为 Integer 本身存在缓存，也可能出现多个线程错误地锁了相同对象的情况。

```java
Integer num = 41;
synchronized(num) {
    ...
    // 通过查看编译后的字节码可以发现实际上是通过 num = Integer.valueOf(42); 实现的
    num += 1;
}
```
- `Integer.valueOf(int i)` 的实现？

当给定的 i 处在区间 [IntegerCache.low, IntegerCache.high] 上的时候，会返回缓存的 Integer 对象。对于不在此区间上的值，则新建一个对象返回。

```java
class IntegerCache {
    static final int low = -128;
    # high 默认是 127，可以通过系统属性 "java.lang.Integer.IntegerCache.high" 进行配置
    static final int high;
    static final Integer cache[];

    static {
        // high value may be configured by property
        int h = 127;
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
            } catch( NumberFormatException nfe) {
                // If the property cannot be parsed into an int, ignore it.
            }
        }
        high = h;

        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);

        // range [-128, 127] must be interned (JLS7 5.1.7)
        assert IntegerCache.high >= 127;
    }    
}

public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

