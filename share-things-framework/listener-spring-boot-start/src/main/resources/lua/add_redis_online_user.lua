-- KEYS[1] = 在线用户集合的 key，例如 "online_users"
-- ARGV[1] = 用户标识（如 user_id）
-- ARGV[2] = 过期时间（秒），例如 300 表示 5 分钟
-- ARGV[3] = 当前 Unix 时间戳（秒）

local key = KEYS[1]
local user = ARGV[1]
local expire_seconds = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local expire_at = now + expire_seconds

-- 添加或更新用户，设置其过期时间戳为 score
redis.call('ZADD', key, expire_at, user)

-- 清理已过期的用户（score <= now）
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 返回当前在线人数，但转为字符串
return tostring(redis.call('ZCARD', key))