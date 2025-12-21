package com.zhangzc.sharethingsgateway.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.reactor.context.SaReactorHolder;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(-99)   // 必须在 SaReactorFilter (WebFilter, 默认 -100) 之前执行
public class SaTokenContextWebFilter implements WebFilter {
  public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    log.info("Gftong Sa-Token Context WebFilter");
    SaReactorSyncHolder.setContext(exchange);
    return chain.filter(SaReactorSyncHolder.getExchange())
      .contextWrite(ctx -> SaReactorHolder.setContext(ctx, exchange, chain))
      .doFinally((t) -> {
          log.info("Gftong Sa-Token Context WebFilter End");
          // 检查上下文是否有效,只有无效时才清除
          // 这样可以避免异步提前清除导致后续 GlobalFilter 无法使用上下文
          if (!SaManager.getSaTokenContext().isValid()) {
              SaReactorSyncHolder.clearContext();
          }
      });
  }
}
