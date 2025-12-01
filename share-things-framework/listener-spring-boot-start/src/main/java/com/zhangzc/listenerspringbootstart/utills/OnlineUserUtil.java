package com.zhangzc.listenerspringbootstart.utills;


import com.zhangzc.listenerspringbootstart.service.OnlineUserCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Component
@RequiredArgsConstructor
public class OnlineUserUtil {

    private final OnlineUserCount onlineUserCount;

    public void addOnlineCount(String userId) {
        onlineUserCount.addOnlineCount(userId);
    }

    public void subOnlineCount(String userId) {
        onlineUserCount.subOnlineCount(userId);
    }
}
