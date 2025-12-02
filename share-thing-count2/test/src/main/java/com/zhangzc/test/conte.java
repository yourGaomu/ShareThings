package com.zhangzc.test;



import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/test")
@RestController
public class conte {

    @DubboReference(check = false)
    private text text;


    @RequestMapping("/test")
    public String test() {
        return text.test();
    }

}
