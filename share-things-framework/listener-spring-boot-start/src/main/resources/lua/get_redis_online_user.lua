-- KEYS[1] = 在线用户集合的 key（如 "online_users"）
-- ARGV[1] = 当前 Unix 时间戳（秒），用于清理过期用户

local key = KEYS[1]
local now = tonumber(ARGV[1])

-- 先清理已过期的用户（score <= 当前时间戳），保证统计的是有效在线人数
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 获取当前有效在线人数并转为字符串返回（保持和原脚本一致的返回格式）
return tostring(redis.call('ZCARD', key))