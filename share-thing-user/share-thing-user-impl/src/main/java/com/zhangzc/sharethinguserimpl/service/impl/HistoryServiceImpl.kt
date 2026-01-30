package com.zhangzc.sharethinguserimpl.service.impl

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.metadata.IPage
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil
import com.zhangzc.redisspringbootstart.utills.LuaUtil
import com.zhangzc.redisspringbootstart.utills.RedisUtil
import com.zhangzc.sharethingarticleapi.rpc.ArticleRpc
import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO
import com.zhangzc.sharethingscommon.utils.TimeUtil
import com.zhangzc.sharethinguserimpl.consts.RedisConstants
import com.zhangzc.sharethinguserimpl.pojo.domain.BbsBrowsingHistory
import com.zhangzc.sharethinguserimpl.pojo.req.ArticleQueryRequestDto
import com.zhangzc.sharethinguserimpl.service.BbsBrowsingHistoryService
import com.zhangzc.sharethinguserimpl.service.HistoryService
import org.apache.dubbo.config.annotation.DubboReference
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
class HistoryServiceImpl(
    private val mongoUtil: MongoUtil,
    private val redisUtil: RedisUtil,
    private val luaUtil: LuaUtil,
    @DubboReference(check = false, timeout = 5000)
    private val articleRpc: ArticleRpc,
    private val bbsBrowsingHistoryService: BbsBrowsingHistoryService
) : HistoryService {

    override fun getHistory(articleQueryRequestDto: ArticleQueryRequestDto?): List<ArticleDTO> {
        //查询用户的浏览记录
        //去mongo查询
        val currentPage: Int = articleQueryRequestDto?.currentPage ?: 1
        val pageSize: Int = articleQueryRequestDto?.pageSize ?: 10
        val userId = GlobalContext.get()?.toString() ?: throw RuntimeException("用户未登录")

        //去redis里面查询
        val redisKey = RedisConstants.RedisUserHistory + userId
        val data: MutableList<Any> = mutableListOf(currentPage, pageSize)
        var execute = luaUtil.execute("get_user_view_history", redisKey, data)
        //返回的数据是{id,时间戳}
        val result: List<ArticleDTO> = execute?.let {
            //返回的数据不为空
            var parseObject = JsonUtils.parseObject(execute.toString(), Map::class.java)
            //判断是否是空list
            if (!parseObject.isNotEmpty()) {
                //数据为空，应该返回空记录
                return emptyList()
            } else {
                //有数据存在
                //根据文章id去搜索文章信息
                val articleDtoByArticleIds =
                    articleRpc.getArticleDtoByArticleIds(parseObject.keys.map { it.toString() })
                return articleDtoByArticleIds
            }
        } ?: run {
            //为空，需要去查询
            var page = Page.of<BbsBrowsingHistory>(currentPage.toLong(), pageSize.toLong())
            val eq = LambdaQueryWrapper<BbsBrowsingHistory>()
                .eq(BbsBrowsingHistory::getUserId, userId)
                .eq(BbsBrowsingHistory::getIsDeleted, 0)
            val pageResult = bbsBrowsingHistoryService.page(page, eq)
            CompletableFuture.runAsync {
                //存入redis里面
                if (pageResult.size > 0){
                    redisUtil.zAdd(
                        redisKey,
                        pageResult.records.associate { it.articleId.toString() to it.createTime.time.toDouble() })
                }
            }
            //构建结果
            val dto = pageResult.records.map {
                val dto = ArticleDTO()
                dto.createTime = TimeUtil.getLocalDateTime(it.createTime)
                dto.updateTime = TimeUtil.getLocalDateTime(it.updateTime)
                dto.id = it.articleId.toInt()
                BeanUtils.copyProperties(it, dto)
                dto
            }.toList()
            dto
        }
        return result
    }

    override fun clearHistory(id: String?): Boolean {
        val userId = GlobalContext.get()?.toString() ?: throw RuntimeException("用户未登录")
        val update = bbsBrowsingHistoryService.lambdaUpdate()
            .eq(BbsBrowsingHistory::getUserId, userId)
            .set(BbsBrowsingHistory::getIsDeleted, 1)
            .set(BbsBrowsingHistory::getUpdateTime,TimeUtil.getDateTime(LocalDateTime.now()))
            .update()
        return update
    }

    override fun clearAllHistory(): Boolean {
        val userId = GlobalContext.get()?.toString() ?: throw RuntimeException("用户未登录")
        val update = bbsBrowsingHistoryService.lambdaUpdate()
            .eq(BbsBrowsingHistory::getIsDeleted, 0)
            .eq(BbsBrowsingHistory::getUserId, userId)
            .set(BbsBrowsingHistory::getIsDeleted, 1)
            .set(BbsBrowsingHistory::getUpdateTime,TimeUtil.getDateTime(LocalDateTime.now()))
            .update()
        return update
    }
}