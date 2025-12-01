-- KEYS[1] = 在线用户集合 key，例如 "online_users"
-- ARGV[1] = 用户标识（user_id）

local key = KEYS[1]
local user = ARGV[1]

-- 1. 尝试移除该用户
redis.call('ZREM', key, user)

-- 2. 清理可能残留的过期用户（可选但推荐，保持一致性）
local now = redis.call('TIME')[1]  -- 获取 Redis 当前秒级时间戳
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 3. 返回当前在线人数
return redis.call('ZCARD', key)