package com.zhangzc.globalcontextspringbootstart.config;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextConfig {
    @Bean
    public GlobalContext globalContext(){
        return new GlobalContext();
    }
}
