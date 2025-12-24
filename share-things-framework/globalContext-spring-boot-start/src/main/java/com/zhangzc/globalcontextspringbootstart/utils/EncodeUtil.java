package com.zhangzc.globalcontextspringbootstart.utils;


import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密/解密工具类
 * 提供两种级别的加密/解密方法：
 * 1. 高层方法：encode/decode - 自动处理 userId + timestamp
 * 2. 底层方法：encryptRaw/decryptRaw - 原始加密/解密，不处理业务逻辑
 */
@RequiredArgsConstructor
@Slf4j
public class EncodeUtil {

    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    private static final long MAX_TIMESTAMP_DIFF = 300000; // 5分钟有效期

    @Value("${security.aes.secret-key:12345678901234567890123456789012}")
    private String secretKey;

    // ==================== 高层方法：业务层使用 ====================

    /**
     * 高层加密方法：自动加入时间戳
     * 用于业务层加密 userId
     * 
     * @param userId 用户ID
     * @return Base64 编码的密文（包含 userId|timestamp）
     */
    public String encode(String userId) throws Exception {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("加密的userId不能为空");
        }
        
        // 拼接：userId|timestamp
        long timestamp = System.currentTimeMillis();
        String plainText = userId + "|" + timestamp;
        
        return encryptRaw(plainText);
    }

    /**
     * 高层解密方法：自动验证时间戳并提取 userId
     * 用于业务层解密 userId
     * 
     * @param encrypted Base64 编码的密文
     * @return 用户ID，如果解密失败或过期则返回 null
     */
    public String decode(String encrypted) throws Exception {
        if (encrypted == null || encrypted.isEmpty()) {
            log.warn("解密的密文不能为空");
            return null;
        }
        
        String decrypted = decryptRaw(encrypted);

        // 解析：userId|timestamp
        String[] parts = decrypted.split("\\|");
        if (parts.length == 2) {
            // 验证时间戳是否在有效期内
            long timestamp = Long.parseLong(parts[1]);
            long currentTime = System.currentTimeMillis();
            if (currentTime - timestamp > MAX_TIMESTAMP_DIFF) {
                log.warn("加密的userId已过期，时间戳：{}", timestamp);
                return null;
            }
            return parts[0];
        } else {
            log.warn("解密后的数据格式错误：{}", decrypted);
            return null;
        }
    }

    // ==================== 底层方法：OnceFilter 等使用 ====================

    /**
     * 底层加密方法：原始 AES 加密
     * 直接加密任意字符串，不处理业务逻辑
     * 
     * @param plainText 明文
     * @return Base64 编码的密文（URL 安全）
     */
    public String encryptRaw(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("加密的明文不能为空");
        }
        
        // 校验密钥长度（AES-128需要16字节，AES-256需要32字节）
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new IllegalArgumentException("密钥长度必须是16或32字节");
        }

        SecretKeySpec key = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 使用 URL 安全的 Base64 编码（避免 / 和 + 字符）
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    }

    /**
     * 底层解密方法：原始 AES 解密
     * 直接解密，不处理业务逻辑
     * 
     * @param encryptedBase64 Base64 编码的密文（URL 安全格式）
     * @return 明文
     */
    public String decryptRaw(String encryptedBase64) throws Exception {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            throw new IllegalArgumentException("解密的密文不能为空");
        }
            
        // 使用 URL 安全的 Base64 解码
        byte[] encryptedBytes = Base64.getUrlDecoder().decode(encryptedBase64);
    
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
