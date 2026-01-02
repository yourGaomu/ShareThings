package com.zhangzc.sharethingnotifyimpl.service.impl

import com.baomidou.mybatisplus.extension.service.IService
import com.fasterxml.jackson.core.type.TypeReference
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext
import com.zhangzc.redisspringbootstart.utills.RedisUtil
import com.zhangzc.sharethingnotifyimpl.consts.RedisNotifyConstants
import com.zhangzc.sharethingnotifyimpl.pojo.domain.FsNotify
import com.zhangzc.sharethingnotifyimpl.pojo.domain.FsNotifyBroadcastState
import com.zhangzc.sharethingnotifyimpl.pojo.domain.FsNotifyUser
import com.zhangzc.sharethingnotifyimpl.pojo.mongo.Notification
import com.zhangzc.sharethingnotifyimpl.pojo.vo.NotifySearchVo
import com.zhangzc.sharethingnotifyimpl.service.FsNotifyBroadcastStateService
import com.zhangzc.sharethingnotifyimpl.service.FsNotifyService
import com.zhangzc.sharethingnotifyimpl.service.FsNotifyUserService
import com.zhangzc.sharethingnotifyimpl.service.NotifyService
import com.zhangzc.sharethingscommon.utils.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 系统通知服务实现类
 * 
 * 使用 MySQL 表：
 * - fs_notify                  系统通知主表
 * - fs_notify_user             用户-通知关系表（定向通知）
 * - fs_notify_broadcast_state  用户广播通知阅读游标表
 */
@Service
class NotifyServiceImpl(
    private val fsNotifyService: FsNotifyService,
    private val fsNotifyUserService: FsNotifyUserService,
    private val fsNotifyBroadcastStateService: FsNotifyBroadcastStateService,
    private val redisUtil: RedisUtil,
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor
) : NotifyService {

    private val log = LoggerFactory.getLogger(NotifyServiceImpl::class.java)

    /**
     * 分页查询当前用户的系统通知（广播 + 定向），按时间倒序
     */
    override fun getList(notifySearchDTO: NotifySearchVo): PageResponse<Notification> {
        // 获取当前登录用户ID
        val userIdStr = GlobalContext.get()?.toString()
            ?: throw RuntimeException("用户未登录")
        val userId = userIdStr.toLong()

        val current = (notifySearchDTO.current ?: 1).let { if (it < 1) 1 else it }
        val size = (notifySearchDTO.size ?: 10).let { if (it < 1) 10 else it }

        // 构建缓存 key：Notify:System:List:{userId}:{type}:{isRead}:{current}:{size}
        val typePart = notifySearchDTO.type?.toString() ?: "all"
        val isReadPart = notifySearchDTO.isRead?.let { if (it) "1" else "0" } ?: "all"
        val cacheKey = buildSystemListCacheKey(userId, typePart, isReadPart, current, size)

        // 1. 先尝试从 Redis 读取缓存
        try {
            val cache = redisUtil.get(cacheKey)
            if (cache != null) {
                val cachedPage = JsonUtils.parseObject(
                    JsonUtils.toJsonString(cache),
                    object : TypeReference<PageResponse<Notification>>() {}
                )
                if (cachedPage != null) {
                    return cachedPage
                }
            }
        } catch (e: Exception) {
            log.error("读取系统通知列表缓存失败, key: {}", cacheKey, e)
        }

        val resultList = mutableListOf<Notification>()

        // 2. 处理广播类通知（scope_type = 0）
        val broadcastState: FsNotifyBroadcastState? = fsNotifyBroadcastStateService.getById(userId)
        val lastReadNotifyId = broadcastState?.lastReadNotifyId ?: 0L

        val broadcastQuery = fsNotifyService.lambdaQuery()
            .eq(FsNotify::getScopeType, 0)
            .eq(FsNotify::getNotifyState, 1)
            .eq(FsNotify::getIsDeleted, 0)

        // 按通知类型过滤（可选）
        notifySearchDTO.type?.let { type ->
            broadcastQuery.eq(FsNotify::getNotifyType, type)
        }

        val broadcastList = broadcastQuery
            .orderByDesc(FsNotify::getSendTime)
            .list()

        broadcastList?.forEach { notify ->
            val isRead = notify.id != null && notify.id!! <= lastReadNotifyId
            val dto = mapToNotification(notify, isRead, userId)
            resultList.add(dto)
        }

        // 3. 处理定向通知（scope_type != 0，对当前用户）
        val userNotifyQuery = fsNotifyUserService.lambdaQuery()
            .eq(FsNotifyUser::getReceiverUserId, userId)
            .eq(FsNotifyUser::getIsDeleted, 0)

        // 如果前端传了 isRead，则在关系表层面先做一次过滤
        notifySearchDTO.isRead?.let { read ->
            userNotifyQuery.eq(FsNotifyUser::getIsRead, if (read) 1 else 0)
        }

        val relations = userNotifyQuery.list()
        if (!relations.isNullOrEmpty()) {
            val notifyIds = relations.mapNotNull { it.notifyId }.toSet()
            if (notifyIds.isNotEmpty()) {
                val notifyList = fsNotifyService.listByIds(notifyIds)
                val notifyMap = notifyList.associateBy { it.id }

                relations.forEach { rel ->
                    val notify = notifyMap[rel.notifyId] ?: return@forEach

                    // 按通知类型过滤（可选）
                    if (notifySearchDTO.type != null && notify.notifyType != notifySearchDTO.type) {
                        return@forEach
                    }

                    val isRead = rel.isRead == 1
                    val dto = mapToNotification(notify, isRead, userId)
                    resultList.add(dto)
                }
            }
        }

        // 4. 按 isRead 再次统一过滤（广播 + 定向）
        val filteredList = notifySearchDTO.isRead?.let { readFlag ->
            resultList.filter { it.isRead == readFlag }
        } ?: resultList

        // 5. 按创建时间倒序排序
        val sortedList = filteredList.sortedByDescending { it.createTime }

        // 6. 手动分页
        val fromIndex = (current - 1) * size
        val toIndex = kotlin.math.min(fromIndex + size, sortedList.size)
        val pageRecords = if (fromIndex >= sortedList.size) {
            emptyList<Notification>()
        } else {
            sortedList.subList(fromIndex, toIndex)
        }

        val total = sortedList.size.toLong()
        val pageResponse = PageResponse.success(pageRecords, current.toLong(), total)

        // 7. 异步写入缓存并记录 key，方便后续失效
        try {
            CompletableFuture.runAsync( {
                try {
                    redisUtil.set(cacheKey, pageResponse, RedisNotifyConstants.NOTIFY_SYSTEM_LIST_TTL)
                    val keySetKey = buildSystemListKeySetKey(userId)
                    redisUtil.sSetAndTime(keySetKey, RedisNotifyConstants.NOTIFY_SYSTEM_LIST_TTL, cacheKey)
                } catch (e: Exception) {
                    log.error("写入系统通知列表缓存失败, key: {}", cacheKey, e)
                }
            },threadPoolTaskExecutor)
        } catch (e: Exception) {
            log.error("提交系统通知列表缓存异步任务失败, key: {}", cacheKey, e)
        }

        return pageResponse
    }

    /**
     * 获取用户未读通知数量（广播 + 定向），不区分类型
     */
    fun getUnreadCount(userId: Long): Long {
        // 广播未读
        val broadcastState: FsNotifyBroadcastState? = fsNotifyBroadcastStateService.getById(userId)
        val lastReadNotifyId = broadcastState?.lastReadNotifyId ?: 0L

        val broadcastUnread = fsNotifyService.lambdaQuery()
            .eq(FsNotify::getScopeType, 0)
            .eq(FsNotify::getNotifyState, 1)
            .eq(FsNotify::getIsDeleted, 0)
            .gt(FsNotify::getId, lastReadNotifyId)
            .count()

        // 定向未读
        val directUnread = fsNotifyUserService.lambdaQuery()
            .eq(FsNotifyUser::getReceiverUserId, userId)
            .eq(FsNotifyUser::getIsDeleted, 0)
            .eq(FsNotifyUser::getIsRead, 0)
            .count()

        return broadcastUnread + directUnread
    }

    /**
     * 标记单个通知为已读
     */
    fun markAsRead(notificationId: String, userId: Long): Boolean {
        val id = notificationId.toLongOrNull() ?: return false
        val notify = fsNotifyService.getById(id) ?: return false

        return try {
            val success = if (notify.scopeType == 0) {
                // 广播通知，更新游标
                var state = fsNotifyBroadcastStateService.getById(userId)
                val now = Date()
                if (state == null) {
                    state = FsNotifyBroadcastState()
                    state.userId = userId
                    state.lastReadNotifyId = id
                    state.createTime = now
                    state.updateTime = now
                } else {
                    if (state.lastReadNotifyId == null || id > state.lastReadNotifyId!!) {
                        state.lastReadNotifyId = id
                    }
                    state.updateTime = now
                }
                fsNotifyBroadcastStateService.saveOrUpdate(state)
            } else {
                // 定向通知，更新关系表
                val relation = fsNotifyUserService.lambdaQuery()
                    .eq(FsNotifyUser::getNotifyId, id)
                    .eq(FsNotifyUser::getReceiverUserId, userId)
                    .one() ?: return false

                if (relation.isRead == 1) {
                    true
                } else {
                    val now = Date()
                    relation.isRead = 1
                    relation.readTime = now
                    relation.updateTime = now
                    fsNotifyUserService.updateById(relation)
                }
            }

            if (success) {
                clearUserSystemListCache(userId)
            }
            success
        } catch (e: Exception) {
            log.error("标记通知为已读失败，notificationId: {}, userId: {}", notificationId, userId, e)
            false
        }
    }

    /**
     * 标记当前用户所有通知为已读
     */
    fun markAllAsRead(userId: Long): Boolean {
        return try {
            val now = Date()

            // 1. 更新广播游标为当前最大广播通知ID
            val latestBroadcast = fsNotifyService.lambdaQuery()
                .eq(FsNotify::getScopeType, 0)
                .eq(FsNotify::getNotifyState, 1)
                .eq(FsNotify::getIsDeleted, 0)
                .orderByDesc(FsNotify::getId)
                .last("LIMIT 1")
                .one()

            if (latestBroadcast != null && latestBroadcast.id != null) {
                var state = fsNotifyBroadcastStateService.getById(userId)
                if (state == null) {
                    state = FsNotifyBroadcastState()
                    state.userId = userId
                    state.createTime = now
                }
                state.lastReadNotifyId = latestBroadcast.id
                state.updateTime = now
                fsNotifyBroadcastStateService.saveOrUpdate(state)
            }

            // 2. 更新定向通知为已读
            val relations = fsNotifyUserService.lambdaQuery()
                .eq(FsNotifyUser::getReceiverUserId, userId)
                .eq(FsNotifyUser::getIsDeleted, 0)
                .eq(FsNotifyUser::getIsRead, 0)
                .list()

            if (!relations.isNullOrEmpty()) {
                relations.forEach { rel ->
                    rel.isRead = 1
                    rel.readTime = now
                    rel.updateTime = now
                }
                (fsNotifyUserService as IService<FsNotifyUser>).updateBatchById(relations)
            }

            clearUserSystemListCache(userId)

            true
        } catch (e: Exception) {
            log.error("标记所有通知为已读失败，userId: {}", userId, e)
            false
        }
    }

    /**
     * 创建新系统通知
     * - 如果 notification.receiverUserId 为空，则视为广播通知
     * - 否则视为定向通知，并在 fs_notify_user 中插入关系
     */
    fun createNotification(notification: Notification): Notification {
        val now = Date()

        val notify = FsNotify()
        // 这里因为 Notification 没有标题字段，暂时用 message 作为标题占位
        notify.title = notification.getProjectName() ?: "系统通知"
        notify.content = notification.message
        notify.notifyType = notification.type
        notify.scopeType = if (notification.receiverUserId == null) 0 else 1
        notify.bizType = null
        notify.bizId = null
        notify.sendTime = now
        notify.creatorId = notification.createUser
        notify.creatorName = notification.createUserName
        notify.notifyState = 1
        notify.isDeleted = 0
        notify.createTime = now
        notify.updateTime = now

        fsNotifyService.save(notify)

        // 如果是定向通知，写入用户关系表
        if (notify.scopeType != null && notify.scopeType != 0 && notification.receiverUserId != null) {
            val relation = FsNotifyUser()
            relation.notifyId = notify.id
            relation.receiverUserId = notification.receiverUserId
            relation.isRead = 0
            relation.isDeleted = 0
            relation.createTime = now
            relation.updateTime = now
            fsNotifyUserService.save(relation)
        }

        // 回填 Notification DTO
        notification.id = notify.id?.toString()
        notification.isDeleted = false
        notification.isRead = false
        notification.createTime = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        notification.updateTime = notification.createTime

        // 通知创建后清理相关用户的列表缓存（仅定向通知）
        if (notify.scopeType != null && notify.scopeType != 0 && notification.receiverUserId != null) {
            clearUserSystemListCache(notification.receiverUserId!!)
        }

        log.info("创建系统通知成功，notifyId: {}", notify.id)
        return notification
    }

    /**
     * 构建系统通知列表缓存 key
     */
    private fun buildSystemListCacheKey(
        userId: Long,
        typePart: String,
        isReadPart: String,
        current: Int,
        size: Int
    ): String {
        return RedisNotifyConstants.NOTIFY_SYSTEM_LIST_PREFIX +
            "$userId:$typePart:$isReadPart:$current:$size"
    }

    /**
     * 构建系统通知列表 key 集合的 key
     */
    private fun buildSystemListKeySetKey(userId: Long): String {
        return RedisNotifyConstants.NOTIFY_SYSTEM_LIST_KEY_SET_PREFIX + userId
    }

    /**
     * 清理某个用户的系统通知列表缓存
     */
    private fun clearUserSystemListCache(userId: Long) {
        val keySetKey = buildSystemListKeySetKey(userId)
        try {
            val keys = redisUtil.sGet(keySetKey)
            if (keys != null && keys.isNotEmpty()) {
                val keyArray = keys.map { it.toString() }.toTypedArray()
                redisUtil.del(*keyArray)
            }
            redisUtil.del(keySetKey)
        } catch (e: Exception) {
            log.error("清理用户系统通知列表缓存失败, userId: {}", userId, e)
        }
    }

    /**
     * 将 FsNotify 转换为对外返回的 Notification DTO
     */
    private fun mapToNotification(notify: FsNotify, isRead: Boolean, receiverUserId: Long): Notification {
        val dto = Notification()
        dto.id = notify.id?.toString()
        dto.message = notify.content
        dto.type = notify.notifyType
        dto.isRead = isRead
        dto.isDeleted = notify.isDeleted == 1
        dto.createUser = notify.creatorId
        dto.createUserName = notify.creatorName
        dto.receiverUserId = receiverUserId

        // 使用 sendTime/createTime 转换为 LocalDateTime
        val createDate: Date? = notify.sendTime ?: notify.createTime
        val updateDate: Date? = notify.updateTime ?: notify.sendTime ?: notify.createTime

        dto.createTime = createDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
        dto.updateTime = updateDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

        return dto
    }
}
