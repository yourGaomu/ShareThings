package com.zhangzc.sharethingcountimpl.service.rpc;


import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsLike;
import com.zhangzc.sharethingcountimpl.service.impl.FsLikeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class likeCountRpcImpl implements likeCount {

    private final FsLikeServiceImpl fsLikeServiceImpl;


    @Override
    public List<FsLikeDto> getLikeCountByArticleId(String articleId) {
        List<FsLike> list = fsLikeServiceImpl.lambdaQuery().eq(FsLike::getArticle_id, articleId).list();
        return list.stream().map(fsLike -> {
            FsLikeDto fsLikeDto = new FsLikeDto();
            BeanUtils.copyProperties(fsLike, fsLikeDto);
            return fsLikeDto;
        }).collect(Collectors.toList());

    }

    @Override
    public List<Map<Long, Boolean>> getLikeCountByArticleIdAndUserId(List<String> articleId, String userId) {
        List<FsLike> list = fsLikeServiceImpl.lambdaQuery()
                .eq(FsLike::getLike_user, userId)
                .list();

        // 为了快速判断，先把请求的 articleId 放入 Set
        Set<String> articleSet = new HashSet<>(articleId);
        Map<Long, Boolean> result = new HashMap<>();

        // 标记用户已点赞的文章
        for (FsLike fsLike : list) {
            String idString = fsLike.getArticle_id().toString();
            if (articleSet.contains(idString)) {
                try {
                    result.put(Long.parseLong(idString), true);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 确保所有请求的 articleId 都有一个布尔值（未点赞为 false）
        for (String idStr : articleId) {
            try {
                Long id = Long.parseLong(idStr);
                result.putIfAbsent(id, false);
            } catch (NumberFormatException ignored) {
            }
        }

        return Collections.singletonList(result);
    }
}
