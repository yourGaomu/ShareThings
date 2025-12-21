-- save_banners_batch.lua
-- 适配仅传入 KEYS[1]（Hash Key）和 ARGV（id+json 键值对）的场景
-- KEYS[1] = hash key (e.g., "banners")
-- ARGV[1], ARGV[2], ..., pairs of (id, json_string)

-- 1. 校验核心参数
if not KEYS[1] or KEYS[1] == "" then
    return cjson.encode({
        code = -1,
        msg = "Hash Key 不能为空",
        success_count = 0
    })
end

local hash_key = KEYS[1]
local success_count = 0  -- 记录成功写入的字段数

-- 2. 校验 ARGV 参数数量（必须是偶数，因为是 id+json 成对传入）
if #ARGV == 0 then
    return cjson.encode({
        code = 0,
        msg = "无待写入的 banner 数据",
        success_count = 0
    })
end
if #ARGV % 2 ~= 0 then
    return cjson.encode({
        code = -2,
        msg = "参数数量异常：id 和 json 必须成对传入（当前参数数：" .. #ARGV .. "）",
        success_count = 0
    })
end

-- 3. 可选：全量替换旧数据（如需覆盖则取消注释）
-- redis.call('DEL', hash_key)

-- 4. 循环写入数据（每两个 ARGV 为一组：id + json）
local i = 1
while i <= #ARGV do
    local banner_id = ARGV[i]
    local banner_json = ARGV[i + 1]

    -- 跳过空值（避免写入无效数据）
    if banner_id and banner_id ~= "" and banner_json and banner_json ~= "" then
        redis.call('HSET', hash_key, banner_id, banner_json)
        success_count = success_count + 1
    end

    i = i + 2
end

-- 5. 返回结构化结果（便于 Java 端解析）
return cjson.encode({
    code = 1,
    msg = "数据写入成功",
    success_count = success_count,
    total_received = #ARGV / 2  -- 接收的总数据对数（含空值）
})