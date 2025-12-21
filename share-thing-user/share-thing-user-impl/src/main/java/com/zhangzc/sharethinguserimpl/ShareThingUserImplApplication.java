package com.zhangzc.sharethinguserimpl;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import com.zhangzc.sharethingscommon.ExceptionHandle.SaTokenExceptionHandler;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.zhangzc.sharethinguserimpl.mapper")
@EnableTransactionManagement // 开启事务管理（关键注解）
@ComponentScan({"com.zhangzc.sharethinguserimpl","com.zhangzc.sharethingscommon"})
@EnableScheduling
@Import(SaTokenExceptionHandler.class)
public class ShareThingUserImplApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareThingUserImplApplication.class, args);
    }

}
