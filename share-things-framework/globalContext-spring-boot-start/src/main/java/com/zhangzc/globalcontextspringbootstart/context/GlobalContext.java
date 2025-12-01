package com.zhangzc.globalcontextspringbootstart.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
public class GlobalContext {
    private static String defaultKey = "defaultKey";
    // 初始化一个 ThreadLocal 变量
    private static final ThreadLocal<Map<String, Object>> LOGIN_USER_CONTEXT_THREAD_LOCAL
            = TransmittableThreadLocal.withInitial(HashMap::new);


    public static void set(Object value) {
        LOGIN_USER_CONTEXT_THREAD_LOCAL.get().put(defaultKey, value);
    }

    public static void set(String key, Object value) {
        LOGIN_USER_CONTEXT_THREAD_LOCAL.get().put(key, value);
    }

    public static Object get(String key) {
        return LOGIN_USER_CONTEXT_THREAD_LOCAL.get().get(key);
    }

    public static Object get() {
        return LOGIN_USER_CONTEXT_THREAD_LOCAL.get().get(defaultKey);
    }

    public static void remove() {
        LOGIN_USER_CONTEXT_THREAD_LOCAL.remove();
    }
}
