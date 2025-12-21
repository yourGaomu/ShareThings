package com.zhangzc.sharethinguserimpl.pojo.vo

data class UserRightsDTO(
    // 主构造函数参数（data class 必须有至少一个）
     var userId: Long? = null,
     var userName: String? = null,
     var roles: MutableList<RoleSsoDTO?>? = null
)