package com.zhangzc.sharethingcommentapi.rpc;

import java.util.List;
import java.util.Map;

public interface CommentSearch {

    Map<Long,Map<String,Long>> getCommentNumbersByArticleIds(List<String> articleId);

    Map<String,Long> getCommentNumbers();

}
