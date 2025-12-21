package com.zhangzc.sharethingcountapi.rpc;

import java.util.List;
import java.util.Map;

public interface CommentAndLike4Article {

   Map<Long,Map<String,Long>> getArticleLikeAndCommentNumbersByArticleIds(List<String> articleId);
   Map<Long,Map<String,Boolean>> getLikeAndFollowByArticleIdAndUserId(List<String> articleId, String userId);
}
