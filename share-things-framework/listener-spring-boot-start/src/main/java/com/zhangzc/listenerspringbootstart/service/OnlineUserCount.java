package com.zhangzc.listenerspringbootstart.service;

public interface OnlineUserCount {

    Long addOnlineCount(String userId);
    Long subOnlineCount(String userId);
}
