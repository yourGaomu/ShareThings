-- KEYS[1] = 在线用户集合 key，例如 "online_users"
-- ARGV[1] = userId（可能为空字符串或 "null" 表示未登录）
-- ARGV[2] = userIp（如 IP 地址）
-- ARGV[3] = 过期时间（秒），如 300
-- ARGV[4] = 当前 Unix 时间戳（秒）

local key = KEYS[1]
local userId = ARGV[1]
local userIp = ARGV[2]
local expire_seconds = tonumber(ARGV[3])
local now = tonumber(ARGV[4])
local expire_at = now + expire_seconds

-- 确定当前应使用的 member 标识
local currentMember
if userId and userId ~= "" and userId ~= "null" then
    currentMember = userId
    -- 如果用户已登录，尝试删除之前可能存在的 userUrl 记录（避免重复计数）
    redis.call('ZREM', key, userIp)
else
    currentMember = userIp
end

-- 添加或更新当前成员的过期时间戳（score）
redis.call('ZADD', key, expire_at, currentMember)

-- 清理所有已过期的成员（score <= now）
redis.call('ZREMRANGEBYSCORE', key, '-inf', now)

-- 返回当前在线人数（转为字符串）
return tostring(redis.call('ZCARD', key))