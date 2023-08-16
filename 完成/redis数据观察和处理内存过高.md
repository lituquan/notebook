## 背景
今天收到一个告警
```
[阿里云监控]
告警状态：OK
实例名称：alihn1-prd-ark-redis02
实例ID：['instanceId=r-wz9ute09osm4ffbi7c']
监控项：memory_usage_rate
监控项说明：内存使用率
表达式：79.98%>=80%
当前阈值：79.98%
告警时间：2023-07-05 07:51:03
告警环境:prd
```
内存大？之前没处理过redis的告警，有几个猜想：
* 存在大的key
* 存在某一类突然出现的key
* 存在永不过期的key

## 看监控
阿里云上的监控显示，内存是线性增长的，也没有突然增高的现象。应该不是突然出现的、或者大key。
看了几个db的key数值，有个db的key数值有450w，比其他的都多。再继续看，看到这些key和内部一个数据服务有关系。key的组成如下：
```
service-name:event-name:user-id:2020-11-11
服务名：业务名：用户id：日期

用于计数用户每天的聊天记录数，并且没有设置过期时间.
```
### 查询大的key
使用scan扫描key，MEMORY 可以获取内存占用大小，TTL 可以获取key过期时间。
```lua
# 扫描key
> SCAN 0 COUNT 100

# 获取键的内存占用
> MEMORY USAGE key1       

# 获取键的剩余生存时间
> TTL key1    
```

### 永不过期的key、业务无用key
针对这种业务TTL=-1，不过期的key。使用scan + del 批量处理。
```python

import redis

cursor = '0'
pattern = '*'
count = 1000

r = redis.Redis()

while cursor != '0':
    scan_result = r.scan(cursor, match=pattern, count=count)
    cursor = scan_result[0]
    keys = scan_result[1]

    for key in keys:
        r.delete(key)

return "Deletion completed."

```
也可以用其他语言处理。

### 监控参考: 阿里云redis
* https://help.aliyun.com/document_detail/265988.html?spm=a2c4g.200631.0.0

### 解决
做大key扫描之后，找到了一部分key对应的value，每个占用都是250k。堡垒机链接redis之后，发现value有点不对。看了代码后发现，之前同事做服务迁移，有个redis的参数在之前的接口中是作为过期时间259200s，迁移之后的接口用作value的长度了。对应参数之后，删除之前的key，key个数接口9000个，之后redis内存从3.3G降低到660M左右。
```java
250k来源：
3 day=3*24*60*60 second=259200 second  

内存下降估算：
250k*9000=2250~2.3g
3.3g-700m~2.6g
考虑到字符额外的存储消耗，所以可以看到下降内存正好一致。
```
## 总结
* redis key需要结合业务设置过期时间，或者指定删除策略，避免占用空间。
* 使用sacn+del命令可以删除大量redis key。
* 使用scan+memory命令可以查询最大的key。