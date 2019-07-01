Java 基础

#### Integer

- 可以用 Integer 实例作为加锁对象吗？

不可以。因为 Integer 对象是可变的，每次对 Integer 对象进行加减操作都会创建一个新的对象，从而导致 num 的引用发生变化。另外，因为 Integer 本身存在缓存，也可能出现多个 num 是相同对象的情况。

```java
Integer num = 41;
synchronized(num) {
    ...
}
```
- `Integer.valueOf(int i)` 的实现？

当给定的 i 处在区间 [IntegerCache.low, IntegerCache.high] 上的时候，会返回缓存的 Integer 对象。对于不在此区间上的值，则新建一个对象返回。

```java
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
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

#### [集合](./集合.md)