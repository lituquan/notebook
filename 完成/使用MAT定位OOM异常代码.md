### 背景
最近碰到系统触发OOM了，感到既兴奋又惶恐。

### 开始跟踪
服务有开启触发OOM的时候生成JVM堆快照，参数：HeapDumpOnOutOfMemoryError，HeapDumpPath

叫运维同学把heap dump文件压缩，再下载下来。

这里之所以要压缩是因为dump文件很大，且堡垒机下载比较慢。原文件1.1G，压缩后260M。

#### 安装MAT
MAT([memory analyzer tool](https://www.eclipse.org/mat/downloads.php))之前就安装过，安装要注意本地jdk8的不要下载太高的版本，高版本是jdk11的。

打开MAT，左上角File-->Open Heap Dump-->选择文件，这里最好单独建一个文件夹，因为加载之后会生成一堆相关文件：

```
.
├── java_pid10.a2s.index
├── java_pid10.domIn.index
├── java_pid10.domOut.index
├── java_pid10.hprof
├── java_pid10.i2sv2.index
├── java_pid10.idx.index
├── java_pid10.inbound.index
├── java_pid10.index
├── java_pid10_Leak_Suspects.zip
├── java_pid10.o2c.index
├── java_pid10.o2hprof.index
├── java_pid10.o2ret.index
├── java_pid10.outbound.index
├── java_pid10_Thread_Details.zip
├── java_pid10.threads
└── java_pid10_Top_Components.zip
```
#### 怎么定位
![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7c2c9716fbcb4507b051380f2892a8ef~tplv-k3u1fbpfcp-watermark.image?)

加载完后，这里看饼图，明显有大内存占用。点击Leak Suspects

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/96ffcf92a52444b1affef4e597a06ee9~tplv-k3u1fbpfcp-watermark.image?)

看到Problem Suspect 1 和Problem Suspect 2， 一般的内存泄露这时已经可以看到具体类了，但是这里只看到线程、Spring相关，异常是在框架里抛出，还看不到具体代码。这里我的思路是找线程栈：

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3a21c9090c9040e99ae2b6c2ee85572c~tplv-k3u1fbpfcp-watermark.image?)

鼠标放到线程，右键打开Thread Detail，可以看到很深的栈调用，用com.xxx(我的代码开头)搜索，定位成功。经过分析，发现接口是批量查询，但是没有限制查询的数量且返回全部字段，查库加载了大量对象。

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b232bedd75ef425fbaeda8cea53ef5eb~tplv-k3u1fbpfcp-watermark.image?)

#### 一个小坑

一开始没注意，MAT最大内存只给了:-Xmx512m，直接就报错OOM了。改成：-Xmx1024m，然后加载正常。

### 总结

- 运行环境加上Heap Dump参数：-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=文件路径
- MAT分析，先看Leak Suspects，如果可以直接看到和业务有关异常类，那就是非常顺利的。看不出问题的话，试下看相关线程栈。
- 批量查询最好限制下数量，或者改成分页。

有其他分析方法的，欢迎讨论~





