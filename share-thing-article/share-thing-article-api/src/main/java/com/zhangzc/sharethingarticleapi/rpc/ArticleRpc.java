package com.zhangzc.sharethingarticleapi.rpc;

import java.util.List;
import java.util.Map;

public interface ArticleRpc {

    Map<String,String> getUserIdsByArticleIds(List<String> articleIds);

}
