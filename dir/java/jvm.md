#### 内存分配

- 所有对象都是在堆上创建的吗?

不是的，随着 JIT 编译器的发展，在编译期间，如果 JIT 经过逃逸分析，发现有些对象没有逃逸出方法，那么有可能堆内存分配会被优化成栈内存分配。

#### 虚拟机分区

![image](../img/jvm_allocation.png)

虚拟机的内存可分为：程序计数器、虚拟机栈、本地栈、堆、方法区，

各个区的作用如下：

1. 程序计数器：当前线程执行的字节码的行号指示器；
2. 虚拟机栈：**它的生命周期与线程相同，虚拟机栈描述的是 Java 方法执行的内存模型**。每个方法在执行的同时都会创建一个栈帧用于存储局部变量表（方法参数和方法内部定义的局部变量）、操作数栈、动态链接、方法返回地址等信息。**每一个方法从调用直至执行完成的过程，就对应着一个栈帧在虚拟机栈中入栈到出栈的过程**；
3. 本地栈：即对本地方法调用的方法栈；
4. 堆：是虚拟机中最大的一块内存，用于存放对象实例，虚拟机内的对象都在这块区域上进行分配，也是垃圾回收的主要关注区域；
5. 方法区：用于存储已被虚拟机加载的类信息、常量、静态变量、JIT 编译后的代码等。

其中，程序计数器、虚拟机栈、本地栈是线程私有的；堆、方法区是线程共享的。

- 方法栈中的引用如何定位到堆中的对象？

1. 使用句柄访问

![image](../img/reference_handler.png)

2. 使用指针定位

![image](../img/reference_direct.png)

优缺点：

1. 使用句柄来访问的最大好处就是 reference 中存储的是稳定的句柄地址，在对象被移动（垃圾收集时移动对象是非常普遍的行为）时只会改变句柄中的实例数据指针，而 reference 本身不需要修改。

2. 使用直接指针访问方式的最大好处就是速度更快，它节省了一次指针定位的时间开销，由于对象的访问在 Java 中非常频繁，因此这类开销积少成多后也是一项非常可观的执行成本。

- Hotspot 虚拟机在 Java 7 和 Java 8 做了哪些改动？为什么做这些变动？

在 Java 7 中将方法区的字符串常量池移动到了堆内；在 Java 8 中移除了原有的永久代，改用 MetaSpace 替代，並使用了本地内存。

移除的原因如下：

> This is part of the JRockit and Hotspot convergence effort. JRockit customers do not need to configure the permanent generation (since JRockit does not have a permanent generation) and are accustomed to not configuring the permanent generation.
>
> http://openjdk.java.net/jeps/122

也就是为了 Hotspot 与 JRockit 的融合，因为 JRockit 是没有使用永久代的；另外，持久代大小受到 `-XX：MaxPermSize` 和 JVM 设定的内存大小限制，这就导致在使用中可能会出现持久代内存溢出的问题，因此在 `Java 8` 及之后的版本中彻底移除了持久代而使用 `Metaspace` 来进行替代。

- MetaSpace 的说明

#### [类加载](类加载.md)

#### GC
- 垃圾回收基本原理

可达性分析：通过判断从 GC Root 到目标对象之间有没有一条引用链相连，来判断该对象可否被回收。其中可以做为 GC Roots 的对象有：

1. 虚拟机栈（栈帧中的局部变量表）中引用的对象；
2. 方法区中引用类型静态变量；
3. 方法区中引用类型常量；
4. 本地方法栈中 JNI（即一般说的 Native 方法）引用的对象。

- 了解 Java 中的强引用、软引用、弱引用、虚引用的适用场景以及释放机制

> 强引用就是指在程序代码之中普遍存在的，类似 "Object obj = new Object()" 这类的引用，只要强引用还存在，垃圾收集器永远不会回收掉被引用的对象。
>
> 软引用是用来描述一些还有用但并非必需的对象。*对于软引用关联着的对象，在系统将要发生内存溢出异常之前，将会把这些对象列进回收范围之中进行第二次回收。如果这次回收还没有足够的内存，才会抛出内存溢出异常*。在 JDK1.2 之后，提供了 SoftReference 类来实现软引用。
>
> 弱引用也是用来描述非必需对象的，但是它的强度比软引用更弱一些，*被弱引用关联的对象只能生存到下一次垃圾收集发生之前。当垃圾收集器工作时，无论当前内存是否足够，都会回收掉只被弱引用关联的对象*。在 JDK1.2 之后，提供了 WeakReference 类来实现弱引用。还有 WeakHashMap ，其 key 是一个 WeakReference，当内存中的引用只剩下这个 WeakReference 的 key 的时候，虚拟机就能够自动将其对应的内存进行回收。（**补充 WeakHashMap 中 value 回收的过程**）
>
> 虚引用也称为幽灵引用或者幻影引用，它是最弱的一种引用关系。一个对象是否有虚引用的存在，完全不会对其生存时间构成影响，也无法通过虚引用来取得一个对象实例。为一个对象设置虚引用关联的唯一目的就是能在这个对象被收集器回收时收到一个系统通知。在 JDK1.2 之后，提供了 PhantomReference 类来实现虚引用。
>
> 周志明. 深入理解Java虚拟机：JVM高级特性与最佳实践

- `System.gc()` 方法执行的是哪一类 gc？

在 hotspot 上触发的是 full gc，可以通过参数 `-XX:+DisableExplicitGC` 来禁用它。

- Reference 类型及其状态转换

![image](../img/reference_class_hierarchy.png)

其中，Finalizer 对应强引用，SoftReference 对应软引用，WeakReference 对应弱引用，PhantomReference 对应虚引用。

在实例化一个 `java.lang.ref.Reference` 时可以指定一个 ReferenceQueue，该 ReferenceQueue 会影响后续 Reference 对象的回收过程。

Reference 的状态转换：

![image](../img/reference_state.png)

- 几种垃圾回收算法

1. 标记清除（Mark-Sweep）：先标记然后直接清除掉被标记的内存区域，会造成内存碎片；
2. 复制算法（Copying）：需要划分为两个区域，然后将一个区域的存活对象复制到另一个区域，使得其中总是有一块区域不能够被使用，造成浪费且在存活对象较多的情况下需要做大量复制操作，效率降低；
3. 标记整理（Mark-Compact）：在标记完之后不是直接进行清除，而是让所有存活的对象都向一端移动，然后直接清理掉端边界意外的内存。

存活对象比例低的新生代适合复制算法，而存活比例高的老年代则适合标记清除或标记整理算法。

- 几种常见的垃圾回收器的特性、重点了解 CMS（或 G1）以及一些重要的参数

1. 按照单/多线程划分：

单线程：Serial、Serial Old

多线程：ParNew、Parallel Scavenge、Parallel Old、CMS、G1

2. 按照垃圾回收区域划分

新生代：Serial、ParNew、Parallel Scavenge

老年代：Serial Old、Parallel Old、CMS

其中 G1 收集器没有使用新生代/老年代的概念，而是对整个堆内存进行回收。

3. 按照采用的垃圾回收算法划分

标记清除（Mark-Sweep）：CMS、

复制（Copying）：Serial、ParNew、Parallel Scavenge、

标记整理（Mark-Compact）：Serial Old、Parallel Old

其中，G1 从整体来看是基于“标记-整理”算法实现的收集器，从局部（两个 Region 之间）上看是基于“复制”算法的。

CMS（Concurrent Mark Sweep）

针对老年代的并发的标记清除算法。


G1

- CMS GC 回收分为哪几个阶段？分别做了什么事情？

1. 初始标记

初始标记仅仅只是标记一下 GCRoots 能直接关联到的对象，速度很快，**需要停止其他正在运行的线程**。

2. 并发标记

并发标记阶段就是进行 GCRoots Tracing 的过程，可以与其他用户线程一起并发。

3. 重新标记

重新标记阶段则是为了修正并发标记期间因用户程序继续运作而导致标记产生变动的那一部分对象的标记记录，这个阶段的停顿时间一般会比初始标记阶段稍长一些，但远比并发标记的时间短，**需要停止其他正在运行的线程**。

4. 并发清除

- CMS 有哪些重要参数？

1. 设置使用 CMS 收集器：`-XX：+UseConcMarkSweepGC`，默认 HotSpot JVM 使用的是并行收集器；
2. 设置预留给用户线程执行所需的内存空间，预留的空间大小可以通过参数 `-XX:CMSInitiatingOccupancyFraction=N(0-100)` 设置；
3. 使用增量回收模式：`-XX:+CMSIncrementalMode`，在并发标记、清理的时候让 GC 线程、用户线程交替执行，尽量减少 GC 线程独占资源的时间，同时也会使得 GC 的时间变长；
4. 设置开启合并整理过程：`-XX:+UseCMSCompactAtFullCollection`，用于开启内存整理，默认是开启的，但会使得停顿时间变长，因为整理过程是不能并发的；
5. `-XX:CMSFullGCsBeforeCompaction=N`，设置进行多少次不整理的回收后就会进行一次带整理的回收，默认为 0，即每次回收都会进行内存整理。

- Concurrent Model Failure 和 ParNew promotion failed 什么情况下会发生？

1. Concurrent Mode Failure 的出现场景

CMS 的回收线程因为是与用户线程并发执行的，所以需要预留足够的内存空间给用户线程执行所需，CMS 会在老年代使用了预留空间大小的内存后被激活，可以通过参数 `-XX:CMSInitiatingOccupancyFraction` 来设置，在 JDK 1.5 中预留空间为 68%，也就是老年代空间使用了 68% 之后会触发 CMS 进行回收。在 1.6 中提高到 92%。要是 CMS 运行期间预留的内存无法满足程序需要，就会出现一次 "Concurrent Mode Failure" 失败，这时虚拟机将启动后备预案：临时启用 SerialOld 收集器来重新进行老年代的垃圾收集，这样停顿时间就很长了。

2. ParNew promotion failed 的出现场景

在进行 minor gc 时，因为 survivor 区域不足以放下所有存活的对象，因此需要将一部分对象放入老年代中，而如果这时候老年代的空间也不足的话，就会出现 “ParNew promotion failed”。

- CMS 的优缺点？

优点：并发收集，低停顿。

缺点：

1. 不能处理浮动垃圾，也就是在并发清除过程中用户线程产生的垃圾，因为这部分垃圾并没有被标记到，所以无法进行回收；
2. 在并发阶段，它虽然不会导致用户线程停顿，但是会因为占用了一部分线程（或者说CPU资源）而导致应用程序变慢，总吞吐量会降低。CMS默认启动的回收线程数是（CPU数量+3）/4，也就是当CPU在4个以上时，并发回收时垃圾收集线程不少于25%的CPU资源，并且随着CPU数量的增加而下降；
3. CMS 使用的是标记清除算法，在垃圾收集完成后会产生大量空间碎片；
4. 在堆空间比较大的情况下，标记垃圾的时间会变长，使得停顿时间变久。

- 有做过哪些 GC 调优？
- 为什么要划分成年轻代和老年代？

> 一般是把 Java 堆分为新生代和老年代，这样就可以根据各个年代的特点采用最适当的收集算法。在新生代中，每次垃圾收集时都发现有大批对象死去，只有少量存活，那就选用复制算法，只需要付出少量存活对象的复制成本就可以完成收集。而老年代中因为对象存活率高、没有额外空间对它进行分配担保，就必须使用“标记—清除”或者“标记—整理”算法来进行回收。
>
> 周志明. 深入理解Java虚拟机：JVM高级特性与最佳实践（第2版）

- 什么情况下使用堆外内存？要注意些什么？使用堆外空间的好处是什么？

NIO、Java 8 中的 MetaSpace。用参数 `-XX:MaxDirectMemorySize` 来指定可用堆外内存的最大值。	

好处如下：

1. 减少了垃圾回收的工作，因为不是在堆上分配的内存；
2. 避免了系统调用时要将堆上内存拷贝到本地内存的过程，提升效率；
3. 可以在进程间共享，减少JVM间的对象复制，使得JVM的分割部署更容易实现；
4、可以扩展至更大的内存空间。比如超过1TB甚至比主存还大的空间。

坏处如下：

1. 堆外内存难以控制，如果内存泄漏，那么很难排查；
2. 堆外内存相对来说，不适合存储很复杂的对象。一般简单的对象或者扁平化的比较适合。

- 堆外内存如何被回收？DirectByteBuffer 是如何回收堆外内存的？

1. PhantomReference 的回收过程

PhantomReference 在被 GC 线程判定为不可达对象之后，会将其状态从 Active 改为 Pending（当 PhantomReference 实例化时指定了 ReferenceQueue 的情况下），並且 Reference 类在类初始化时会启动一个线程来负责对 Pending 状态的对象进行处理，该线程就是负责不停地执行一个 `tryHandlePending` 方法，它会判定当前 Pending 状态的对象是不是 Cleaner 类的实例，如果是的话，就执行 Cleaner 的 clean 方法，而 `Cleaner.clean` 方法的作用就是运行 Cleaner 构造方法传入的 Runnable 任务。

2. DirectByteBuffer 实例化的参数说明，Cleaner 的构造参数说明

继承了 PhantomReference 的 Cleaner 在被 GC 的时候会执行其 clean 方法;

在实例化一个 DirectByteBuffer 对象的时候会通过 `unsafe.allocateMemory(size);` 申请本地内存，同时会通过调用 `Cleaner.create(this, new Deallocator(base, size, cap))` 来初始化一个 Cleaner 对象。Cleaner 继承自 PhantomReference，其中参数 this 是当前的 DirectByteBuffer 实例，Deallocator 则是实现了 Runnable 接口的一个 Task。

当 DirectByteBuffer 被回收的时候，其引用的 Cleaner 也会被回收。因为 Cleaner 是一个 PhantomReference，所以要遵循 PhantomReference 的回收规则。

根据“1. PhantomReference 的回收过程”可以知道 tryHandlePending 方法最终会执行到 Deallocator.run 方法，而 Deallocator.run 的作用就是调用 `unsafe.freeMemory(address);`  来释放申请的本地内存。

另外，除了通过 GC 回收，DirectByteBuffer 在实例化的时候还会调用 `Bits.reserveMemory(size, cap);` 来尝试回收堆外空间，而该方法最终调用的也是 Cleaner.clean 方法。（补充一个完整流程）

- oom了怎么办

通过添加启动参数让JVM在遇到OOM(OutOfMemoryError)时生成Dump文件

`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/to/file`

- 如何查看 GC 日志

通过设置VM参数 `XX:+PrintGCDetails` 就可以打印出 GC 日志。

参数| 意义
---|---
-verbose:gc  | 开启 GC 日志打印
-XX:+PrintGCDetails  | 打印详细的 GC 日志
-XX:+PrintGCDateStamps | 打印出 GC 时间
-Xloggc:/path/to/gc.log| GC 日志路径
-XX:+UseGCLogFileRotation |  启用GC日志文件的自动转储 (Since Java)
-XX:NumberOfGClogFiles=1 |  GC日志文件的循环数目 (Since Java)
-XX:GCLogFileSize=1M |  控制GC日志文件的大小 (Since Java)

-XX:+PrintGC 包含-verbose:gc，-XX:+PrintGCDetails //包含-XX:+PrintGC；

只要设置 -XX:+PrintGCDetails 就会自动带上 -verbose:gc 和 -XX:+PrintGC

-XX:+PrintGCDateStamps/-XX:+PrintGCTimeStamps 输出gc的触发时间