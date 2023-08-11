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
```lua
# 第一次迭代，获取前100个键
> SCAN 0 COUNT 100

# 返回结果格式如下：
1) "234"    # 游标（cursor）
2) 
    1) "key1"
    2) "key2"

# 循环处理每个键
> MEMORY USAGE key1       # 获取键的内存占用

# 循环处理每个键
> TTL key1    # 获取键的剩余生存时间

# 重复上述步骤，直到找到所需数量的大键
```
完整扫描脚本：
```python
import redis
import heapq

cursor = '0'
pattern = 'service-name:event-name:*2020*'  # 匹配2020年的记录，替换为你的键匹配模式

import redis
import heapq

cursor = '0'
pattern = 'your_pattern'  # 替换为你的键匹配模式
count = 1000

r = redis.Redis()

keys = []

while cursor != '0':
    scan_result = r.scan(cursor, match=pattern, count=count)
    cursor = scan_result[0]
    batch_keys = scan_result[1]

    for key in batch_keys:
        memory_usage = r.memory_usage(key)

        heapq.heappush(keys, (int(memory_usage), key))
            
        # 保持最大堆大小为1000
        if len(keys) > count:
            heapq.heappop(keys)

# 获取前1000个键
top_keys = [key for _, key in heapq.nlargest(count, keys)]

return top_keys
```
这里我没有执行实践，因为在上面已经找到需要删除的key，所以没做大key扫描。
### 永不过期的key、业务无用key
针对这种业务TTL=-1，不过期的key。使用scan + del 批量处理。
```python
'''
    scan 0 MATCH service-name:event-name:*2020* count 1000 
'''
import redis

cursor = '0'
pattern = 'service-name:event-name:*2020*'
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

## 总结
* redis key需要结合业务设置过期时间，或者指定删除策略，避免占用空间。
* 使用sacn+del命令可以删除大量redis key。
* 使用scan+memory命令可以查询最大的key。