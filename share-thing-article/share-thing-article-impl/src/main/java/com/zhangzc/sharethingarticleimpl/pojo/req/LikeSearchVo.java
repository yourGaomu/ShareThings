package com.zhangzc.sharethingarticleimpl.pojo.req;

import lombok.Data;

@Data
public class LikeSearchVo {
    /*
    用户id
    * */
    private Long likeUser;

    private Integer currentPage;

    private Integer pageSize;
}
