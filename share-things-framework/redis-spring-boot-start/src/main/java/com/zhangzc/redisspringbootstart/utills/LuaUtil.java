package com.zhangzc.redisspringbootstart.utills;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class LuaUtil {
    private RedisTemplate redisTemplate;

    public LuaUtil(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    public Object execute(String luaPath, String Hashkey, List<Object> data) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript();
        String path = creatLuaPath(luaPath);
        log.info("==> lua路径: {}", path);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        redisScript.setResultType(String.class);
        // 关键修正：List<Object> 转成 Object[]，传递给可变参数
        Object[] args = data.toArray(new Object[0]);
        log.info("==> 传递给 Lua 的 ARGV 参数：长度={}, 元素={}", args.length, Arrays.toString(args));
        Object execute = redisTemplate.execute(redisScript, Collections.singletonList(Hashkey), args);
        return  execute;
    }



    private String creatLuaPath(String luaPath){
        return  "/lua/"+luaPath+".lua";
    }

}
