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


    @PostMapping("creat")
    public R creatNode(@RequestBody NodeDTO nodeDTO){
        String userId = (String) GlobalContext.get();
        Boolean result = nodeService.creatNode(nodeDTO,userId);
        return R.ok(result?"创建成功":"创建失败了");
    }

    @PostMapping("/delete/{nodeid}")
    public R deleteNodeById(@PathVariable Integer nodeid){
        return null;

    }

    @PostMapping("/update")
    public R updateNode(){
        return null;
    }


}
