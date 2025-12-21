-- save_banner.lua
-- KEYS[1] = hash key (e.g., "banners")
-- ARGV[1] = banner id (e.g., "1001")
-- ARGV[2] = banner object as JSON string

local hash_key = KEYS[1]
local banner_id = ARGV[1]
local banner_json = ARGV[2]

-- 写入单个 banner
redis.call('HSET', hash_key, banner_id, banner_json)

-- 设置过期时间为 14 天（14 * 24 * 3600 = 1209600 秒）
redis.call('EXPIRE', hash_key, 14 * 24 * 3600)

return redis.status_reply("OK")