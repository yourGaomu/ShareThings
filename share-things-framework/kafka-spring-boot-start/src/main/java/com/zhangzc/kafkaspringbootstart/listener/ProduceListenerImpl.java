package com.zhangzc.kafkaspringbootstart.listener;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.lang.Nullable;

/**
 * Kafka生产者监听器
 * <p>用于监听消息发送的成功和失败事件</p>
 * <p>注意：ProducerListener接口的所有方法都是default方法，可以选择性重写</p>
 *
 * @author zhangzc
 */
@Slf4j
public class ProduceListenerImpl implements ProducerListener<String, String> {

    /**
     * 消息发送成功回调
     * <p>当消息成功发送到Kafka后触发</p>
     *
     * @param producerRecord 发送的消息记录
     * @param recordMetadata 消息元数据（分区、偏移量等）
     */
    @Override
    public void onSuccess(ProducerRecord<String, String> producerRecord, RecordMetadata recordMetadata) {
        log.info("消息发送成功 | Topic: {} | Partition: {} | Offset: {} | Key: {} | Value: {}",
                recordMetadata.topic(),
                recordMetadata.partition(),
                recordMetadata.offset(),
                producerRecord.key(),
                producerRecord.value());
        //存入数据库里面进行记录
        //todo

    }

    /**
     * 消息发送失败回调
     * <p>当消息发送失败时触发（重试次数用尽后）</p>
     *
     * @param producerRecord 发送失败的消息记录
     * @param recordMetadata 消息元数据（可能为null）
     * @param exception      失败原因
     */
    @Override
    public void onError(ProducerRecord<String, String> producerRecord,
                        @Nullable RecordMetadata recordMetadata,
                        Exception exception) {
        log.error("消息发送失败 | Topic: {} | Key: {} | Value: {} | Error: {}",
                producerRecord.topic(),
                producerRecord.key(),
                producerRecord.value(),
                exception.getMessage(),
                exception);

        // TODO: 可以在这里添加失败处理逻辑
        // 例如：保存到数据库、发送告警、重试等
    }
}
