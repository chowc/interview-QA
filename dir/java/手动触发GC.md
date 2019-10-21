#### 手动触发 3 次 minor gc、2 次 full gc，再 2 次 minor gc

使用最简单的 Serial 收集器。

```java
private static final int One_MB = 1024*1024;
// -verbose:gc -Xmx40M -Xms40M -Xmn20M -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC
public static void main(String[] args) {
	byte[] obj1 = new byte[10*One_MB];
	obj1 = null;
	// eden 区是 16MB，所以 obj2 的创建会引发一次 minor gc，此次将 obj1 回收，然后 obj2 被分配在 eden 区
	byte[] obj2 = new byte[10*One_MB];
	obj2 = null;

	// eden 区是 16MB，所以 obj3 的创建会引发一次 minor gc，此次将 obj2 回收，然后 obj3 被分配在 eden 区
	byte[] obj3 = new byte[10*One_MB];
	// eden 区是 16MB，所以 obj4 的创建会引发一次 minor gc，obj3 晋升到老年代，obj4 被分配在 eden 区
	byte[] obj4 = new byte[10*One_MB];

	obj3 = null;
	// 此时 eden 区使用了 10MB，老年代使用了 10MB，创建 obj5 引发一次 full gc，老年代的 obj3 被回收，obj5 被分配到 eden 上，obj4 晋升到老年代
	byte[] obj5 = new byte[10*One_MB];

	obj4 = null;
	// 此时 eden 区使用了 10MB，老年代使用了 10MB，创建 obj6 引发一次 full gc，老年代的 obj4 被回收，obj6 被分配到 eden 上，obj5 晋升到老年代
	byte[] obj6 = new byte[10*One_MB];

	obj6 = null;
	// 引发一次 minor gc，回收 obj6
	obj4 = new byte[10*One_MB];

	obj4 = null;
	// 引发一次 minor gc，回收 obj4
	obj6 = new byte[10*One_MB];
}
```