package com.zhangzc.sharethingsgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许携带凭证时，必须使用 addAllowedOriginPattern 而不是 addAllowedOrigin
        config.addAllowedOriginPattern("http://localhost:*"); // 支持所有 localhost 端口
        config.addAllowedOriginPattern("http://127.0.0.1:*"); // 支持 127.0.0.1
        
        config.addAllowedMethod("*"); // 允许所有方法（包括 OPTIONS）
        config.addAllowedHeader("*"); // 允许所有请求头
        config.setAllowCredentials(true); // 允许携带凭证（cookies、Authorization 等）
        
        // 暴露响应头，允许前端读取
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-User-Context");
        config.addExposedHeader("XSRF-TOKEN");
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}