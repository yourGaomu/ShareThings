package com.zhangzc.sharethingcountimpl.service.rpc;

import com.zhangzc.sharethingcommentapi.rpc.CommentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcountapi.rpc.CommentAndLike4Article;
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
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class commentLIkeRpcImpl implements CommentAndLike4Article {
    private final FsCommentLikeService fsCommentLikeService;
    private final FsLikeService fsLikeService;
    private final FsFollowService fsFollowService;
    private final FsArticleService fsArticleService;
    @DubboReference(check = false)
    private CommentSearch commentSearch;

    @Override
    public Map<Long, Map<String, Long>> getArticleLikeAndCommentNumbersByArticleIds(List<String> articleId) {
        Map<Long, Map<String, Long>> result = new HashMap<>();
        Map<Long, Map<String, Long>> likeNumbers = new HashMap<>();
        //查询评论数量
        Map<Long, Map<String, Long>> commentAndLikeNumbersByArticleIds = commentSearch.getCommentNumbersByArticleIds(articleId);
        //查询点赞的数量
        List<FsLike> list = fsLikeService.lambdaQuery()
                .in(FsLike::getArticleId, articleId).list();
        //按照文章id分组
        Map<Integer, Long> collect = list.stream().collect(Collectors.groupingBy(FsLike::getArticleId, Collectors.counting()));

        collect.forEach((articleId1, likeNumber) -> likeNumbers
                .put(articleId1.longValue(), Map.of(articleCommentAndLike.likeNumber, likeNumber)));
        //保证每个文章id都有
        articleId.forEach(id ->
                likeNumbers.putIfAbsent(Long.parseLong(id), Map.of(articleCommentAndLike.likeNumber, 0L)));
        //合并
        likeNumbers.forEach((k, v) -> {
            //放入点赞的数量
            Map<String, Long> data = new  HashMap<>();
            data.putAll(v);
            //放入评论的数量
            data.putAll(commentAndLikeNumbersByArticleIds.get(k));
            result.put(k, data);
        });
        System.out.println(result);
        return result;
    }

    @Override
    public Map<Long, Map<String, Boolean>> getLikeAndFollowByArticleIdAndUserId(List<String> articleId, List<String> userId) {
        Set<String> userIDs = new HashSet<>(userId);
        Map<Long, Map<String, Boolean>> result = new HashMap<>();
        //查询点赞
        List<FsLike> list = fsLikeService.lambdaQuery().in(FsLike::getArticleId, articleId).list();
        list.forEach(like -> {
            Integer articleId1 = like.getArticleId();
            Long likeUser = like.getLikeUser();
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
                .collect(Collectors.groupingBy(FsArticle::getCreateUser));
        //查询关注表
        List<Long> followUserIDs = fsFollowService.lambdaQuery().in(FsFollow::getFromUser, userId).list()
                .stream().map(FsFollow::getToUser).toList();

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
