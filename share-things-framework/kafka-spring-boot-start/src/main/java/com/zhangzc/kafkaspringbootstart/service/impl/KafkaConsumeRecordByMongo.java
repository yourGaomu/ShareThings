package com.zhangzc.kafkaspringbootstart.service.impl;

import com.zhangzc.sharethingscommon.utils.TimeUtil;
import com.zhangzc.kafkaspringbootstart.core.mongo.MogoKafkaRecord;
import com.zhangzc.kafkaspringbootstart.service.StoreKafkaRecord;
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;

@RequiredArgsConstructor
public class KafkaConsumeRecordByMongo implements StoreKafkaRecord {
    private final MongoUtil mongoUtil;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void storeKafkaRecordOnSuccess(ProducerRecord<String, String> producerRecord, RecordMetadata recordMetadata) {
        Object execute = transactionTemplate.execute(status -> {
            try {
                MogoKafkaRecord mongoKafkaRecord = new MogoKafkaRecord();
                mongoKafkaRecord.setMessageId(Arrays.toString(producerRecord.headers().lastHeader("messageId").value()));
                mongoKafkaRecord.setContent(producerRecord.value());
                mongoKafkaRecord.setOffset(recordMetadata.offset());
                mongoKafkaRecord.setPartition(recordMetadata.partition());
                mongoKafkaRecord.setTopic(producerRecord.topic());
                mongoKafkaRecord.setType("SEND");
                mongoKafkaRecord.setStatus("SUCCESS");
                mongoKafkaRecord.setCreateTime(TimeUtil.getDateTime(LocalDateTime.now()));
                mongoUtil.insert(mongoKafkaRecord, "kafka_record");
                return true;
            } catch (Exception e) {
                //数据入库有问题
                status.setRollbackOnly();
                return false;
            }
        });
    }

    @Override
    public void storeKafkaRecordOnFail(ProducerRecord<String, String> producerRecord, Exception exception) {
        transactionTemplate.execute(status -> {
            try {
                MogoKafkaRecord mongoKafkaRecord = new MogoKafkaRecord();
                mongoKafkaRecord.setMessageId(Arrays.toString(producerRecord.headers().lastHeader("messageId").value()));
                mongoKafkaRecord.setContent("消息发送失败：" + producerRecord.value() + ";" + "异常消息是：" + exception.getMessage());
                mongoKafkaRecord.setOffset(-1L);
                mongoKafkaRecord.setPartition(-1);
                mongoKafkaRecord.setTopic(producerRecord.topic());
                mongoKafkaRecord.setType("SEND");
                mongoKafkaRecord.setStatus("FAIL");
                mongoKafkaRecord.setCreateTime(TimeUtil.getDateTime(LocalDateTime.now()));
                mongoUtil.insert(mongoKafkaRecord, "kafka_record");
                return true;
            } catch (Exception e) {
                //进行回滚操作
                status.setRollbackOnly();
                return false;
            }
        });
    }
}


