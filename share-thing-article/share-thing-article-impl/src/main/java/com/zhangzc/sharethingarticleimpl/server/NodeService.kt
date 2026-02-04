package com.zhangzc.sharethingarticleimpl.server

import com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO

interface NodeService {
    fun creatNode(nodeDTO: NodeDTO, userId: String): Boolean
    fun deleteNode(nodeId: Long): Boolean
    fun updateNode(nodeDTO: NodeDTO): Boolean
    fun getNodeList(userId: String, parentId: Long?): List<NodeDTO>
}