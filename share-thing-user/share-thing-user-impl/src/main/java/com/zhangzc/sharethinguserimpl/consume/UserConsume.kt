package com.zhangzc.sharethinguserimpl.consume

import com.fasterxml.jackson.core.type.TypeReference

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil
import com.zhangzc.sharethingscommon.enums.UserActionEnum
import com.zhangzc.sharethingscommon.utils.PageResponse
import com.zhangzc.sharethingscommon.utils.TimeUtil
import com.zhangzc.sharethinguserimpl.pojo.domain.BbsBrowsingHistory
import com.zhangzc.sharethinguserimpl.pojo.mongo.HeatBehaviorRecordMongo
import com.zhangzc.sharethinguserimpl.service.BbsBrowsingHistoryService
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
    private val mongoUtil: MongoUtil,
    private val bbsBrowsingHistoryService: BbsBrowsingHistoryService
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

            // 健壮地查找动作Key（排除articleId）
            val actionKey = parseObject.keys.firstOrNull { key -> key != "articleId" }
                ?: throw RuntimeException("Invalid message format: missing action key")

            val actionEnum = UserActionEnum.valueOfActionName(actionKey)
            val userId = parseObject[actionKey] ?: throw RuntimeException("Invalid message format: missing userId")

            //获取操作之后的结果
            val handleUserActionResult = handleUserAction(actionEnum, userId,
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
                result = handleOnLike(actionEnum, it, targetId)
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
            UserActionEnum.PUBLISH_WORK -> {
                //发布作品
                result = handleOnPublishWork(actionEnum, it, targetId)
            }
            else -> {
                //其他行为
            }
        }
        return result
    }

    private fun handleOnArticleReading(actionEnum: UserActionEnum, userId: String,targetId: String): Boolean {
        //构建
        //用户行为构建记录
        val heatBehaviorRecordMongo = HeatBehaviorRecordMongo()
        heatBehaviorRecordMongo.userId = userId.toLong()
        heatBehaviorRecordMongo.targetId = targetId.toLong()
        heatBehaviorRecordMongo.targetType = 1
        heatBehaviorRecordMongo.behaviorTypeId = actionEnum.id
        heatBehaviorRecordMongo.behaviorStatus = 1
        heatBehaviorRecordMongo.extInfo = mapOf(userId to actionEnum.description)
        heatBehaviorRecordMongo.updateTime= TimeUtil.getDateTime(LocalDateTime.now())
        heatBehaviorRecordMongo.createTime= TimeUtil.getDateTime(LocalDateTime.now())
        //用户浏览记录
        val record = BbsBrowsingHistory()
        record.userId = userId.toLong()
        record.articleId = targetId.toLong()
        record.viewTime = TimeUtil.getDateTime(LocalDateTime.now())
        record.createTime = TimeUtil.getDateTime(LocalDateTime.now())
        record.updateTime = TimeUtil.getDateTime(LocalDateTime.now())
        record.isDeleted = 0
        try {
            // mongo插入 + 数据库保存
            mongoUtil.insert(heatBehaviorRecordMongo, "user_behavior_record")
            bbsBrowsingHistoryService.save(record)
            return true
        } catch (e: Exception) {
            if (e is DuplicateKeyException) {
                // 预期异常（唯一键重复）
                return true
            } else {
                // 非预期异常：记录日志，然后返回false
                log.error(e.message)
                return false
            }
        }
    }

    private fun handleOnLike(actionEnum: UserActionEnum, userId: String, targetId: String): Boolean {
        //构建
        //用户行为构建记录
        val heatBehaviorRecordMongo = HeatBehaviorRecordMongo()
        heatBehaviorRecordMongo.userId = userId.toLong()
        heatBehaviorRecordMongo.targetId = targetId.toLong()
        heatBehaviorRecordMongo.targetType = 1
        heatBehaviorRecordMongo.behaviorTypeId = actionEnum.id
        heatBehaviorRecordMongo.behaviorStatus = 1
        heatBehaviorRecordMongo.extInfo = mapOf(userId to actionEnum.description)
        heatBehaviorRecordMongo.updateTime = TimeUtil.getDateTime(LocalDateTime.now())
        heatBehaviorRecordMongo.createTime = TimeUtil.getDateTime(LocalDateTime.now())
        try {
            // mongo插入
            mongoUtil.save(heatBehaviorRecordMongo, "user_behavior_record")
            return true
        } catch (e: Exception) {
            log.error(e.message)
            return false
        }
    }

    private fun handleOnPublishWork(actionEnum: UserActionEnum, userId: String, targetId: String): Boolean {
        //构建
        //用户行为构建记录
        val heatBehaviorRecordMongo = HeatBehaviorRecordMongo()
        heatBehaviorRecordMongo.userId = userId.toLong()
        heatBehaviorRecordMongo.targetId = targetId.toLong()
        heatBehaviorRecordMongo.targetType = 1
        heatBehaviorRecordMongo.behaviorTypeId = actionEnum.id
        heatBehaviorRecordMongo.behaviorStatus = 1
        heatBehaviorRecordMongo.extInfo = mapOf(userId to actionEnum.description)
        heatBehaviorRecordMongo.updateTime = TimeUtil.getDateTime(LocalDateTime.now())
        heatBehaviorRecordMongo.createTime = TimeUtil.getDateTime(LocalDateTime.now())
        try {
            // mongo插入
            mongoUtil.save(heatBehaviorRecordMongo, "user_behavior_record")
            return true
        } catch (e: Exception) {
            log.error(e.message)
            return false
        }
    }

}