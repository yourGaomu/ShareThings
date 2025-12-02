package com.zhangzc.globalcontextspringbootstart.interapt;


import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 用户上下文解密过滤器
 * 从请求头中提取加密的用户ID，解密后设置到GlobalContext
 * 
 * 注意：此过滤器不再使用 @Component 自动注册
 * 而是通过 ContextConfig 中的 @ConditionalOnProperty 条件化注入
 */
@Slf4j
@Order(-100)
public class OnceFilter extends OncePerRequestFilter {
    
    private static final String USER_CONTEXT_HEADER = "X-User-Context";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    private static final long MAX_TIMESTAMP_DIFF = 300000; // 5分钟有效期

    @Value("${security.aes.secret-key:12345678901234567890123456789012}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头中获取加密的用户上下文
            String encrypted = request.getHeader(USER_CONTEXT_HEADER);
            
            if (encrypted != null && !encrypted.isEmpty()) {
                // 解密
                String decrypted = decrypt(encrypted);
                
                // 解析：userId|timestamp
                String[] parts = decrypted.split("\\|");
                if (parts.length == 2) {
                    String userId = parts[0];
                    long timestamp = Long.parseLong(parts[1]);
                    
                    // 验证时间戳（防重放攻击，可选）
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - timestamp > MAX_TIMESTAMP_DIFF) {
                        log.warn("用户上下文已过期: userId={}, timestamp={}", userId, timestamp);
                    } else {
                        // 设置到 GlobalContext
                        GlobalContext.set(userId);
                        log.debug("已解密用户ID并设置到上下文: userId={}", userId);
                    }
                } else {
                    log.warn("用户上下文格式错误: {}", decrypted);
                }
            } else {
                log.debug("请求头中未找到用户上下文，可能是未登录请求");
            }
            
            // 继续过滤器链
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.warn("解密用户上下文失败: {}", e.getMessage());
            // 解密失败不影响请求继续，因为可能是未登录的公开接口
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理上下文，防止内存泄漏
            GlobalContext.remove();
        }
    }

    /**
     * AES 解密（输入是 Base64 编码的密文）
     */
    private String decrypt(String encryptedBase64) throws Exception {
        // Base64 解码
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
        
        // 校验密钥长度
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new IllegalArgumentException("密钥长度必须是16或32字节");
        }
        
        // AES 解密
        SecretKeySpec key = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
