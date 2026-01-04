package com.zhangzc.sharethingarticleimpl;


import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.dromara.easyes.spring.annotation.EsMapperScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
@EsMapperScan("com.zhangzc.sharethingarticleimpl.mapper.esMapper")
@MapperScan("com.zhangzc.sharethingarticleimpl.mapper")
public class ShareThingArticleImplApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareThingArticleImplApplication.class, args);
    }

}
