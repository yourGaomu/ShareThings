package com.zhangzc.sharethingcountimpl.service.rpc;

import com.zhangzc.sharethingcommentapi.rpc.commentSearch;
import com.zhangzc.sharethingcountapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcountapi.rpc.commentAndLike4Article;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsCommentLike;
import com.zhangzc.sharethingcountimpl.service.FsCommentLikeService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class commentLIkeRpcImpl implements commentAndLike4Article {
    private final FsCommentLikeService fsCommentLikeService;

    @DubboReference
    private commentSearch commentSearch;

    @Override
    public Map<Long, Map<String, Long>> getArticleLikeAndCommentNumbersByArticleIds(List<String> articleId) {
        Map<Long, Map<String, Long>> result = new HashMap<>();
        Map<Long, Map<String, Long>> likeNumbers = new HashMap<>();
        //查询评论数量
        Map<Long, Map<String, Long>> commentAndLikeNumbersByArticleIds = commentSearch.getCommentAndLikeNumbersByArticleIds(articleId);
        //查询点赞的数量
        List<FsCommentLike> list = fsCommentLikeService.lambdaQuery().in(FsCommentLike::getComment_id, articleId).list();
        //按照文章id分组
        Map<Integer, Long> collect = list.stream().collect(Collectors.groupingBy(FsCommentLike::getComment_id, Collectors.counting()));
        collect.forEach((k, v) -> likeNumbers.put(k.longValue(), Map.of(articleCommentAndLike.likeNumber, v)));
        //保证每个文章id都有
        articleId.forEach(id -> likeNumbers.putIfAbsent(Long.parseLong(id), Map.of(articleCommentAndLike.likeNumber, 0L)));
        //合并
        likeNumbers.forEach((k, v) -> {
            result.put(k, v);
            result.get(k).putAll(commentAndLikeNumbersByArticleIds.getOrDefault(k, Map.of()));
        });
        return result;
    }
}
