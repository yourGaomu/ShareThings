package com.zhangzc.sharethingsgateway.pojo.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthLoginVo {
    private String phone;
    private String code;
}
