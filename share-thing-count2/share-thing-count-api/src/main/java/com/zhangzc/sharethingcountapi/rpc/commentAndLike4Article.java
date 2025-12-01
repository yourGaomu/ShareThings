package com.zhangzc.sharethingcountapi.rpc;

import java.util.List;
import java.util.Map;

public interface commentAndLike4Article {

   Map<Long,Map<String,Long>> getArticleLikeAndCommentNumbersByArticleIds(List<String> articleId);

}
