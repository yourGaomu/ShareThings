package com.zhangzc.sharethingcountimpl.service.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangzc.sharethingcountapi.pojo.dto.FsFollowDto;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountimpl.pojo.domain.FsFollow;
import com.zhangzc.sharethingcountimpl.service.impl.FsFollowServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class followCountRpcImpl implements followCount {

    private final FsFollowServiceImpl fsFollowServiceImpl;


    @Override
    public List<FsFollowDto> getFollowCountByUserId(String userId) {
        List<FsFollow> list = fsFollowServiceImpl.lambdaQuery().eq(FsFollow::getFrom_user, userId).list();
        return list.stream().map(fsFollow -> {
            FsFollowDto fsFollowDto = new FsFollowDto();
            BeanUtils.copyProperties(fsFollow, fsFollowDto);
            return fsFollowDto;
        }).toList();
    }

    @Override
    public Map<String, Boolean> getFollowCountByUserIdAndToUserId(List<Long> toUserIds, Long fromUserId) {
        List<FsFollow> list = fsFollowServiceImpl.lambdaQuery()
                .eq(FsFollow::getFrom_user, fromUserId).list();
        Set<Long> toUserSet = new HashSet<>(toUserIds);

        Map<String, Boolean> result = new HashMap<>();
        for (FsFollow fsFollow : list) {
            Long toUser = fsFollow.getTo_user();
            if (toUserSet.contains(toUser)) {
                try {
                    result.put(toUser.toString(), true);
                } catch (Exception e) {
                }
            }

            for (Long mouser : toUserIds) {
                try {
                    result.putIfAbsent(mouser.toString(), false);
                } catch (Exception e) {
                }
            }
        }
        return result;
    }
}