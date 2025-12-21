// com/zhangzc/sharethinguserimpl/consts/RedisConstants.kt
package com.zhangzc.sharethinguserimpl.consts

/**
 * Redis常量管理类
 * 统一管理Redis键前缀、过期时间等常量，便于维护
 */
object RedisConstants {
    const val RedisUserRole = "UserRoleMap"
    const val RedisRoleInfo = "RoleInfoMap"
    const val RedisRolePermissions = "RolePermissionsMap"
    const val RedisPermissionsInfo = "PermissionsInfoMap"

}