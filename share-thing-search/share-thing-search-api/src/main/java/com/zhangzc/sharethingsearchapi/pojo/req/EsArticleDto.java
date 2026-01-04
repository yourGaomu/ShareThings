package com.zhangzc.sharethingsearchapi.pojo.req;

import java.util.List;

public class EsArticleDto {

    private String title;

    private String summary;

    private String contentHtmlPlain;

    private List<Integer> labelIds;

    private String labelNames;

    private Integer state;

    private Boolean isDeleted;

    private Long authorId;

    private String authorName;

    private String authorLevel;

    private Long pv;

    private Long likeCount;

    private Long commentCount;

    private Integer top;

}
