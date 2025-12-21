package com.zhangzc.sharethingarticleimpl.pojo.req;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetArticleInfoVo {
    private String id;
    private Boolean isPv;
}
