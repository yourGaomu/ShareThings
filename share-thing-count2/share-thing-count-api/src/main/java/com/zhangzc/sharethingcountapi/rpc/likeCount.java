package com.zhangzc.sharethingcountapi.rpc;


import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;

import java.util.List;
import java.util.Map;

public interface likeCount {

    List<FsLikeDto> getLikeCountByArticleId(String articleId);

    List<Map<Long,Boolean>> getLikeCountByArticleIdAndUserId(List<String> articleId, String userId);

}