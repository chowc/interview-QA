#### 内存分配

- 所有对象都是在堆上创建的吗?


#### GC

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