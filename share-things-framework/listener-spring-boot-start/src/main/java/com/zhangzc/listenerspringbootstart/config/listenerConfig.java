package com.zhangzc.listenerspringbootstart.config;

import com.zhangzc.listenerspringbootstart.service.OnlineUserCount;
import com.zhangzc.listenerspringbootstart.service.impl.onlineRedisUserCountImpl;
import com.zhangzc.listenerspringbootstart.utills.OnlineUserUtil;
import com.zhangzc.redisspringbootstart.utills.LuaUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.redisspringbootstart.utills.RedissonUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ListenerProperties.class)
public class listenerConfig {


    @Bean
    @ConditionalOnProperty(prefix = "zhangzc.listener", name = "enableRedis", havingValue = "true")
    public onlineRedisUserCountImpl onlineRedisUserCountImpl(RedisUtil redisUtil, RedissonUtil redissonUtil, LuaUtil luaUtil) {
        return new onlineRedisUserCountImpl(redisUtil, redissonUtil, luaUtil);
    }


    @Bean
    public OnlineUserUtil onlineUserUtil(OnlineUserCount onlineUserCount) {
        return new OnlineUserUtil(onlineUserCount);
    }



}
