package com.zhangzc.sharethingsgateway.config;


import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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
                    SaRequest request = SaHolder.getRequest();
                    String method = request.getMethod();
                    String path = request.getRequestPath();
                    
                    log.info("==================> SaReactorFilter, Method: {}, Path: {}", method, path);
                    
                    // 放行所有 OPTIONS 请求（CORS 预检）
                    if ("OPTIONS".equalsIgnoreCase(method)) {
                        log.info("放行 OPTIONS 预检请求: {}", path);
                        return;
                    }
                    // 登录校验
                    SaRouter.match("/**") // 拦截所有路由
                            .notMatch("/auth/phone-login") // 排除登录接口
                            .notMatch("/auth/send-code") // 排除验证码发送接口
                            .notMatch("/bbs/article/**") // 排除文章列表（公开浏览）
                            .notMatch("/bbs/article/getArticleCommentVisitTotal") // 排除统计数据（公开）
                            .notMatch("/bbs/comment/getLatestComment") // 排除最新评论（公开）
                            .notMatch("/bbs/user/getHotAuthorsList") // 排除热门作者列表（公开）
                            .notMatch("/actuator/**")
                            .notMatch("/error")// 排除 Actuator 健康检查
                            .check(r -> {
                                        StpUtil.checkLogin();
                                        Object loginId = StpUtil.getLoginId();
                                        GlobalContext.set(loginId.toString());
                                    }
                            ); // 校验是否登录;
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
                    
                    // 根据异常类型返回不同的错误码
                    String code = "500";
                    String message = e.getMessage();
                    
                    // 判断是否是登录认证异常
                    if (e.getMessage().contains("token") || e.getMessage().contains("未能读取")) {
                        code = "401"; // Unauthorized
                        message = "身份验证失败，请重新登录"; // 不暴露 Token 详情
                    }
                    
                    R<?> errorResponse = R.error(code, message);
                    try {
                        // 将 R 对象序列化为 JSON 字符串
                        return objectMapper.writeValueAsString(errorResponse);
                    } catch (JsonProcessingException ex) {
                        log.error("序列化错误响应失败", ex);
                        return "{\"code\":\"500\",\"desc\":\"Internal Server Error\"}";
                    }
                });
    }
}

