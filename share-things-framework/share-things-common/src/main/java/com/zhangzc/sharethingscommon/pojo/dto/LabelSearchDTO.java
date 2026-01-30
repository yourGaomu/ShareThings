package com.zhangzc.sharethingscommon.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author maliangnansheng
 * @date 2022/4/6 15:06
 */
@Data
public class LabelSearchDTO implements Serializable {

    /**
     * 当前页
     */
    private Long currentPage;

    /**
     * 每页条数
     */
    private Long pageSize;

    private static final long serialVersionUID = 1L;

}
