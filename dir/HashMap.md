#### 计算 hash 的过程

```
// 先获取 key 的 hashCode，再将 h 的高 16 位与低 16 位进行异或操作
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    // h=hashCode():     1111 1111 1111 1111 1111 0000 1110 1010 调用 hashCode()

    // h:                1111 1111 1111 1111 1111 0000 1110 1010
    // h>>>16:           0000 0000 0000 0000 1111 1111 1111 1111

    // hash=h^(h>>>16):  1111 1111 1111 1111 0000 1111 0001 0101


}
```
在 JDK 8 的实现中，优化了高位运算的算法，通过 hashCode() 的高16位异或低16位实现的：`(h = k.hashCode()) ^ (h >>> 16)`，主要是从速度、功效、质量来考虑的，这么做可以在数组 table 的 length 比较小的时候，也能保证考虑到高低Bit都参与到Hash的计算中，同时不会有太大的开销。
#### 计算 hash 对应的 bucket

```
// 进行取余
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // i=(n-1) & hash 即是 (hash%n)
    // hash         :  1111 1111 1111 1111 0000 1111 0001 0101
    // n-1          :  0000 0000 0000 0000 0000 0000 0000 1111  n=16
    // (n-1)&hash   :  0000 0000 0000 0000 0000 0000 0000(0101)
    // 0101 即是取余结果
    *if ((p = tab[i = (n - 1) & hash]) == null)*
        tab[i] = newNode(hash, key, value, null);
    ...
    
```

#### 获取下一个最接近的 2 次方（==还没看懂==）

```
/**
 * Returns a power of two size for the given target capacity.
 */
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

#### resize

```
// 重新计算 bucket
// newCap = oldCap<<1
// index = hash&(newCap-1)

```

#### rehash

1.8 中进行了优化，只需要判断 hash&oldCap 是0还是1就可以知道需不需要移动该节点。

- [参考文章](https://tech.meituan.com/2016/06/24/java-hashmap.html)