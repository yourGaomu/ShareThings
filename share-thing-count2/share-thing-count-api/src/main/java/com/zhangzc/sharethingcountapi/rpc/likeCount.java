package com.zhangzc.sharethingcountapi.rpc;


import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;

import java.util.List;
import java.util.Map;

public interface likeCount {

    List<FsLikeDto> getLikeCountByArticleId(String articleId);

    Map<Long,Boolean> getLikeCountByArticleIdAndUserId(List<String> articleId, String userId);

    List<FsLikeDto> getLikeCountByLikeUser(Integer currentPage, Integer pageSize, Long likeUser);

    Map<String,Double> getLikeCountByUserIds(List<String> userIds);

    Boolean likeArticleByUserId(String articleId,String userId,String authorId);

}