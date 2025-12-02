package com.zhangzc.globalcontextspringbootstart.config;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.globalcontextspringbootstart.properties.GlobalContextFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局上下文配置类
 * 
 * 注意：Servlet Filter 的配置已移至 ServletFilterConfig
 * 避免 WebFlux 环境（Gateway）加载时出现 ClassNotFoundException
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(GlobalContextFilter.class)
public class ContextConfig {
    
    @Bean
    public GlobalContext globalContext(){
        return new GlobalContext();
    }
}
