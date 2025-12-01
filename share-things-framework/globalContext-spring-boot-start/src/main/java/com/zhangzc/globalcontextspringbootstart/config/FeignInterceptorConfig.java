package com.zhangzc.globalcontextspringbootstart.config;


import com.zhangzc.globalcontextspringbootstart.interapt.FeignRequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class FeignInterceptorConfig {

    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

}
