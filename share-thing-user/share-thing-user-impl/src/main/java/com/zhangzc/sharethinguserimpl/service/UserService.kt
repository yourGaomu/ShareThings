package com.zhangzc.sharethinguserimpl.service

import com.zhangzc.sharethingscommon.utils.PageResponse
import com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO
import com.zhangzc.sharethinguserimpl.pojo.vo.UserRightsDTO
import com.zhangzc.sharethinguserimpl.pojo.vo.UserSearchDTO

interface UserService {
    // 用 fun 声明抽象方法，无方法体，由实现类重写
    fun getCurrentUserRights(): UserRightsDTO
    fun getUserInfo(userId: String): com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO
    fun getHotAuthorsList(userSearchDTO: com.zhangzc.sharethinguserimpl.pojo.vo.UserSearchDTO): com.zhangzc.sharethingscommon.utils.PageResponse<com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO>
    fun likeArticle(articleId: String, userId: String)
}