package com.zhangzc.sharethingcountimpl.service.rpc;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangzc.redisspringbootstart.redisConst.RedisSetConst;
import com.zhangzc.redisspringbootstart.utills.RedisSetUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.redisspringbootstart.utills.RedissonUtil;
import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsLike;
import com.zhangzc.sharethingcountimpl.service.impl.FsLikeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class likeCountRpcImpl implements likeCount {

    private final FsLikeServiceImpl fsLikeServiceImpl;
    private final RedissonUtil redissonUtil;
    private final RedisUtil redisUtil;
    private final RedisSetUtil redisSetUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public List<FsLikeDto> getLikeCountByArticleId(String articleId) {
        List<FsLike> list = fsLikeServiceImpl.lambdaQuery().eq(FsLike::getArticleId, articleId).list();
        return list.stream().map(fsLike -> {
            FsLikeDto fsLikeDto = new FsLikeDto();
            BeanUtils.copyProperties(fsLike, fsLikeDto);
            return fsLikeDto;
        }).collect(Collectors.toList());

    }

    @Override
    public Map<Long, Boolean> getLikeCountByArticleIdAndUserId(List<String> articleId, String userId) {
        // 1. 参数校验
        if (articleId == null || articleId.isEmpty() || userId == null || userId.isEmpty()) {
            return new HashMap<>();
        }
        // 2. 转换articleId为Long集合(避免多次转换)
        Set<Long> articleIdSet = articleId.stream()
                .map(idStr -> {
                    try {
                        return Long.parseLong(idStr);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (articleIdSet.isEmpty()) {
            return new HashMap<>();
        }

        // 3. 初始化结果Map,默认所有文章都未点赞
        Map<Long, Boolean> result = new HashMap<>();
        articleIdSet.forEach(id -> result.put(id, false));

        // 4. 优先从Redis查询(Redis中存储的是String类型的articleId)
        String likeSetKey = RedisSetConst.getLikeSetKey(userId);
        Set<String> redisLikedArticles = redisSetUtil.members(likeSetKey);

        if (redisLikedArticles != null && !redisLikedArticles.isEmpty()) {
            // Redis命中,直接使用缓存数据
            for (String articleIdStr : redisLikedArticles) {
                try {
                    Long likedArticleId = Long.parseLong(articleIdStr);
                    if (articleIdSet.contains(likedArticleId)) {
                        result.put(likedArticleId, true);
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略无效数据
                }
            }
            return result;
        }

        // 5. Redis未命中,从数据库查询(只查询指定的文章ID,避免全表扫描)
        List<Integer> articleIdIntegers = articleIdSet.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

        List<FsLike> likedArticles = fsLikeServiceImpl.lambdaQuery()
                .eq(FsLike::getLikeUser, Long.parseLong(userId))
                .in(FsLike::getArticleId, articleIdIntegers)
                .eq(FsLike::getState, 1) // 只查询点赞状态的记录
                .list();

        // 6. 更新结果Map
        Set<String> likedArticleIdsForRedis = new HashSet<>();
        for (FsLike fsLike : likedArticles) {
            Long likedArticleId = fsLike.getArticleId().longValue();
            result.put(likedArticleId, true);
            likedArticleIdsForRedis.add(fsLike.getArticleId().toString());
        }

        // 7. 回写Redis缓存(异步或同步,根据业务需求)
        CompletableFuture.runAsync(() -> {
            if (!likedArticleIdsForRedis.isEmpty()) {
                redisSetUtil.addAll(likeSetKey, likedArticleIdsForRedis);
                // 设置过期时间(例如7天)
                redisSetUtil.expire(likeSetKey, 7, java.util.concurrent.TimeUnit.DAYS);
            }
        }, threadPoolTaskExecutor);

        return result;
    }

    @Override
    public List<FsLikeDto> getLikeCountByLikeUser(Integer currentPage, Integer pageSize, Long likeUser) {
        IPage<FsLike> page = new Page<>(currentPage, pageSize);
        IPage<FsLike> page1 = fsLikeServiceImpl
                .lambdaQuery()
                .eq(FsLike::getLikeUser, likeUser)
                .page(page);
        if (page1.getRecords() == null || page1.getRecords().isEmpty()) {
            return Collections.emptyList();
        }

        return page1.getRecords().stream().map(record -> {
            FsLikeDto fsLikeDto = new FsLikeDto();
            BeanUtils.copyProperties(record, fsLikeDto);
            return fsLikeDto;
        }).toList();


    }
}
