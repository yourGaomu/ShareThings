package com.zhangzc.sharethingsgateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingsgateway.Enum.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 用户ID加密传递过滤器
 * 在网关中将登录用户ID加密后添加到请求头，传递给下游服务
 */
@Slf4j
@Component
public class UserIdEncryptFilter implements GlobalFilter, Ordered {

    private static final String USER_CONTEXT_HEADER = "X-User-Context";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    @Value("${security.aes.secret-key:12345678901234567890123456789012}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Object loginId = null;
        try {
            try {
                loginId = StpUtil.getLoginId();
            } catch (Exception e) {
                    log.info("获取用户ID失败: {}", e.getMessage());
                try {
                    loginId = GlobalContext.get();
                } catch (Exception ex) {
                    log.info("获取用户ID失败: {}", ex.getMessage());
                    throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
                }
            }
            long timestamp = System.currentTimeMillis();

            // 组合数据：userId|timestamp
            System.out.println("==> loginId: " + loginId);
            String plainText = loginId + "|" + timestamp;

            // AES 加密并 Base64 编码
            String encrypted = encrypt(plainText);

            // 添加到请求头
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header(USER_CONTEXT_HEADER, encrypted)
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            log.debug("已加密用户ID并添加到请求头: userId={}", loginId);

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            return chain.filter(exchange);
        } finally {
            GlobalContext.remove();
        }
    }

    @Override
    public int getOrder() {
        // 在SaToken过滤器(-100)之后执行
        return -99;
    }

    /**
     * AES 加密（加密后返回 Base64 编码字符串）
     */
    private String encrypt(String plainText) throws Exception {
        // 校验密钥长度（AES-128需要16字节，AES-256需要32字节）
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new IllegalArgumentException("密钥长度必须是16或32字节");
        }

        SecretKeySpec key = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 加密后进行 Base64 编码（只编码一次）
        return Base64.getEncoder().encodeToString(encrypted);
    }
}