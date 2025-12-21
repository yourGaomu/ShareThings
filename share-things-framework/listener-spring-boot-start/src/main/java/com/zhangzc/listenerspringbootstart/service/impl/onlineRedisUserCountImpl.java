package com.zhangzc.listenerspringbootstart.service.impl;

import com.zhangzc.listenerspringbootstart.enums.OnlineUserCountRedis;
import com.zhangzc.listenerspringbootstart.service.OnlineUserCount;
import com.zhangzc.redisspringbootstart.utills.LuaUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.redisspringbootstart.utills.RedissonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Service
public class onlineRedisUserCountImpl implements OnlineUserCount {
    private final RedisUtil redisUtil;
    private final RedissonUtil redissonUtil;
    private final LuaUtil luaUtil;
    String redisUserCountKey = OnlineUserCountRedis.redisUserCountKey;

    @Override
    public Long addOnlineCount(String userID) {
        List<Object> data = List.of(userID, 300L, System.currentTimeMillis() / 1000);
        Object execute = luaUtil.execute("add_redis_online_user",
                redisUserCountKey, data);
        return Long.parseLong(execute.toString());

    }

    @Override
    public Long addOnlineCount(String userId, String userIp) {
        List<Object> data = List.of(userId, userIp,300L, System.currentTimeMillis() / 1000);
        Object execute = luaUtil.execute("add_redis_online_user_login",
                redisUserCountKey, data);
        return Long.parseLong(execute.toString());
    }

    @Override
    public Long subOnlineCount(String userID) {
        List<Object> data = List.of(userID);
        Object execute = luaUtil.execute("delete_redis_online_user", redisUserCountKey, data);
        return Long.parseLong(execute.toString());
    }

    @Override
    public Long getOnlineCount() {
        long now = System.currentTimeMillis() / 1000;
        List<Object> data = List.of(now);
        Object execute = luaUtil.execute("get_redis_online_user", redisUserCountKey, data);
        return Long.valueOf(execute.toString());
    }
}
