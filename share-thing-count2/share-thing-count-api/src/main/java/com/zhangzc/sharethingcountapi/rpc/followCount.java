package com.zhangzc.sharethingcountapi.rpc;

import com.zhangzc.sharethingcountapi.pojo.dto.FsFollowDto;

import java.util.List;
import java.util.Map;

public interface followCount {

    List<FsFollowDto> getFollowCountByUserId(String userId);

    Map<String,Boolean> getFollowCountByUserIdAndToUserId(List<Long> toUserIds, Long fromUserId);
}
