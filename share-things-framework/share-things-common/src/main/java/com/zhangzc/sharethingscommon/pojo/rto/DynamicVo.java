package com.zhangzc.sharethingscommon.pojo.rto;


import lombok.Data;

@Data
public class DynamicVo {
    Long userID;
    Integer currentPage;
    Integer pageSize;
}
