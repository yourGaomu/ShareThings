package com.zhangzc.listenerspringbootstart.service;

public interface OnlineUserCount {

    Long addOnlineCount(String userId);
    Long addOnlineCount(String userId,String userIp);
    Long subOnlineCount(String userId);
    Long getOnlineCount();
}
