- 一致性哈希

实现一致性哈希的几个关键地方主要如下：

1. 虚拟节点：需要将虚拟节点映射到各个真实节点上；
2. 虚拟节点的查找：需要根据给定的 Key 找到不小于它的第一个虚拟节点，这一点可以使用 TreeMap 的 `tailMap(K fromKey)` 方法。

```java
public class ConsistentCache {
    private TreeMap<Long, Node> circle;
    private int VIRTUAL_FACTOR = 100;

    public ConsistentCache(List<Machine> machines) {
        circle = new TreeMap<>();
        for (int i=0; i<machines.size(); i++) {
            for (int j = 0; j < VIRTUAL_FACTOR; j++) {
                // 均匀地分布虚拟节点
                circle.put(hash("Node"+j+", Machine-"+i), new Node(machines.get(i)));
            }
        }
    }

    /**
     * 查找给定 key 的节点
     * @param key
     * @return
     */
    public Node node(String key) {
        long hash = hash(key);
        SortedMap<Long, Node> tailMap = circle.tailMap(hash);
        if (tailMap.size() == 0) {
            //
            return circle.firstEntry().getValue();
        }
        return circle.get(tailMap.firstKey());
    }

    /**
     * hash 函数，将给定的 key 映射到一致性环上的一个整数
     * @param key
     * @return
     */
    public long hash(String key) {
        return key.hashCode();
    }
}

/**
 * 真实节点类
 */
class Machine {
    boolean put(String key, Object val) {
        // 加入缓存
        return true;
    }

    Object get(String key) {
        // 获取缓存中的数据
        return new Object();
    }
}

/**
 * 虚拟节点类
 */
class Node {
    private Machine machine;

    public Node(Machine machine) {
        this.machine = machine;
    }

    public boolean add(String key, Object val) {
        return machine.put(key, val);
    }

    public Object get(String key) {
        return machine.get(key);
    }
}
```