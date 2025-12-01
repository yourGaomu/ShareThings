package com.zhangzc.sharethingadminimpl;


import com.zhangzc.leaf.server.properties.LeafProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;




@SpringBootApplication
public class ShareThingAdminImplApplication {



    public static void main(String[] args) {
        SpringApplication.run(ShareThingAdminImplApplication.class, args);
    }

}
