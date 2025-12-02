package com.zhangzc.sharethingadminimpl.controller;


import com.zhangzc.leaf.core.common.Result;
import com.zhangzc.leaf.server.service.SegmentService;

import com.zhangzc.leaf.server.service.SnowflakeService;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class test {

    private final  SegmentService segmentService;
    private final SnowflakeService snowflakeService;

    @PostMapping("/test")
    public R test() {
        return R.ok("test");
    }


    @PostMapping("/test2")
    public R test2() {
        Result user = segmentService.getId("user");
        System.out.println(user.getId());
        return R.ok("test2");
    }


    @PostMapping("/test3")
    public R test3() {
        Result user = snowflakeService.getId("user");
        System.out.println(user.getId());
        return R.ok("test3");
    }

}
