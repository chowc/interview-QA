## 树

### 二叉查找树

二叉查找树，也称有序二叉树（ordered binary tree），或已排序二叉树（sorted binary tree），是指一棵空树或者具有下列性质的二叉树：

1. 若任意节点的左子树不空，则左子树上所有结点的值均小于它的根结点的值；
2. 若任意节点的右子树不空，则右子树上所有结点的值均大于它的根结点的值；
3. 任意节点的左、右子树也分别为二叉查找树。
4. 没有键值相等的节点（no duplicate nodes）。

因为一棵由n个结点随机构造的二叉查找树的高度为lgn，所以顺理成章，二叉查找树的一般操作的执行时间为O(lgn)。但二叉查找树若退化成了一棵具有n个结点的线性链后，则这些操作最坏情况运行时间为O(n)。

### 红黑树

红黑树，一种二叉查找树，但在每个结点上增加一个存储位表示结点的颜色，可以是Red或Black。
通过对任何一条从根到叶子的路径上各个结点着色方式的限制，红黑树确保没有一条路径会比其他路径长出俩倍，因而是接近平衡的。

红黑树虽然本质上是一棵二叉查找树，但它在二叉查找树的基础上增加了着色和相关的性质使得红黑树相对平衡，从而保证了红黑树的查找、插入、删除的时间复杂度最坏为 O(lgN)。

红黑树相比 AVL，虽然平衡性较差，但是维护树的平衡成本较低（需要的操作较少）。

### AVL：自平衡的二叉查找树

1. 如果插入一个node引起了树的不平衡，AVL和红黑树(红黑树)都是最多只需要2次旋转操作，即两者都是 O(1)；但是在删除node引起树的不平衡时，最坏情况下，AVL需要维护从被删node到root这条路径上所有node的平衡性，因此需要旋转的量级 O(logN)，而红黑树最多只需3次(因为不需要严格的平衡，从根到叶子的最长的可能路径不多于最短的可能路径的两倍长)旋转以及修改节点的颜色，只需要 O(1) 的复杂度；
2. 其次，AVL的结构相较红黑树来说更为平衡，在插入和删除node更容易引起Tree的unbalance，因此在大量数据需要插入或者删除时，AVL需要rebalance的频率会更高。因此，红黑树在需要大量插入和删除node的场景下，效率更高。自然，由于AVL高度平衡，因此AVL的search效率更高。

### B 树：平衡多路查找树

- Concurrenthashmap为什么要用红黑树？为何不用其他的树，平衡二叉树，b+?

因为平衡二叉是高度平衡的树, 而每一次对树的修改, 都要 rebalance, 这里的开销会比红黑树大. 如果插入一个node引起了树的不平衡，平衡二叉树和红黑树都是最多只需要2次旋转操作，即两者都是O(1)；但是在删除node引起树的不平衡时，最坏情况下，平衡二叉树需要维护从被删node到root这条路径上所有node的平衡性，因此需要旋转的量级O(logN)，而红黑树最多只需3次旋转，只需要O(1)的复杂度, 所以平衡二叉树需要rebalance的频率会更高，因此红黑树在大量插入和删除的场景下效率更高。

### 环形数组：定时器

Netty 的 HashedWheelTimer。

使用一个环形数组来存储定时触发的任务，有一个线程每隔一段时间将指针向前移动一个数组下标，将当前指向下标内的任务取出，若任务 count 为 0，则触发它；若不为 0，将 count 减一，继续等待。

增加任务的时候，需要根据任务的触发时间、队列长度、间隔时长来确定任务的存放下标和 count，count 表示任务需要被循环 count 次后才能执行，是为了避免维护一个过大的数组。

另外，当数组过大时，还可以使用分级定时器，例如：以小时为单位的定时器+以分钟为单位的定时器+以秒为单位的定时器。
---
参考资料：

- https://blog.csdn.net/v_JULY_v/article/details/6105630