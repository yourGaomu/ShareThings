package com.zhangzc.sharethingarticleimpl.controller;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.sharethingarticleimpl.pojo.dto.NodeDTO;
import com.zhangzc.sharethingarticleimpl.server.NodeService;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bbs/node/")
@RequiredArgsConstructor
public class NodeController {
    private final NodeService nodeService;


    @PostMapping("/create")
    public R creatNode(@RequestBody NodeDTO nodeDTO){
        String userId = (String) GlobalContext.get();
        Boolean result = nodeService.creatNode(nodeDTO,userId);
        return R.ok(result?"创建成功":"创建失败了");
    }

    @PostMapping("/delete/{nodeid}")
    public R deleteNodeById(@PathVariable Integer nodeid){
        Boolean result = nodeService.deleteNode(Long.valueOf(nodeid));
        return R.ok(result ? "删除成功" : "删除失败");
    }

    @PostMapping("/update")
    public R updateNode(@RequestBody NodeDTO nodeDTO){
        Boolean result = nodeService.updateNode(nodeDTO);
        return R.ok(result ? "更新成功" : "更新失败");
    }

    @PostMapping("/list")
    public R getNodeList(@RequestBody(required = false) NodeDTO nodeDTO){
        String userId = (String) GlobalContext.get();
        Long parentId = (nodeDTO != null) ? nodeDTO.getParentId() : null;
        return R.ok(nodeService.getNodeList(userId, parentId));
    }


}
