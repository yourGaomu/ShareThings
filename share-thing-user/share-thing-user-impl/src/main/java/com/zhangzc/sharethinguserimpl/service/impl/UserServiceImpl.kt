package com.zhangzc.sharethinguserimpl.service.impl

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.fasterxml.jackson.core.type.TypeReference
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext
import com.zhangzc.redisspringbootstart.utills.RedisUtil
import com.zhangzc.sharethingarticleapi.rpc.ArticleRpc
import com.zhangzc.sharethingcountapi.rpc.likeCount
import com.zhangzc.sharethingcountapi.rpc.pvCount
import com.zhangzc.sharethingscommon.exception.BusinessException
import com.zhangzc.sharethingscommon.utils.PageResponse
import com.zhangzc.sharethingscommon.utils.TimeUtil
import com.zhangzc.sharethinguserimpl.Enum.ResponseCodeEnum
import com.zhangzc.sharethinguserimpl.consts.RedisConstants
import com.zhangzc.sharethinguserimpl.pojo.domain.*
import com.zhangzc.sharethinguserimpl.pojo.vo.*
import com.zhangzc.sharethinguserimpl.service.*
import lombok.extern.slf4j.Slf4j
import org.apache.dubbo.config.annotation.DubboReference
import org.springframework.beans.BeanUtils
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor

@Service
@Slf4j
class UserServiceImpl(
    private val fsPermissionsService: FsPermissionsService,
    private val fsUserRoleService: FsUserRoleService,
    private val fsRolePermissionsService: FsRolePermissionsService,
    private val fsRoleService: FsRoleService,
    private val FsUserLevelService: FsUserLevelService,
    private val fsUserInfoService: FsUserInfoService,
    private val redisUtil: RedisUtil,
    private val authorHeatMonthServiceImpl: AuthorHeatMonthServiceImpl,
    private val authorHeatWeekServiceImpl: AuthorHeatWeekServiceImpl,
    private val authorHeatDayServiceImpl: AuthorHeatDayServiceImpl,
    private val authorHeatBehaviorServiceImpl: AuthorHeatBehaviorServiceImpl,
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor,
    @DubboReference(check = false, timeout = 5000)
    private val articleRpc: ArticleRpc,
    @DubboReference(check = false, timeout = 5000)
    val likeCountImpl: likeCount,
    @DubboReference(check = false, timeout = 5000)
    val pvCountImpl: pvCount,
    val RANK_TOP_LIMIT: Int = 100 // 只缓存前100名作者
) : UserService {
    override fun getCurrentUserRights(): UserRightsDTO {
        //获取当前的用户操作id
        //用户id->角色id->权限id
        // 核心：先强转成 Long?（可空），失败则为 null，再抛异常
        val userId = GlobalContext.get()?.toString() ?: throw RuntimeException("用户未登录")
        //查询用户的信息
        val userInfo: FsUserInfo = redisUtil.hmget("userInfo", userId)?.let {
            //查询到了数据就会进行序列化
            JsonUtils.parseObject(JsonUtils.toJsonString(it), FsUserInfo::class.java)
        } ?: run {
            //Redis无数据
            //redis查询不到数据
            val userInfo = fsUserInfoService.lambdaQuery()
                .eq(FsUserInfo::getUserId, userId)
                .one()
            //开启异步线程去存起来
            CompletableFuture.runAsync {
                redisUtil.hmsetGeneric("userInfo", mapOf(userId to userInfo))
            }
            userInfo
        }

        //查询当前用户的所有角色
        val redisRoleKey: String = RedisConstants.RedisUserRole
        //从这个里面查询到
        // 从Redis获取数据并解析
        val roleIds: List<String> = redisUtil.hmget(redisRoleKey, userId)?.let { result ->
            JsonUtils.parseList(JsonUtils.toJsonString(result), object : TypeReference<List<String>>() {})
        } ?: run {
            val roleIds = fsUserRoleService.lambdaQuery()
                .eq(FsUserRole::getUserId, userId)
                .list()
                .map { it.roleId.toString() }
            //放入redis数据库
            CompletableFuture.runAsync {
                redisUtil.hmsetGeneric(redisRoleKey, mapOf(userId.toString() to roleIds))
            }
            roleIds
        }

        //根据角色id查询对应的具体的角色信息
        val roleMap: Map<String, FsRole> = redisUtil.hmget(RedisConstants.RedisRoleInfo, roleIds)
            .toMutableList()
            .filterNotNull()
            .takeIf { it.isNotEmpty() } // 只有非空列表才继续，空列表直接走数据库
            ?.let { result ->
                JsonUtils.parseObject(JsonUtils.toJsonString(result), object : TypeReference<List<FsRole>>() {})
                    .associateBy { it.id.toString() }
            } ?: run {
            //查询数据库
            val associateBy: Map<String, FsRole> = fsRoleService.lambdaQuery()
                .eq(FsRole::getIsDeleted, 0)
                .eq(FsRole::getState, 1)
                .`in`(FsRole::getId, roleIds)
                .list()
                .associateBy { it.id.toString() }
            CompletableFuture.runAsync {
                //存入redis
                redisUtil.hmsetGeneric(RedisConstants.RedisRoleInfo, associateBy)
            }
            associateBy
        }

        //根据角色id查询权限id
        val redisRolePermissions = RedisConstants.RedisRolePermissions
        val redisResults = redisUtil.hmget(redisRolePermissions, roleIds)
        val rolePermissionMap: Map<String, List<String>> = roleIds
            .zip(redisResults)
            .filter { (_, result) -> result != null }
            .takeIf { it.size == roleIds.size }
            ?.associate { (roleId, result) ->
                try {
                    // 解析权限列表，解析失败则返回空列表
                    val permissions = JsonUtils.parseObject(
                        JsonUtils.toJsonString(result),
                        object : TypeReference<List<String>>() {}
                    ) ?: emptyList()
                    roleId to permissions
                } catch (e: Exception) {
                    // 记录日志：哪个角色ID解析失败，失败原因
                    // 解析失败时，该角色权限设为空列表
                    println("转换失败了")
                    roleId to emptyList()
                }
            } ?: run {
            //查询数据库
            //获取这些角色对应的权限id
            val mapValues: Map<String, List<String>> = fsRolePermissionsService.lambdaQuery()
                .`in`(FsRolePermissions::getRoleId, roleIds)
                .list()
                .groupBy { it.roleId.toString() }
                .mapValues { it.value.map { it.permissionsId.toString() } }
            //存入redis
            CompletableFuture.runAsync {
                redisUtil.hmsetGeneric(redisRolePermissions, mapValues)
            }
            mapValues
        }


        //获取权限id集合
        val toSet = rolePermissionMap.values.flatten().toSet()

        //查询权限信息
        val redisPermissionsInfo = RedisConstants.RedisPermissionsInfo
        val permissionsMap: Map<String, FsPermissions> =
            redisUtil.hmget(redisPermissionsInfo, toSet)
                .filterNotNull()
                .takeIf { it.isNotEmpty() }
                ?.let { result ->
                    JsonUtils.parseObject(
                        JsonUtils.toJsonString(result),
                        object : TypeReference<List<FsPermissions>>() {})
                        .associateBy { it.id.toString() }
                } ?: run {
                //去查询数据库
                val associateBy = fsPermissionsService.lambdaQuery()
                    .`in`(FsPermissions::getId, toSet)
                    .list()
                    .associateBy { it.id.toString() }
                CompletableFuture.runAsync {
                    redisUtil.hmsetGeneric(redisPermissionsInfo, associateBy)
                }
                associateBy
            }

        //开始组装结果
        //构建权限list
        val permissionsSsoDTOs: MutableList<PermissionsSsoDTO?> = permissionsMap.values
            .map { permission ->
                PermissionsSsoDTO(
                    id = permission.id,
                    desc = permission.desc,
                    api = permission.api
                )
            }.toMutableList()

        //构建角色
        val toMutableList: MutableList<RoleSsoDTO?> = roleMap.values.map {
            RoleSsoDTO(
                id = it.id,
                code = it.code,
                name = it.name,
                grade = it.grade.toString(),
                permissions = permissionsSsoDTOs
            )
        }.toMutableList()

        //构建结果
        val result = UserRightsDTO()
        result.userId = userId.toLong()
        result.userName = userInfo.nickname
        result.roles = toMutableList
        return result
    }

    override fun getUserInfo(userId: String): UserForumDTO {
        //获取当前的用户id
        val result = UserForumDTO()
        val currentUserId = GlobalContext.get().toString()
        //查询用户信息
        val userInfo = redisUtil.hmget("userInfo", userId)?.let {
            JsonUtils.parseObject(JsonUtils.toJsonString(it), FsUserInfo::class.java)
        } ?: run {
            val userInfo = fsUserInfoService.lambdaQuery()
                .eq(FsUserInfo::getUserId, userId)
                .one()
            CompletableFuture.runAsync {
                redisUtil.hmset("userInfo", mapOf(userId to userInfo))
            }
            userInfo
        }

        result.name = userInfo.nickname
        result.gender = userInfo.sex
        result.intro = userInfo.introduction
        result.picture = userInfo.avatar
        result.createTime = TimeUtil.getLocalDateTime(userInfo.createTime)
        result.updateTime = TimeUtil.getLocalDateTime(userInfo.updateTime)
        BeanUtils.copyProperties(userInfo, result)
        result.id = userInfo.userId.toLong()
        //查询用户的被点赞数量
        likeCountImpl.getLikeCountByUserIds(listOf(userId)).forEach { (_, v) ->
            result.likeCount = v.toLong()
        }
        //查询用户的角色

        //查询他的阅读量
        pvCountImpl.getPVCountByUserIds(listOf(userId)).forEach { (_, v) ->
            result.readCount = v.toLong()
        }
        //查询他的积分与等级
        val levelInfo = redisUtil.hmget("User:Level:Info", userId)?.let {
            JsonUtils.parseObject(JsonUtils.toJsonString(it), FsUserLevel::class.java)
        } ?: run {
            val userLevel = FsUserLevelService.lambdaQuery()
                .eq(FsUserLevel::getUserId, userId)
                .one()
            CompletableFuture.runAsync {
                redisUtil.hmset("User:Level:Info", mapOf(userId to userLevel))
            }
            userLevel
        }
        result.points = levelInfo.points
        result.level = levelInfo.level
        return result
    }

    /**
     * 热度榜只会缓存前100名
     * */
    override fun getHotAuthorsList(userSearchDTO: UserSearchDTO): PageResponse<UserForumDTO> {
        //先从redis里面查询排行榜
        val hotAuthorsZSet = RedisConstants.HotAuthorsZSet
        //获取需要查询的排行榜的类型
        val authorHeatSize = userSearchDTO.authorHeatSize
        //拼接排行榜的类型
        val redisKey = "$hotAuthorsZSet$authorHeatSize"
        // 计算 Redis ZSet 分页的起始和结束偏移量（从 0 开始）
        // 处理分页参数的边界情况：避免 currentPage 小于 1 或 pageSize 小于 1
        val currentPage = userSearchDTO.currentPage?.let { if (it < 1) 1 else userSearchDTO.currentPage } ?: run { 1 }
        val pageSize =
            userSearchDTO.pageSize?.let { if (it < 1) 10 else userSearchDTO.pageSize } ?: run { 10 } // 默认每页10条
        // 核心计算逻辑,全部转换为Long类型
        val redisTotal = redisUtil.zCard(redisKey)
        val start = pageSize.let { currentPage.let { it1 -> (it1 - 1) * it } }.toLong() // 起始偏移量（包含）
        val end = pageSize.let { start.let { it1 -> it1 + it - 1 } }           // 结束偏移量（包含）
        val actualTotal = minOf(redisTotal, RANK_TOP_LIMIT.toLong()) // 实际有效数据量（最多100）
        // 超出前100名范围，直接返回后10名
        if (start >= actualTotal) {

            val zReverseRange = redisUtil.zReverseRange(redisKey, actualTotal - 10, actualTotal - 1)
                .filter { it != null }
                .takeIf { it.isNotEmpty() }
                ?: run {
                    //榜单没有任何数据，返回空参
                    return PageResponse.success(emptyList(), currentPage.toLong(), 0)
                }
            //序列化
            try {
                val toList = JsonUtils.parseObject(
                    JsonUtils.toJsonString(zReverseRange),
                    object : TypeReference<List<String>>() {})
                    .map {
                        val userInfo = getUserInfo(it)
                        userInfo
                    }.toList()
                return PageResponse.success(toList, currentPage.toLong(), actualTotal)
            } catch (e: Exception) {
                println("序列化失败 ${e.message}")
                return PageResponse.success(emptyList(), currentPage.toLong(), 0)
            }

        }
        // 调用 Redis 工具类获取分页数据（倒序取）
        val rankUserIds = redisUtil.zReverseRange(redisKey, start, end)
            .filter { score ->
                score != null
            }
            .takeIf {
                it.isNotEmpty()
            }
            ?.let {
                //不为空需要进行转换
                try {
                    val userIds =
                        JsonUtils.parseList(JsonUtils.toJsonString(it), object : TypeReference<List<Long>>() {})
                    userIds
                } catch (e: Exception) {
                    println("序列化失败 ${e.message}")
                    null
                }
            }
            ?: run {
                //如果为空，说明这个排行榜没有数据
                //去数据库查询
                when (authorHeatSize) {
                    1 -> {
                        //去数据库查询日榜单
                        makeHotAuthorsListDay()
                    }

                    2 -> {
                        //去数据库查询周榜单
                        makeHotAuthorsListWeek()
                    }

                    3 -> {
                        //去数据库查询月榜单
                        makeHotAuthorsMonthYear()
                    }

                    else -> {
                        throw BusinessException(ResponseCodeEnum.RANK_ERROR)
                    }
                }
            }

        if (rankUserIds == null || rankUserIds.isEmpty()) {
            //这个排行榜还没有人
            return PageResponse.success(emptyList(), currentPage.toLong(), 0)
        }
        //这个榜单存在
        val result = mutableListOf<UserForumDTO>()
        rankUserIds.forEach {
            val userInfo = getUserInfo(it.toString())
            result.add(userInfo)
        }
        return PageResponse.success(result, currentPage.toLong(), actualTotal)
    }

    private fun makeHotAuthorsListWeek(): List<Long> {
        //查询周榜单
        val page = Page.of<AuthorHeatWeek>(1, 100)
        val orderByDesc = LambdaQueryWrapper<AuthorHeatWeek>()
            .eq(AuthorHeatWeek::getStatWeek, calculateStatWeek())
            .orderByDesc(AuthorHeatWeek::getHeatValue)
            .last("LIMIT 100")
        val longs = (authorHeatWeekServiceImpl.page(page, orderByDesc).records
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.map { it.authorId }
            ?: run { return emptyList() })
        return longs
    }

    private fun makeHotAuthorsListDay(): List<Long> {
        //查询日榜单
        val page = Page.of<AuthorHeatDay>(1, 100)
        val orderByDesc = LambdaQueryWrapper<AuthorHeatDay>()
            .eq(AuthorHeatDay::getStatDate, LocalDate.now())
            .orderByDesc(AuthorHeatDay::getHeatValue)
            .last("LIMIT 100")
        val longs = (authorHeatDayServiceImpl.page(page, orderByDesc).records
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.map { it.authorId }
            ?: run { return emptyList() })
        return longs
    }

    private fun makeHotAuthorsMonthYear(): List<Long> {
        //查询年榜单
        val page = Page.of<AuthorHeatMonth>(1, 100)
        val orderByDesc = LambdaQueryWrapper<AuthorHeatMonth>()
            .eq(AuthorHeatMonth::getStatMonth, calculateStatMonth())
            .orderByDesc(AuthorHeatMonth::getHeatValue)
            .last("LIMIT 100")
        val longs = (authorHeatMonthServiceImpl.page(page, orderByDesc).records
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.map { it.authorId }
            ?: run { return emptyList() })
        return longs
    }


    fun calculateStatWeek(date: LocalDate = LocalDate.now()): String {
        // 1. 定义周的计算规则：ISO 8601标准（周一为一周起始，第一周至少4天）
        val weekFields = WeekFields.of(Locale.CHINA) // 也可直接用 WeekFields.ISO

        // 2. 获取年份（注意：跨年周的年份可能和日期年份不同，比如2026-01-01可能属于2025-W53）
        val weekYear = date.get(weekFields.weekBasedYear())

        // 3. 获取周数（1-53），并补两位（比如5周→05）
        val weekNumber = date.get(weekFields.weekOfWeekBasedYear())
        val formattedWeek = String.format("%02d", weekNumber) // 补两位，确保是01-53

        // 4. 拼接成指定格式
        return "${weekYear}-W${formattedWeek}"
    }

    /**
     * 根据日期计算 statMonth（格式：2025-12）
     * @param date 要计算的日期（默认当前日期）
     * @return 格式化的月份标识，如 "2025-12"
     */
    fun calculateStatMonth(date: LocalDate = LocalDate.now()): String {
        // 方式1：手动拼接（直观，新手易理解）
        /*       val year = date.year
               val month = date.monthValue // 获取1-12的月份数字
               val formattedMonth = String.format("%02d", month) // 补两位，确保01-12
               return "${year}-${formattedMonth}"*/
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        return date.format(formatter)
    }

     override fun likeArticle(articleId: String, userId: String) {
        //发送Rpc请求
        CompletableFuture.runAsync({
            val userIdsByArticleIds =
                articleRpc.getUserIdsByArticleIds(listOf<String>(articleId))
            //获取作者id
            val authorId = userIdsByArticleIds.get(articleId)
            likeCountImpl.likeArticleByUserId(articleId, userId, authorId)
        }, threadPoolTaskExecutor)
    }


}