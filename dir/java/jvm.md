#### 内存分配

- 所有对象都是在堆上创建的吗?

#### 虚拟机分区

#### 类加载
- 了解双亲委派机制
- 双亲委派机制的作用？
- Tomcat 的 classloader 结构
- 如何自己实现一个 classloader 打破双亲委派

#### GC
- 垃圾回收基本原理

可达性分析：通过判断从 GC Root 到目标对象之间有没有一条引用链相连，来判断该对象可否被回收。

- 了解 Java 中的强引用、软引用、弱引用、虚引用的适用场景以及释放机制

> 强引用就是指在程序代码之中普遍存在的，类似 "Objectobj=newObject()" 这类的引用，只要强引用还存在，垃圾收集器永远不会回收掉被引用的对象。
>
> 软引用是用来描述一些还有用但并非必需的对象。*对于软引用关联着的对象，在系统将要发生内存溢出异常之前，将会把这些对象列进回收范围之中进行第二次回收。如果这次回收还没有足够的内存，才会抛出内存溢出异常*。在 JDK1.2 之后，提供了 SoftReference 类来实现软引用。
>
> 弱引用也是用来描述非必需对象的，但是它的强度比软引用更弱一些，*被弱引用关联的对象只能生存到下一次垃圾收集发生之前。当垃圾收集器工作时，无论当前内存是否足够，都会回收掉只被弱引用关联的对象*。在 JDK1.2 之后，提供了 WeakReference 类来实现弱引用。
>
> 虚引用也称为幽灵引用或者幻影引用，它是最弱的一种引用关系。一个对象是否有虚引用的存在，完全不会对其生存时间构成影响，也无法通过虚引用来取得一个对象实例。为一个对象设置虚引用关联的唯一目的就是能在这个对象被收集器回收时收到一个系统通知。在 JDK1.2 之后，提供了 PhantomReference 类来实现虚引用。
>
> 周志明. 深入理解Java虚拟机：JVM高级特性与最佳实践

- 几种常见的垃圾回收器的特性、重点了解 CMS （或 G1 ）以及一些重要的参数
- 什么情况下使用堆外内存？要注意些什么？
- 堆外内存如何被回收？DirectByteBuffer
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

-XX:+PrintGC包含-verbose:gc，-XX:+PrintGCDetails //包含-XX:+PrintGC；

只要设置-XX:+PrintGCDetails 就会自动带上-verbose:gc和-XX:+PrintGC

-XX:+PrintGCDateStamps/-XX:+PrintGCTimeStamps 输出gc的触发时间