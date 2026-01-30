package com.zhangzc.sharethinguserimpl.consume

import com.fasterxml.jackson.core.type.TypeReference

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil
import com.zhangzc.sharethingscommon.enums.UserActionEnum
import com.zhangzc.sharethingscommon.utils.PageResponse
import com.zhangzc.sharethingscommon.utils.TimeUtil
import com.zhangzc.sharethinguserimpl.pojo.mongo.HeatBehaviorRecordMongo
import org.apache.dubbo.config.annotation.DubboService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
@DubboService
open class UserConsume(
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
    open fun consumeUserAction(record: ConsumerRecord<String, Object>, ack: Acknowledgment) {
        //开始消费
        //用户Id
        record.value()?.let {
            //获取枚举类
            var parseObject =  JsonUtils
                .parseObject(it.toString(), object : TypeReference<Map<String, String>>() {})

            val actionEnum = UserActionEnum.valueOfActionName(parseObject.keys.first())
            //获取操作之后的结果
            val handleUserActionResult = handleUserAction(actionEnum, parseObject.values.first(),
                parseObject.get("articleId") ?:run { "" })
            handleUserActionResult
        }?.let{
            //判断是否消费成功
            if (it){
                ack.acknowledge()
            }
            else{
                log.error("消费失败")
                throw RuntimeException("消费失败")
            }
        } ?: run {
            log.error("用户动作行为为空")
        }
    }

    private fun handleUserAction(
        actionEnum: UserActionEnum,
        it: String,
        targetId: String
    ): Boolean {
        var result: Boolean = false
        when (actionEnum) {
            UserActionEnum.ARTICLE_READ -> {
                //文章阅读
                result = handleOnArticleReading(actionEnum, it,targetId)
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

    private fun handleOnArticleReading(actionEnum: UserActionEnum, userId: String,targetId: String): Boolean {
        //构建
        val heatBehaviorRecordMongo = HeatBehaviorRecordMongo()
        heatBehaviorRecordMongo.userId = userId.toLong()
        heatBehaviorRecordMongo.targetId = targetId.toLong()
        heatBehaviorRecordMongo.targetType = 1
        heatBehaviorRecordMongo.behaviorTypeId = actionEnum.id
        heatBehaviorRecordMongo.behaviorStatus = 1
        heatBehaviorRecordMongo.extInfo = mapOf(userId to actionEnum.description)
        heatBehaviorRecordMongo.updateTime= TimeUtil.getDateTime(LocalDateTime.now())
        heatBehaviorRecordMongo.createTime= TimeUtil.getDateTime(LocalDateTime.now())
        try {
            mongoUtil.insert(heatBehaviorRecordMongo, "user_behavior_record")
            return true
        } catch (e: Exception) {
            //如果错误类型是DuplicateKeyException跳过
            if (e is DuplicateKeyException){
                return true
            }
            log.error(e.message)
            return false
        }
    }

}