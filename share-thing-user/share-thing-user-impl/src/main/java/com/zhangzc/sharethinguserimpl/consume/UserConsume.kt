package com.zhangzc.sharethinguserimpl.consume

import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil
import com.zhangzc.sharethingscommon.enums.UserActionEnum
import com.zhangzc.sharethinguserimpl.pojo.mongo.HeatBehaviorRecordMongo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


@Component
class UserConsume(
    private val mongoUtil: MongoUtil
) {
    private val log = LoggerFactory.getLogger(UserConsume::class.java)

    /**
     * 消费用户行为
     * */
    @KafkaListener(topics = ["UserBehavior"])
    @AutoInserByRedis(
        strategy = AutoInserByRedis.DuplicateStrategy.SKIP, // 重复消息跳过
        enableAlert = true,                                   // 启用告警
        redisKeyPrefix = "kafka:offset"                      // Redis key前缀
    )
    fun consumeUserAction(record: ConsumerRecord<String, Object>, ack: Acknowledgment) {
        //开始消费
        //用户Id
        record.value()?.let {
            //获取枚举类
            val actionEnum = UserActionEnum.valueOfActionName(record.key())
            //获取操作之后的结果
            val handleUserActionResult = handleUserAction(actionEnum, it.toString())
            handleUserActionResult
        }?.let{
            //判断是否消费成功
            if (it){
                ack.acknowledge()
            }
            else{
                log.error("消费失败")
            }
        } ?: run {
            log.error("用户动作行为为空")
        }
    }

    private fun handleUserAction(
        actionEnum: UserActionEnum,
        it: String
    ): Boolean {
        var result: Boolean = false
        when (actionEnum) {
            UserActionEnum.ARTICLE_READ -> {
                //文章阅读
                result = handleOnArticleReading(actionEnum, it)
            }

            UserActionEnum.LIKE -> {
                //点赞
            }

            UserActionEnum.COMMENT -> {
                //评论
            }

            UserActionEnum.COLLECT -> {
                //收藏
            }

            UserActionEnum.SHARE -> {
                //分享
            }

            UserActionEnum.COMPLETE_READ -> {
                //完整阅读
            }
            else -> {
                //其他行为
            }
        }
        return result
    }

    private fun handleOnArticleReading(actionEnum: UserActionEnum, userId: String): Boolean {
        //构建
        val heatBehaviorRecordMongo = HeatBehaviorRecordMongo()
        heatBehaviorRecordMongo.userId = userId.toLong()
        heatBehaviorRecordMongo.targetId = 1
        heatBehaviorRecordMongo.targetType = 1
        heatBehaviorRecordMongo.behaviorTypeId = actionEnum.id
        heatBehaviorRecordMongo.behaviorStatus = 1
        heatBehaviorRecordMongo.extInfo = mapOf("key" to "value")
        try {
            mongoUtil.save(heatBehaviorRecordMongo, "user_behavior_record")
            return true
        } catch (e: Exception) {
            log.error(e.message)
            return false
        }
    }

}