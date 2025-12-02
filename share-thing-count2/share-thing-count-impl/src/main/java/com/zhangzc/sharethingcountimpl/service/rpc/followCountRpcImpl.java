package com.zhangzc.sharethingcountimpl.service.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangzc.redisspringbootstart.redisConst.RedisSetConst;
import com.zhangzc.redisspringbootstart.utills.RedisSetUtil;
import com.zhangzc.sharethingcountapi.pojo.dto.FsFollowDto;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsFollow;
import com.zhangzc.sharethingcountimpl.service.impl.FsFollowServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class followCountRpcImpl implements followCount {

    private final FsFollowServiceImpl fsFollowServiceImpl;
    private final RedisSetUtil redisSetUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Override
    public List<FsFollowDto> getFollowCountByUserId(String userId) {
        List<FsFollow> list = fsFollowServiceImpl.lambdaQuery().eq(FsFollow::getFromUser, userId).list();
        return list.stream().map(fsFollow -> {
            FsFollowDto fsFollowDto = new FsFollowDto();
            BeanUtils.copyProperties(fsFollow, fsFollowDto);
            return fsFollowDto;
        }).toList();
    }

    @Override
    public Map<String, Boolean> getFollowCountByUserIdAndToUserId(List<Long> toUserIds, Long fromUserId) {
        //构建key
        String key = RedisSetConst.getFollowSetKey(String.valueOf(fromUserId));
        Set<String> members = redisSetUtil.members(key);
        //初始化关注表
        Set<Long> toUserSet = new HashSet<>(toUserIds);
        Map<String, Boolean> result = new HashMap<>();
        toUserSet.forEach(id -> result.putIfAbsent(id.toString(), false));
        if (!members.isEmpty()) {
            //有记录
            members.forEach(id -> result.put(id, true));
            return result;
        }
        //没有查询到记录
        List<FsFollow> list = fsFollowServiceImpl.lambdaQuery()
                .eq(FsFollow::getFromUser, fromUserId)
                .in(FsFollow::getToUser, toUserIds)
                .list();

        for (FsFollow fsFollow : list) {
            Long toUser = fsFollow.getToUser();
            if (toUserSet.contains(toUser)) {
                try {
                    //fromUser关注了toUser
                    result.put(toUser.toString(), true);
                } catch (Exception e) {
                }
            }
        }
        CompletableFuture.runAsync(() -> {
            if (!result.isEmpty()) {
                redisSetUtil.addAll(key, result.keySet());
                // 设置过期时间(例如7天)
                redisSetUtil.expire(key, 7, java.util.concurrent.TimeUnit.DAYS);
            }
        }, threadPoolTaskExecutor);

        return result;
    }
}