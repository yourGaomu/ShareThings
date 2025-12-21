package com.zhangzc.sharethinguserimpl.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext
import com.zhangzc.redisspringbootstart.utills.RedisUtil
import com.zhangzc.sharethingcountapi.rpc.likeCount
import com.zhangzc.sharethingcountapi.rpc.pvCount
import com.zhangzc.sharethinguserimpl.consts.RedisConstants
import com.zhangzc.sharethinguserimpl.pojo.domain.FsPermissions
import com.zhangzc.sharethinguserimpl.pojo.domain.FsRole
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserInfo
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserLevel
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserRole
import com.zhangzc.sharethinguserimpl.pojo.vo.PermissionsSsoDTO
import com.zhangzc.sharethinguserimpl.pojo.vo.RoleSsoDTO
import com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO
import com.zhangzc.sharethinguserimpl.pojo.vo.UserRightsDTO
import com.zhangzc.sharethinguserimpl.service.FsPermissionsService
import com.zhangzc.sharethinguserimpl.service.FsRolePermissionsService
import com.zhangzc.sharethinguserimpl.service.FsRoleService
import com.zhangzc.sharethinguserimpl.service.FsUserInfoService
import com.zhangzc.sharethinguserimpl.service.FsUserLevelService
import com.zhangzc.sharethinguserimpl.service.FsUserRoleService
import com.zhangzc.sharethinguserimpl.service.UserService
import lombok.extern.slf4j.Slf4j
import org.apache.dubbo.config.annotation.DubboReference
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture


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
    @DubboReference(check = false, timeout = 5000)
    private val likeCountImpl: likeCount,
    @DubboReference(check = false, timeout = 5000)
    private val pvCountImpl: pvCount,
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
            .associate { (roleId, result) ->
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
        result.id = userInfo.userId.toLong()
        result.name = userInfo.nickname
        result.gender = userInfo.sex
        result.intro = userInfo.introduction
        BeanUtils.copyProperties(userInfo, result)
        //查询用户的被点赞数量
        likeCountImpl.getLikeCountByUserIds(listOf(userId)).forEach { (_, v) ->
            result.likeCount = v.toLong()
        }
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
}