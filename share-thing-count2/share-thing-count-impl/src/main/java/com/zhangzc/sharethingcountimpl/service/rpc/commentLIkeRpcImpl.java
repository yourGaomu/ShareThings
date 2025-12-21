package com.zhangzc.sharethingcountimpl.service.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingarticleapi.rpc.ArticleRpc;
import com.zhangzc.sharethingcommentapi.rpc.CommentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcountapi.rpc.CommentAndLike4Article;
import com.zhangzc.sharethingcountimpl.consts.RedisFollowConsts;
import com.zhangzc.sharethingcountimpl.consts.RedisLikeConsts;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsFollow;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsLike;
import com.zhangzc.sharethingcountimpl.service.FsArticleService;
import com.zhangzc.sharethingcountimpl.service.FsCommentLikeService;
import com.zhangzc.sharethingcountimpl.service.FsFollowService;
import com.zhangzc.sharethingcountimpl.service.FsLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
@Slf4j
public class commentLIkeRpcImpl implements CommentAndLike4Article {
    private final FsCommentLikeService fsCommentLikeService;
    private final FsLikeService fsLikeService;
    private final FsFollowService fsFollowService;
    private final FsArticleService fsArticleService;
    private final RedisUtil redisUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @DubboReference(check = false)
    private CommentSearch commentSearch;
    @DubboReference(check = false)
    private ArticleRpc articleRpc;

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
            Map<String, Long> data = new HashMap<>();
            data.putAll(v);
            //放入评论的数量
            data.putAll(commentAndLikeNumbersByArticleIds.get(k));
            result.put(k, data);
        });
        System.out.println(result);
        return result;
    }

    @Override
    public Map<Long, Map<String, Boolean>> getLikeAndFollowByArticleIdAndUserId(List<String> articleId, String userId) {
        //参数有检查
        if (articleId == null || articleId.isEmpty() || userId == null || userId.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, Map<String, Boolean>> result = new HashMap<>();
        List<String> needQuery4Like = new ArrayList<>(Collections.singleton(userId));
        List<String> needQuery4LikeArticleIds = new ArrayList<>(articleId);
        List<String> needQuery4Follow = new ArrayList<>();
        //先从redis里面查询点赞表
        String redisLikeKey = RedisLikeConsts.RedisLikeKey;
        //查询userId->ArticleIds
        List<Object> hmget = redisUtil.hmget(redisLikeKey, needQuery4Like);
        if (!hmget.isEmpty()) {
            //不为空
            AtomicInteger index = new AtomicInteger(0);
            hmget.forEach(sign -> {
                if (sign == null) {
                    index.getAndIncrement();
                    return;
                }
                //先序列化
                List<String> userLikeArticleIds = JsonUtils.parseList(JsonUtils.toJsonString(sign)
                        , new TypeReference<List<String>>() {
                        });
                //开始循环
                articleId.forEach(id -> {
                    Map<String, Boolean> stringBooleanMap = result.getOrDefault(Long.parseLong(id), new HashMap<>());
                    if (userLikeArticleIds.contains(id)) {
                        needQuery4LikeArticleIds.remove(id);
                        stringBooleanMap.put(LikeAndFollowEnums.isLike, true);
                    } else {
                        stringBooleanMap.put(LikeAndFollowEnums.isLike, false);
                    }
                    stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isFollow, false);
                    result.put(Long.parseLong(id), stringBooleanMap);
                });
                //不需要查询
                needQuery4Like.remove(index.get());
                index.getAndIncrement();
            });
        }
        //如果需要查询的点赞记录不为空
        if (!needQuery4Like.isEmpty()) {
            //数据库查询点赞记录
            Map<Long, List<Integer>> records = fsLikeService.lambdaQuery().eq(FsLike::getState, 1)
                    .in(FsLike::getLikeUser, needQuery4Like)
                    .list().stream()
                    .collect(Collectors.groupingBy(FsLike::getLikeUser, Collectors.mapping(FsLike::getArticleId, Collectors.toList())));
            records.forEach((k, v) -> {
                if (!v.isEmpty()) {
                    Set<Integer> set = new HashSet<>(v);
                    articleId.forEach(id -> {
                        Map<String, Boolean> stringBooleanMap = result.getOrDefault(Long.parseLong(id), new HashMap<>());
                        stringBooleanMap.put(LikeAndFollowEnums.isLike, set.contains(Integer.parseInt(id)));
                        stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isFollow, false);
                        result.put(Long.parseLong(id), stringBooleanMap);
                    });
                } else {
                    articleId.forEach(id -> {
                        Map<String, Boolean> stringBooleanMap = result.getOrDefault(Long.parseLong(id), new HashMap<>());
                        stringBooleanMap.put(LikeAndFollowEnums.isLike, false);
                        stringBooleanMap.putIfAbsent(LikeAndFollowEnums.isFollow, false);
                        result.put(Long.parseLong(id), stringBooleanMap);
                    });
                }
            });
            CompletableFuture.runAsync(() -> {
                //存入redis里面
                //转换为Map<String,List<String>>
                Map<String, Object> collect = new HashMap<>();
                records.forEach((k, v) -> {
                    List<String> collect1 = v.stream().map(String::valueOf).collect(Collectors.toList());
                    collect.put(k.toString(), collect1);
                });
                redisUtil.hmset(redisLikeKey, collect);
            }, threadPoolTaskExecutor);
        }
        //需要根据文章id查询对应的作者id
        Map<String, String> userIdsByArticleIds = articleRpc.getUserIdsByArticleIds(articleId);

        //如果RPC返回为null或为空，需要初始化所有文章的isFollow为false
        if (userIdsByArticleIds == null || userIdsByArticleIds.isEmpty()) {
            //没有查询到任何作者信息，所有文章的关注状态设为false
            articleId.forEach(id -> {
                Map<String, Boolean> map = result.getOrDefault(Long.parseLong(id), new HashMap<>());
                map.put(LikeAndFollowEnums.isFollow, false);
                result.put(Long.parseLong(id), map);
            });
            return result;
        }

        //去重获取所有作者ID
        Set<String> authorIds = new HashSet<>(userIdsByArticleIds.values());
        needQuery4Follow.addAll(authorIds);

        //先从redis里面查询当前用户的关注列表
        String redisFollowKey = RedisFollowConsts.RedisFollowKey;
        //userId->list.of(AuthorIds)
        List<Object> hmget1 = redisUtil.hmget(redisFollowKey, Collections.singletonList(userId));

        if (!hmget1.isEmpty() && hmget1.get(0) != null) {
            //从缓存获取到当前用户的关注列表
            List<String> userFollowedUserIds = JsonUtils.parseList(JsonUtils.toJsonString(hmget1.get(0))
                    , new TypeReference<List<String>>() {
                    });

            //遍历每篇文章，判断其作者是否在关注列表中
            userIdsByArticleIds.forEach((articleIdStr, authorId) -> {
                Map<String, Boolean> map = result.getOrDefault(Long.parseLong(articleIdStr), new HashMap<>());
                map.put(LikeAndFollowEnums.isFollow, userFollowedUserIds.contains(authorId));
                result.put(Long.parseLong(articleIdStr), map);
            });
            needQuery4Follow.clear(); // 已从缓存获取，无需查数据库
        }

        if (!needQuery4Follow.isEmpty()) {
            //从数据库查询当前用户关注了哪些作者
            List<FsFollow> list = fsFollowService.lambdaQuery()
                    .eq(FsFollow::getFromUser, Long.parseLong(userId)) // 当前用户
                    .in(FsFollow::getToUser, needQuery4Follow.stream()
                            .map(Long::parseLong).collect(Collectors.toList())) // 这些文章的作者ID
                    .eq(FsFollow::getState, 1)
                    .list();

            //获取当前用户关注的作者ID集合
            Set<String> followedAuthorIds = list.stream()
                    .map(FsFollow::getToUser)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());

            //遍历每篇文章，设置关注状态
            userIdsByArticleIds.forEach((articleIdStr, authorId) -> {
                Map<String, Boolean> map = result.getOrDefault(Long.parseLong(articleIdStr), new HashMap<>());
                map.put(LikeAndFollowEnums.isFollow, followedAuthorIds.contains(authorId));
                result.put(Long.parseLong(articleIdStr), map);
            });

            //异步存入redis（存储当前用户的所有关注关系）
            CompletableFuture.runAsync(() -> {
                List<FsFollow> allFollows = fsFollowService.lambdaQuery()
                        .eq(FsFollow::getFromUser, Long.parseLong(userId))
                        .eq(FsFollow::getState, 1)
                        .list();
                List<String> allFollowedUserIds = allFollows.stream()
                        .map(FsFollow::getToUser)
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                Map<String, Object> data = new HashMap<>();
                data.put(userId, allFollowedUserIds);
                redisUtil.hmset(redisFollowKey, data);
            }, threadPoolTaskExecutor);
        }

        //兜底处理：确保所有文章都有isLike和isFollow字段
        articleId.forEach(id -> {
            Map<String, Boolean> map = result.getOrDefault(Long.parseLong(id), new HashMap<>());
            //如果没有isLike字段，设置默认值
            if (!map.containsKey(LikeAndFollowEnums.isLike)) {
                map.put(LikeAndFollowEnums.isLike, false);
            }
            //如果没有isFollow字段，设置默认值
            if (!map.containsKey(LikeAndFollowEnums.isFollow)) {
                map.put(LikeAndFollowEnums.isFollow, false);
            }
            result.put(Long.parseLong(id), map);
        });

        return result;
    }
}
