package com.zhangzc.milvusspringbootstart.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zhang.embedding")
@Data
public class EmbeddingProperty {
    private String model;
    private String endpoint;
    private String apiKey;
    private String baseUrl;
    private Integer dimensions;
    private Integer defaultSliceSize = 2048;
    private String sliceMode = "sentence";



    @Getter
    public enum SliceDefaultMode{
        SENTENCE("sentence"),
        SIZE("size");
        private String mode;
        SliceDefaultMode(String mode) {
            this.mode=mode;
        }
    }
}
