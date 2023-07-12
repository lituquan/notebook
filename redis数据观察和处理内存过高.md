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
看了几个db的key数值，有个db的key数值有450w，比其他的都多。再继续看，看到具体的目录和内部一个数据服务有关系。key的组成如下：
```
service-name:event-name:user-id:2020-11-11
服务名：业务名：用户名：日期

用于计数用户每天的聊天记录数
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
```lua
local cursor = '0'
local pattern = 'your_pattern' -- 替换为你的键匹配模式
local count = 1000

-- 获取所有匹配模式的键及其内存使用量
local keys = {}
repeat
    local scan_result = redis.call('SCAN', cursor, 'MATCH', pattern, 'COUNT', count)
    cursor = scan_result[1]
    local batch_keys = scan_result[2]

    for _, key in ipairs(batch_keys) do
        local memory_usage = redis.call('MEMORY', 'USAGE', key)
        
        -- 只插入内存较大的键
        if tonumber(memory_usage) > <min_memory_threshold> then
            table.insert(keys, { key, tonumber(memory_usage) })
        end
    end
until cursor == '0'

-- 根据内存使用量排序，并仅保留前1000个键
table.sort(keys, function(a, b)
    return a[2] > b[2]
end)

local top_keys = {}
for i = 1, math.min(count, #keys) do
    table.insert(top_keys, keys[i][1])
end

return top_keys
```
### 永不过期的key、业务无用key
scan + del
```lua
/* 
    scan 0 MATCH insnail-data-search-service:if_wechat_chat_exist:*2020-11-11 count 1000
    EVAL "$(cat delete_keys.lua)" 0
*/
    local cursor = '0'
    local pattern = 'insnail-data-search-service:if_wechat_chat_exist:*2020*'
    local count = 1000

    repeat
        local scan_result = redis.call('SCAN', cursor, 'MATCH', pattern, 'COUNT', count)
        cursor = scan_result[1]
        local keys = scan_result[2]

        for _, key in ipairs(keys) do
            redis.call('DEL', key)
        end
    until cursor == '0'

    return "Deletion completed."
```
也可以用其他语言处理。

### 监控
https://help.aliyun.com/document_detail/265988.html?spm=a2c4g.200631.0.0
