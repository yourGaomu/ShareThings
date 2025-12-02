package com.zhangzc.globalcontextspringbootstart.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GlobalContext 过滤器配置类
 * 
 * @author xiaojie
 * @version 1.0
 * @description: 全局上下文配置
 * @date 2022/8/26 0:18
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "zhangzc.filter")
public class GlobalContextFilter {

    /**
     * 是否启用微服务上下文同步
     * true: 启用解密过滤器（微服务架构）
     * false: 不启用过滤器（单体应用）
     */
    private Boolean enableSyncContext = false;

    @PostConstruct()
    public void init() {
        log.info("==> [GlobalContext Starter] 配置初始化完成: enableSyncContext={}", enableSyncContext);
        if (Boolean.TRUE.equals(enableSyncContext)) {
            log.info("==> [GlobalContext Starter] 微服务模式已启用，将在 Servlet 环境中自动解密请求头中的用户上下文");
            log.info("==> [GlobalContext Starter] 注意：WebFlux 环境（如 Gateway）将自动跳过 Filter 注入");
        } else {
            log.info("==> [GlobalContext Starter] 单体应用模式，不启用解密过滤器");
        }
    }
}
