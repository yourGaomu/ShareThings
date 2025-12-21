package com.zhangzc.listenerspringbootstart.utills;


import com.zhangzc.listenerspringbootstart.service.OnlineUserCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RequiredArgsConstructor
@Component
public class OnlineUserUtil {

    private final OnlineUserCount onlineUserCount;

    public Long addOnlineCount(String userId) {
        return onlineUserCount.addOnlineCount(userId);
    }

    public Long addOnlineCount(String userId,String userIp) {
        return onlineUserCount.addOnlineCount(userId,userIp);
    }


    public Long subOnlineCount(String userId) {
        return onlineUserCount.subOnlineCount(userId);
    }

    public Long getOnlineCount() {
        return onlineUserCount.getOnlineCount();
    }



}
