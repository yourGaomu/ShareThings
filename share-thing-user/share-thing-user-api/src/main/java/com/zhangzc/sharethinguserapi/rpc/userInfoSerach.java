package com.zhangzc.sharethinguserapi.rpc;

import com.zhangzc.sharethinguserapi.pojo.dto.FsUserInfoDto;

import java.util.List;
import java.util.Map;

public interface userInfoSerach {

    Map<String, FsUserInfoDto> getUserInfoByUserId(List<String> userId);
}
