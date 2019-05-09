# 算法题

---

### Java 语言

#### Java 基础

- 普通集合

1. 经常使用的集合类有哪些？
2. 讲述一下集合类的类继承关系？TreeSet 继承了哪些接口？TreeMap 继承了哪些接口？

![Collection 集合继承关系，哪些是接口，哪些是抽象类]()

![Map 继承关系，哪些是接口，哪些是抽象类]()

3. ArrayList 与 LinkedList 的实现和区别？

    1. ArrayList 基于动态数组，而 LinkedList 是基于双向链表；
    2. 各个操作的时间复杂度如下：

操作 |          ArrayList 时间复杂度 |　　LinkedList 时间复杂度
---|---|---
get(int index)|　O(1) | O(n) |
add(E element)| 在不需要进行扩容的时候是 O(1)，需要扩容的话是 O(n) | O(1) |
add(int index, E element)| O(n)，因为需要移动 index 之后的数据 | O(n)，因为需要定位到 index，如果 index=0 则为 O(1) |
remove(int index)| O(n)，因为需要将 index 之前的数据往前移动 | O(n)，因为需要定位到 index |
Iterator.remove()| O(n)，因为需要将 index 之前的数据往前移动 | O(1)，无需定位，直接修改指针即可|
ListIterator.add(E element)|O(n)，因为需要移动 index 之后的数据 | O(1)，无需定位，直接修改指针即可|