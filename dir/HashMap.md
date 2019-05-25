#### ���� hash �Ĺ���

```
// �Ȼ�ȡ key �� hashCode���ٽ� h �ĸ� 16 λ��� 16 λ����������
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    // h=hashCode():     1111 1111 1111 1111 1111 0000 1110 1010 ���� hashCode()

    // h:                1111 1111 1111 1111 1111 0000 1110 1010
    // h>>>16:           0000 0000 0000 0000 1111 1111 1111 1111

    // hash=h^(h>>>16):  1111 1111 1111 1111 0000 1111 0001 0101


}
```
�� JDK 8 ��ʵ���У��Ż��˸�λ������㷨��ͨ�� hashCode() �ĸ�16λ����16λʵ�ֵģ�`(h = k.hashCode()) ^ (h >>> 16)`����Ҫ�Ǵ��ٶȡ���Ч�����������ǵģ���ô������������ table �� length �Ƚ�С��ʱ��Ҳ�ܱ�֤���ǵ��ߵ�Bit�����뵽Hash�ļ����У�ͬʱ������̫��Ŀ�����
#### ���� hash ��Ӧ�� bucket

```
// ����ȡ��
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // i=(n-1) & hash ���� (hash%n)
    // hash         :  1111 1111 1111 1111 0000 1111 0001 0101
    // n-1          :  0000 0000 0000 0000 0000 0000 0000 1111  n=16
    // (n-1)&hash   :  0000 0000 0000 0000 0000 0000 0000(0101)
    // 0101 ����ȡ����
    *if ((p = tab[i = (n - 1) & hash]) == null)*
        tab[i] = newNode(hash, key, value, null);
    ...
    
```

#### ��ȡ��һ����ӽ��� 2 �η���==��û����==��

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
// ���¼��� bucket
// newCap = oldCap<<1
// index = hash&(newCap-1)

```

#### rehash

1.8 �н������Ż���ֻ��Ҫ�ж� hash&oldCap ��0����1�Ϳ���֪���費��Ҫ�ƶ��ýڵ㡣

- [�ο�����](https://tech.meituan.com/2016/06/24/java-hashmap.html)