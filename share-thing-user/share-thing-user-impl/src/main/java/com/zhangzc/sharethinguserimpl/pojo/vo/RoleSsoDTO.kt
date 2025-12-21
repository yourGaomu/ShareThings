package com.zhangzc.sharethinguserimpl.pojo.vo

data class RoleSsoDTO(
     var id: Int? = null,
     var code: String? = null,
     var name: String? = null,
     var grade: String? = null ,
     var permissions: MutableList<PermissionsSsoDTO?>? = null
) {

}