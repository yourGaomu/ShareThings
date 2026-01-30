package com.zhangzc.milvusspringbootstart.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "zhang.milvus")
@Setter
@Getter
public class MilvusProperty {
    private Boolean enable;
    private String uri;
    private String dbName;
    private String username;
    private String password;
    private String token;
    private List<String> packages;
    private Boolean openLog;
    private String logLevel;
    private Boolean banner;


}
