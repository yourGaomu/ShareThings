package com.zhangzc.sharethinguserimpl.pojo.vo;


import lombok.Data;

// 1. 定义参数接收类
@Data
public class UserIdRequest {
    private String userId; // 参数名必须和前端传递的一致
}



