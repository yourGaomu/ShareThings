-- 【Redis Lua脚本：用户浏览历史分页查询】
-- 入参（ARGV）：ARGV[1]=用户ID  ARGV[2]=当前页码  ARGV[3]=每页条数
-- 返回值：Map<String,String> → {文章ID: 时间戳, 文章ID2: 时间戳2,...}（倒序分页）
local zsetKey =  Keys[1]
-- 入参强转+默认值，非数字/空则设默认1/10
local currentPage = tonumber(ARGV[1]) or 1
local pageSize = tonumber(ARGV[2]) or 10

-- 容错处理：页码/条数<1设默认，每页最大50条防查过多
if currentPage < 1 then currentPage = 1 end
if pageSize < 1 then pageSize = 10 end
if pageSize > 50 then pageSize = 50 end

-- 计算ZSet倒序分页索引（0开始，闭区间）
local startIndex = (currentPage - 1) * pageSize
local endIndex = currentPage * pageSize - 1

-- 执行查询：获取[artId(Str), timestamp(Str), ...]原始数组
local rawData = redis.call("ZREVRANGE", zsetKey, startIndex, endIndex, "WITHSCORES")
local resultMap = {}

-- 组装Map：key=文章ID(Str)，value=时间戳(Str)，无任何类型转换
for i = 1, #rawData, 2 do
    resultMap[rawData[i]] = rawData[i+1]
end

-- 返回纯字符串键值对Map，客户端直接解析为Map<String,String>
return resultMap