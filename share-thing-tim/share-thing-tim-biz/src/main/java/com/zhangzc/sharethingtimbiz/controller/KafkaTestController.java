package com.zhangzc.sharethingtimbiz.controller;

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.sharethingtimbiz.pojo.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka测试Controller
 * <p>用于验证Kafka是否正常工作</p>
 *
 * @author zhangzc
 */
@RestController
@RequestMapping("/kafka/test")
@RequiredArgsConstructor
@Slf4j
public class KafkaTestController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 测试发送Kafka消息（字符串）
     *
     * @param message 消息内容
     * @return 发送结果
     */
    @GetMapping("/send")
    public String sendMessage(@RequestParam(defaultValue = "Hello Kafka!") String message) {
        try {
            String topic = "test-topic";
            kafkaTemplate.send(topic, message);
            log.info("消息发送成功 | Topic: {} | Message: {}", topic, message);
            return "消息发送成功: " + message;
        } catch (Exception e) {
            log.error("消息发送失败", e);
            return "消息发送失败: " + e.getMessage();
        }
    }

    /**
     * 测试发送User对象消息（正确方式）
     *
     * @param id 用户ID
     * @param name 用户名
     * @return 发送结果
     */
    @PostMapping("/sendUser")
    public String sendUserMessage(
            @RequestParam(defaultValue = "1") String id,
            @RequestParam(defaultValue = "test") String name) {
        try {
            String topic = "test-topic";
            
            // 创建User对象
            User user = new User(id, name);
            
            // ✅ 正确方式：将User对象序列化为JSON字符串
            String jsonMessage = JsonUtils.toJsonString(user);
            
            log.info("准备发送User消息 | Topic: {} | User: {} | JSON: {}", topic, user, jsonMessage);
            
            // 发送JSON字符串（不是双重序列化）
            kafkaTemplate.send(topic, jsonMessage);
            
            log.info("✅ User消息发送成功");
            return "✅ User消息发送成功: " + jsonMessage;
            
        } catch (Exception e) {
            log.error("❌ User消息发送失败", e);
            return "❌ User消息发送失败: " + e.getMessage();
        }
    }

    /**
     * 检查KafkaTemplate是否注入成功
     *
     * @return 检查结果
     */
    @GetMapping("/check")
    public String checkKafka() {
        if (kafkaTemplate != null) {
            log.info("✅ KafkaTemplate已成功注入");
            return "✅ Kafka配置正常，KafkaTemplate已成功注入";
        } else {
            log.error("❌ KafkaTemplate注入失败");
            return "❌ Kafka配置异常，KafkaTemplate未注入";
        }
    }
}
