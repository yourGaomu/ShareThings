package com.zhangzc.sharethingarticleimpl.server.rpc;


import com.zhangzc.sharethingadminapi.api.test3;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
public class testrpc {

    @DubboReference(registry = "share_things")
    private test3 test3;

    public String test() {
        return test3.test();
    }
}
