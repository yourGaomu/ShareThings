package com.zhangzc.sharethingarticleapi.rpc;

import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;

import java.util.List;
import java.util.Map;

public interface  ArticleRpc {

    Map<String,String> getUserIdsByArticleIds(List<String> articleIds);
    List<ArticleDTO> getArticleDtoByArticleIds(List<String> articleIds);
}
