package com.zhangzc.sharethingsearchapi.rpc;

import com.zhangzc.sharethingsearchapi.pojo.req.EsArticleDto;

import java.util.List;

public interface EsRpc {
    Long addArticles(List<EsArticleDto> esArticleDto);

    List<EsArticleDto> search(String keyword);
}
