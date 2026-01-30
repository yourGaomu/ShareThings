package com.zhangzc.sharethinguserimpl.pojo.req;


import lombok.Data;

/**
 * 内容查询入参实体
 * 对应前端分页+筛选的查询参数
 */
@Data
public class ArticleQueryRequestDto {

    /**
     * 当前页码，必填，默认1
     */

    private Integer currentPage = 1;

    /**
     * 每页条数，必填，默认10
     */

    private Integer pageSize = 10;

    /**
     * 时间筛选，非必填
     * 可选值：today(今日)、week(近7天)、month(近30天)、all(全部)
     */
    private String timeRange;

    /**
     * 分类筛选，非必填
     * 可选值：tech(技术)、life(生活)、qa(问答)、other(其他)、all(全部)
     */
    private String category;
}