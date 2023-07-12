### 索引
update_time

### 没走索引
user_id,union_id

agent_code,沟通时间

### 视图的使用和问题

### limit 改写
SELECT
	msg_id msgId,
	from_user_id fromUserId,
	msg_time msgTime,
	msg_type msgType,
	room_id roomId,
	inner_id innerId,
	inner_name innerName 
FROM
	t_enterprise_more_chat_content 
WHERE
	msg_id IN ( SELECT msg_id FROM ( SELECT msg_id FROM t_enterprise_more_chat_content WHERE msg_time BETWEEN '2022-06-16 00:00:00.0' AND '2022-06-30 23:59:00.0' ORDER BY msg_time ASC LIMIT 156000, 500 ) AS more_chat_temp )

SELECT
    t1.msg_id AS msgId,
    t1.from_user_id AS fromUserId,
    t1.msg_time AS msgTime,
    t1.msg_type AS msgType,
    t1.room_id AS roomId,
    t1.inner_id AS innerId,
    t1.inner_name AS innerName 
FROM
    t_enterprise_more_chat_content AS t1
JOIN (
    SELECT
        msg_id
    FROM
        t_enterprise_more_chat_content
    WHERE
        msg_time BETWEEN '2022-06-16 00:00:00.0' AND '2022-06-30 23:59:00.0'
    ORDER BY
        msg_time ASC
    LIMIT 156000, 500
) AS t2 ON t1.msg_id = t2.msg_id;

### 分组取最大
EXPLAIN
UPDATE t_policy_renewal AS t1
JOIN (
    SELECT policy_code, MAX(renewal_period) AS max_renewal_period
    FROM t_policy_renewal
    WHERE renewal_period IS NOT NULL 
    GROUP BY policy_code
) AS t2 ON t1.policy_code = t2.policy_code AND t1.renewal_period = t2.max_renewal_period
SET t1.is_latest = 1;

explain 
UPDATE t_policy_renewal 
SET is_latest = 1 
WHERE id IN (
    SELECT id FROM (
        SELECT substring_index(group_concat(id ORDER BY renewal_period DESC), ',', 1) AS id
        FROM t_policy_renewal 
        WHERE renewal_period IS NOT NULL 
        GROUP BY policy_code
    ) AS subquery
);

explain	
SELECT t1.id
FROM t_policy_renewal AS t1
INNER JOIN (
    SELECT policy_code, MAX(renewal_period) AS max_renewal_period
    FROM t_policy_renewal
    GROUP BY policy_code
) AS t2
ON t1.policy_code = t2.policy_code AND t1.renewal_period = t2.max_renewal_period;

explain SELECT substring_index(group_concat(id order by renewal_period desc) , ',' , 1 )
    FROM t_policy_renewal
		where renewal_period is not null
    GROUP BY policy_code 