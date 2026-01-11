package com.zhangzc.sharethingarticleimpl.consume;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis;
import com.zhangzc.sharethingarticleimpl.consts.KafKaConst;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingarticleimpl.server.FsArticleService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleConsume {

    private final FsArticleService fsArticleService;

    @KafkaListener(topics = KafKaConst.ADD_PV_TOPIC)
    @AutoInserByRedis(
            strategy = AutoInserByRedis.DuplicateStrategy.SKIP, // 重复消息跳过
            enableAlert = true,                                   // 启用告警
            redisKeyPrefix = "kafka:offset"                      // Redis key前缀
    )
    public void consume4Pv(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        if (record.value() == null) {
            ack.acknowledge();
        }
        System.out.println("处理文章PV统计，文章ID：" + record.value());
        boolean update = fsArticleService.lambdaUpdate()
                .eq(FsArticle::getId, record.value())
                .setSql("pv = pv + 1")
                .update();
        if (update) {
            ack.acknowledge();
        }
    }
}
