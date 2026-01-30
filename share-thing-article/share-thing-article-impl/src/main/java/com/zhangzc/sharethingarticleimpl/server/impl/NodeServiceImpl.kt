package com.zhangzc.sharethingarticleimpl.server.impl

import com.zhangzc.sharethingarticleimpl.pojo.domain.ForumNodes
import com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO
import com.zhangzc.sharethingarticleimpl.server.ForumNodesService
import com.zhangzc.sharethingarticleimpl.server.NodeService
import com.zhangzc.sharethingscommon.exception.BusinessException
import com.zhangzc.sharethingscommon.utils.TimeUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class NodeServiceImpl (
    private val forumNodesService: ForumNodesService
) : NodeService{
    private val log = LoggerFactory.getLogger(NodeServiceImpl::class.java)

    override fun creatNode(nodeDTO: NodeDTO, userId: String): Boolean {
        //判断文件夹类型
        var result: Boolean = false
        when(nodeDTO.nodeType){
            NodeDTO.NodeType.FOLDER ->{
                //文件夹类型
                var forumNodes = ForumNodes()
                forumNodes.nodeType = NodeDTO.NodeType.FOLDER
                forumNodes.userId = userId
                forumNodes.name = nodeDTO.name
                forumNodes.parentId = nodeDTO.parentId
                forumNodes.articleId = null
                forumNodes.sortOrder = 1
                forumNodes.isDeleted = 0
                forumNodes.createdAt= TimeUtil.getDateTime(LocalDateTime.now())
                forumNodes.updatedAt= TimeUtil.getDateTime(LocalDateTime.now())
                result = forumNodesService.save(forumNodes)

            }
            NodeDTO.NodeType.ARTICLE ->{
                //文章类型
                var forumNodes = ForumNodes()
                forumNodes.nodeType = NodeDTO.NodeType.FOLDER
                forumNodes.userId = userId
                forumNodes.name = nodeDTO.name
                forumNodes.parentId = nodeDTO.parentId
                forumNodes.articleId = nodeDTO.articleId
                forumNodes.sortOrder = 1
                forumNodes.isDeleted = 0
                forumNodes.createdAt= TimeUtil.getDateTime(LocalDateTime.now())
                forumNodes.updatedAt= TimeUtil.getDateTime(LocalDateTime.now())
                result = forumNodesService.save(forumNodes)

            }
            else -> {
                log.error("用户操作异常")
                throw BusinessException("500", "节点类型错误")
            }
        }
    return result
    }


}