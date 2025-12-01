package com.zhangzc.globalcontextspringbootstart.interapt;


import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Object userId = GlobalContext.get();
        if (userId != null) {
            requestTemplate.header("userId", String.valueOf(userId));
            log.info("########## feign 请求设置请求头 userId: {}", userId);
        }
    }
}
