package com.zhangzc.sharethingsgateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.listenerspringbootstart.utills.OnlineUserUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Order(-99)
@RequiredArgsConstructor
public class onlyOneFilter extends OncePerRequestFilter {
    private final OnlineUserUtil onlineUserUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request
            , HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            //是否登录
            boolean isLogin = false;
            //获取当前的请求的地址
            String requestURI = request.getRequestURI();
            //获取当前的请求的cookie
            Cookie[] cookies = request.getCookies();
            //获取当前的请求的session
            String sessionId = request.getRequestedSessionId();
            //获取当前的请求的ip
            String remoteAddr = request.getRemoteAddr();
            //请求头获取
            String header = request.getHeader("Authorization");
            try {
                StpUtil.isLogin();
                isLogin = true;
            } catch (Exception e) {
                isLogin = false;
            }
            //已经登录
            if (isLogin) {
                Object loginId = StpUtil.getLoginId();
                onlineUserUtil.addOnlineCount(loginId.toString());
            }else {
                //未登录
                onlineUserUtil.addOnlineCount(remoteAddr);
            }
            filterChain.doFilter(request, response);
        } finally {
            GlobalContext.remove();
        }
    }
}
