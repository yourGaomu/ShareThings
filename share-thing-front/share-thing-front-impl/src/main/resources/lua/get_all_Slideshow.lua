-- 确保KEYS[1]存在，避免HGETALL调用失败
if not KEYS[1] or KEYS[1] == "" then
    return cjson.encode({code = -1, msg = "Hash Key不能为空"})
end

local hash_key = KEYS[1]
local all = redis.call('HGETALL', hash_key)

-- 空Hash返回空JSON对象
if #all == 0 then
    return cjson.encode({})
end

local result = {}
for i = 1, #all, 2 do
    local id = all[i]
    local json_str = all[i + 1]

    -- 容错：JSON解析失败时保留原始字符串，避免整个脚本报错
    local ok, data = pcall(cjson.decode, json_str)
    if ok then
        result[id] = data
    else
        -- 解析失败时记录错误，保留原始值
        result[id] = {
            original = json_str,
            error = "JSON解析失败：" .. data
        }
    end
end

-- 返回结构化JSON
return cjson.encode(result)