package com.zhangzc.sharethingarticleimpl.controller;

import com.zhangzc.sharethingarticleimpl.server.rpc.testrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping
@RestController
public class test {
    private final testrpc testrpc;

    @RequestMapping("/test")
    public String test() {
        return testrpc.test();
    }
}
