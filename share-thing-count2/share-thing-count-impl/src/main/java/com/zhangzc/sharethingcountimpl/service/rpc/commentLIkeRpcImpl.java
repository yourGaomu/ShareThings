package com.zhangzc.sharethingcountimpl.service.rpc;

import com.zhangzc.sharethingcommentapi.rpc.commentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcountapi.rpc.commentAndLike4Article;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsCommentLike;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsFollow;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsLike;
import com.zhangzc.sharethingcountimpl.service.FsArticleService;
import com.zhangzc.sharethingcountimpl.service.FsCommentLikeService;
import com.zhangzc.sharethingcountimpl.service.FsFollowService;
import com.zhangzc.sharethingcountimpl.service.FsLikeService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class commentLIkeRpcImpl implements commentAndLike4Article {
    private final FsCommentLikeService fsCommentLikeService;
    private final FsLikeService fsLikeService;
    private final FsFollowService fsFollowService;
    private final FsArticleService fsArticleService;
    @DubboReference
    private commentSearch commentSearch;

    @Override
    public Map<Long, Map<String, Long>> getArticleLikeAndCommentNumbersByArticleIds(List<String> articleId) {
        Map<Long, Map<String, Long>> result = new HashMap<>();
        Map<Long, Map<String, Long>> likeNumbers = new HashMap<>();
        //查询评论数量
        Map<Long, Map<String, Long>> commentAndLikeNumbersByArticleIds = commentSearch.getCommentAndLikeNumbersByArticleIds(articleId);
        //查询点赞的数量
        List<FsCommentLike> list = fsCommentLikeService.lambdaQuery()
                .in(FsCommentLike::getComment_id, articleId).list();
        //按照文章id分组
        Map<Integer, Long> collect = list.stream().collect(Collectors.groupingBy(FsCommentLike::getComment_id, Collectors.counting()));
        collect.forEach((k, v) -> likeNumbers
                .put(k.longValue(), Map.of(articleCommentAndLike.likeNumber, v)));
        //保证每个文章id都有
        articleId.forEach(id -> likeNumbers.putIfAbsent(Long.parseLong(id), Map.of(articleCommentAndLike.likeNumber, 0L)));
        //合并
        likeNumbers.forEach((k, v) -> {
            result.put(k, v);
            result.get(k).putAll(commentAndLikeNumbersByArticleIds.getOrDefault(k, Map.of()));
        });
        return result;
    }

    @Override
    public Map<Long, Map<String, Boolean>> getLikeAndFollowByArticleIdAndUserId(List<String> articleId, List<String> userId) {
        Set<String> userIDs = new HashSet<>(userId);
        Map<Long, Map<String, Boolean>> result = new HashMap<>();
        //查询点赞
        List<FsLike> list = fsLikeService.lambdaQuery().in(FsLike::getArticle_id, articleId).list();
        list.forEach(like -> {
            Integer articleId1 = like.getArticle_id();
            Long likeUser = like.getLike_user();
            if (userIDs.contains(likeUser.toString())) {
                result.putIfAbsent(articleId1.longValue(), new HashMap<>());
                Map<String, Boolean> stringBooleanMap = result.get(articleId1.longValue());
                stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isLike, true);
                stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isFollow, false);
            }else {
                result.putIfAbsent(articleId1.longValue(), new HashMap<>());
                Map<String, Boolean> stringBooleanMap = result.get(articleId1.longValue());
                stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isLike, false);
                stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isFollow, false);
            }
        });
        //查询关注
        //按照用户id分组
        Map<Long, List<FsArticle>> collect = fsArticleService.lambdaQuery()
                .in(FsArticle::getId, articleId).list()
                .stream()
                .collect(Collectors.groupingBy(FsArticle::getCreate_user));
        //查询关注表
        List<Long> followUserIDs = fsFollowService.lambdaQuery().in(FsFollow::getFrom_user, userId).list()
                .stream().map(FsFollow::getTo_user).toList();

        collect.forEach((toUserId, article) -> {
            //获取这个文章
            FsArticle fsArticle = article.get(0);
            //获取读者阅读的文章是他关注的作者写的
            if (followUserIDs.contains(toUserId)) {
                result.putIfAbsent(fsArticle.getId().longValue(), new HashMap<>());
                Map<String, Boolean> stringBooleanMap = result.get(fsArticle.getId().longValue());
                stringBooleanMap.put(LikeAndFollowEnums.isFollow, true);
            }
        });
        return result;
    }
}
