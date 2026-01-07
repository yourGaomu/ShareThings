package com.zhangzc.sharethingtimbiz.controller;

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis;
import com.zhangzc.sharethingtimbiz.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka消费者
 * <p>使用@AutoInserByRedis注解实现自动偏移量管理，防止重复消费</p>
 */
@Component
@Slf4j
public class consume {

    /**
     * 消费test-topic消息
     * <p>注意：使用@AutoInserByRedis后，方法签名必须包含：</p>
     * <ul>
     *     <li>ConsumerRecord - Kafka消息对象</li>
     *     <li>Acknowledgment - 手动应答对象</li>
     * </ul>
     */
    @KafkaListener(topics = "test-topic")
    @AutoInserByRedis(
            strategy = AutoInserByRedis.DuplicateStrategy.SKIP, // 重复消息跳过
            enableAlert = true,                                   // 启用告警
            redisKeyPrefix = "kafka:offset"                      // Redis key前缀
    )
    public void onNormalMessage(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            log.info("📥 开始处理业务逻辑 | Topic: {} | Partition: {} | Offset: {}", 
                    record.topic(), record.partition(), record.offset());

            Object value = record.value();
            log.info("原始消息类型: {} | 内容: {}", value.getClass().getName(), value);
            
            User user;
            
            // 处理双重转义问题：如果消息是字符串类型，需要再次反序列化
            if (value instanceof String jsonStr) {
                log.info("消息为字符串类型，直接反序列化: {}", jsonStr);
                user = JsonUtils.parseObject(jsonStr, User.class);
            } else {
                // 如果已经是对象类型，先转成JSON再转回User
                log.info("消息为对象类型，先转成JSON: {}", value);
                String jsonStr = JsonUtils.toJsonString(value);
                user = JsonUtils.parseObject(jsonStr, User.class);
            }
            
            log.info("✅ 反序列化成功 | User: {}", user);
            
            // TODO: 业务处理逻辑
            processBusinessLogic(user);
            
            // 手动确认消息已消费
            ack.acknowledge();
            log.info("✅ 消息确认成功 | Offset: {}", record.offset());
            
        } catch (Exception e) {
            log.error("❌ Kafka消息消费失败 | Topic: {} | Partition: {} | Offset: {} | Error: {}", 
                    record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            // 注意：如果不调用ack.acknowledge()，消息会重新消费
            // 如果需要跳过错误消息，可以选择在这里调用 ack.acknowledge()
        }
    }
    
    /**
     * 业务处理逻辑
     */
    private void processBusinessLogic(User user) {
        // TODO: 实现具体的业务逻辑
        log.info("📊 执行业务处理 | UserId: {} | UserName: {}", user.getId(), user.getName());
    }


}
