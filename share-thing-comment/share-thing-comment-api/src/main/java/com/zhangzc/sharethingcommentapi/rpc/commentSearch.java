package com.zhangzc.sharethingcommentapi.rpc;

import java.util.List;
import java.util.Map;

public interface commentSearch {

    Map<Long,Map<String,Long>> getCommentAndLikeNumbersByArticleIds(List<String> articleId);

    Map<String,Long> getCommentNumbers();

}
