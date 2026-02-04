package com.zhangzc.sharethingarticleimpl.server.impl

import com.zhangzc.redisspringbootstart.utills.RedisUtil
import com.zhangzc.sharethingarticleimpl.pojo.domain.ForumNodes
import com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO
import com.zhangzc.sharethingarticleimpl.server.ForumNodesService
import com.zhangzc.sharethingarticleimpl.server.NodeService
import com.zhangzc.sharethingscommon.exception.BusinessException
import com.zhangzc.sharethingscommon.utils.TimeUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import org.springframework.beans.BeanUtils
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils
import com.fasterxml.jackson.core.type.TypeReference


@Service
class NodeServiceImpl (
    private val forumNodesService: ForumNodesService,
    private val redisUtil: RedisUtil
) : NodeService{
    private val log = LoggerFactory.getLogger(NodeServiceImpl::class.java)

    override fun creatNode(nodeDTO: NodeDTO, userId: String): Boolean {
        //判断文件夹类型
        var result: Boolean = false
        // 处理 parentId：如果为 0，则设为 null，避免外键约束错误
        val parentIdToSave = if (nodeDTO.parentId == 0L) null else nodeDTO.parentId

        when(nodeDTO.nodeType){
            NodeDTO.NodeType.FOLDER ->{
                //文件夹类型
                var forumNodes = ForumNodes()
                forumNodes.nodeType = NodeDTO.NodeType.FOLDER
                forumNodes.userId = userId
                forumNodes.name = nodeDTO.name
                forumNodes.parentId = parentIdToSave
                forumNodes.articleId = null
                forumNodes.sortOrder = 1
                forumNodes.isDeleted = 0
                forumNodes.createdAt= TimeUtil.getDateTime(LocalDateTime.now())
                forumNodes.updatedAt= TimeUtil.getDateTime(LocalDateTime.now())
                result = forumNodesService.save(forumNodes)
                // 清除列表缓存
                val cacheParentId = nodeDTO.parentId ?: 0L
                redisUtil.del("node:list:$userId:$cacheParentId")
            }
            NodeDTO.NodeType.ARTICLE ->{
                //文章类型
                var forumNodes = ForumNodes()
                forumNodes.nodeType = NodeDTO.NodeType.ARTICLE
                
                forumNodes.userId = userId
                forumNodes.name = nodeDTO.name
                forumNodes.parentId = parentIdToSave
                forumNodes.articleId = nodeDTO.articleId
                forumNodes.sortOrder = 1
                forumNodes.isDeleted = 0
                forumNodes.createdAt= TimeUtil.getDateTime(LocalDateTime.now())
                forumNodes.updatedAt= TimeUtil.getDateTime(LocalDateTime.now())
                result = forumNodesService.save(forumNodes)
                // 清除列表缓存
                val cacheParentId = nodeDTO.parentId ?: 0L
                redisUtil.del("node:list:$userId:$cacheParentId")
            }
            else -> {
                log.error("用户操作异常")
                throw BusinessException("500", "节点类型错误")
            }
        }
    return result
    }

    override fun deleteNode(nodeId: Long): Boolean {
        val node = forumNodesService.getById(nodeId) ?: return false
        val update = forumNodesService.lambdaUpdate()
            .eq(ForumNodes::getId, nodeId)
            .set(ForumNodes::getIsDeleted, 1)
            .update()
        
        if (update) {
            val parentId = node.parentId ?: 0L
            redisUtil.del("node:list:${node.userId}:$parentId")
        }
        return update
    }

    override fun updateNode(nodeDTO: NodeDTO): Boolean {
        if (nodeDTO.id == null) return false
        val node = forumNodesService.getById(nodeDTO.id) ?: return false

        val updateWrapper = forumNodesService.lambdaUpdate()
            .eq(ForumNodes::getId, nodeDTO.id)
            .set(ForumNodes::getUpdatedAt, TimeUtil.getDateTime(LocalDateTime.now()))

        if (nodeDTO.name != null) updateWrapper.set(ForumNodes::getName, nodeDTO.name)
        if (nodeDTO.sortOrder != null) updateWrapper.set(ForumNodes::getSortOrder, nodeDTO.sortOrder)
        
        val result = updateWrapper.update()
        
        if (result) {
            val parentId = node.parentId ?: 0L
            redisUtil.del("node:list:${node.userId}:$parentId")
        }
        return result
    }

    override fun getNodeList(userId: String, parentId: Long?): List<NodeDTO> {
        val pId = parentId ?: 0L
        val cacheKey = "node:list:$userId:$pId"
        
        val cache = redisUtil.get(cacheKey)
        if (cache != null) {
            try {
                val json = JsonUtils.toJsonString(cache)
                return JsonUtils.parseList(json, object : TypeReference<List<NodeDTO>>() {})
            } catch (e: Exception) {
                log.error("Cache parsing error", e)
            }
        }
        
        val queryWrapper = forumNodesService.lambdaQuery()
            .eq(ForumNodes::getUserId, userId)
            .eq(ForumNodes::getIsDeleted, 0)
            .orderByAsc(ForumNodes::getSortOrder)
            
        // 如果 parentId 为 0 或 null，查询 parentId IS NULL
        if (pId == 0L) {
            queryWrapper.isNull(ForumNodes::getParentId)
        } else {
            queryWrapper.eq(ForumNodes::getParentId, pId)
        }
            
        val list = queryWrapper.list()
            
        val dtos = list.map { 
            val dto = NodeDTO()
            BeanUtils.copyProperties(it, dto)
            dto
        }
        
        redisUtil.set(cacheKey, dtos)
        return dtos
    }


}