package com.zhangzc.sharethingcountimpl.service.rpc;

import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingcountapi.consts.RedisUserGetPvCounts;
import com.zhangzc.sharethingcountapi.rpc.pvCount;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingcountimpl.service.FsArticleService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class PvCountRpcImpl implements pvCount {
    private final RedisUtil redisUtil;
    private final FsArticleService fsArticleService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Override
    public Map<String, Double> getPVCountByUserIds(List<String> userIds) {
        String redisUserGetPvCounts = RedisUserGetPvCounts.RedisUserGetPvCounts;
        List<String> needQuery = new ArrayList<>(userIds);
        Map<String, Double> stringDoubleMap = redisUtil.batchZScore(redisUserGetPvCounts, userIds);
        stringDoubleMap.forEach((s, stringDouble) -> {
            if (stringDouble != null) {
                needQuery.remove(s);
            } else {
                stringDoubleMap.put(s, 0.0);
            }
        });
        //需要查询
        if (!needQuery.isEmpty()) {
            //查询这个人有哪些文章
            needQuery.forEach(userId -> {
                //获取有哪些文章id
                List<FsArticle> list1 = fsArticleService.lambdaQuery()
                        .eq(FsArticle::getCreateUser, userId)
                        .eq(FsArticle::getIsDeleted, 0)
                        .list();
                if (list1.isEmpty()) {
                    //没有发布任何文章
                    stringDoubleMap.put(userId, 0.0);
                }
                //获取PV
                int collect = list1.stream().mapToInt(FsArticle::getPv).sum();
                stringDoubleMap.put(userId, (double) collect);
            });
            CompletableFuture.runAsync(() -> {
                Map<Object, Double> map = stringDoubleMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                redisUtil.zAdd(redisUserGetPvCounts, map);
            }, threadPoolTaskExecutor);
        }
        return stringDoubleMap;
    }
}
