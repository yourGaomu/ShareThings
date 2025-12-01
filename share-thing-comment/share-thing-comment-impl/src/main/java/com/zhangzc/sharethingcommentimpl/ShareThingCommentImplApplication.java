package com.zhangzc.sharethingcommentimpl;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class ShareThingCommentImplApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareThingCommentImplApplication.class, args);
    }

}
