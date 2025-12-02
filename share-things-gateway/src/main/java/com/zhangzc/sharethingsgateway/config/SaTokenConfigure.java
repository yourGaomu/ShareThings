package com.zhangzc.sharethingsgateway.config;


import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.listenerspringbootstart.utills.OnlineUserUtil;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Sa-Token 权限认证] 配置类
 *
 * @author click33
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SaTokenConfigure {

    private final GlobalContext globalContext;
    private final OnlineUserUtil onlineUserUtil;

    // 注册 Sa-Token全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")    /* 拦截全部path */
                // 鉴权方法：每次访问进入
                // 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
                .setBeforeAuth((object) -> {
                    SaRequest request = SaHolder.getRequest();
                    String requestPath = request.getRequestPath();
                    log.info("当前的请求路径是：{}参数是：{}", requestPath);
                    boolean isLogin;
                    try {
                        StpUtil.isLogin();
                        isLogin = true;
                    } catch (Exception e) {
                        isLogin = false;
                    }
                    if (isLogin) {
                        try {
                            Object loginId = StpUtil.getLoginId();
                            Long l = onlineUserUtil.addOnlineCount(String.valueOf(loginId));
                            log.info("==> 在线人数增加成功:{},当前在线人数:{},请求地址:{}", loginId, l);
                        } catch (Exception e) {
                            log.info("==> 在线人数增加失败:原因如下:{} ", e.getMessage());
                        }
                    } else {
                        String remoteAddr = request.getHeader("X-Real-IP");
                        Long l = onlineUserUtil.addOnlineCount(remoteAddr);
                        log.info("==> 在线人数增加成功:{},当前在线人数:{},请求地址:{}", remoteAddr, l);
                    }
                })

                .setAuth(obj -> {
                    log.info("==================> SaReactorFilter, Path: {}", SaHolder.getRequest().getRequestPath());
                    // 登录校验
                    SaRouter.match("/**") // 拦截所有路由
                            .notMatch("/auth/phone-login") // 排除登录接口
                            .notMatch("/auth/send-code") // 排除验证码发送接口
                            .check(r -> {
                                        StpUtil.checkLogin();
                                        Object loginId = StpUtil.getLoginId();
                                        GlobalContext.set(loginId.toString());
                                    }
                            ) // 校验是否登录
                    ;
                    // 权限认证 -- 不同模块, 校验不同权限
                    //SaRouter.match("/auth/logout", r -> StpUtil.checkPermission("app:note:publish"));
                    // SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
                    // SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
                    // SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));
                    // SaRouter.match("/orders/**", r -> StpUtil.checkPermission("orders"));
                    // 更多匹配 ...  */
                })
                .setError(e -> {
                    log.info("==================> SaReactorFilter, Error: {}", e.getMessage());
                    return R.error("500", e.getMessage());
                });
    }
}

