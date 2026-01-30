package com.zhangzc.sharethingarticleimpl.server

import com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO

interface NodeService {
    fun creatNode(nodeDTO: com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO, userId: String): Boolean
}