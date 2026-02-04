package com.zhangzc.sharethingnotifyimpl.controller;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.sharethingnotifyimpl.pojo.mongo.Notification;
import com.zhangzc.sharethingnotifyimpl.pojo.vo.NotifySearchVo;
import com.zhangzc.sharethingnotifyimpl.service.NotifyService;
import com.zhangzc.sharethingnotifyimpl.service.impl.NotifyServiceImpl;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 * 提供通知的CRUD接口
 */
@RestController
@RequestMapping("/bbs/notify/")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    /**
     * 分页查询通知列表
     * @param notifySearchVo 查询条件
     * @return 通知列表
     */
    @PostMapping("getList")
    public R<PageResponse<Notification>> getList(@RequestBody NotifySearchVo notifySearchVo) {
        PageResponse<Notification> result = notifyService.getList(notifySearchVo);
        return R.ok(result);
    }

    /**
     * 获取当前用户未读通知数量
     * @return 未读数量
     */
    @PostMapping("getUnreadCount")
    public R<Long> getUnreadCount() {
        Long userId = Long.valueOf(GlobalContext.get().toString());
        Long count = notifyService.getUnreadCount(userId);
        return R.ok(count);
    }

    /**
     * 标记单个通知为已读
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @PostMapping("markAsRead")
    public R<Boolean> markAsRead(@RequestParam String notificationId) {
        Long userId = Long.valueOf(GlobalContext.get().toString());
        Boolean result = notifyService.markAsRead(notificationId, userId);
        return R.ok(result);
    }

    /**
     * 标记所有通知为已读
     * @return 操作结果
     */
    @PostMapping("markAllAsRead")
    public R<Boolean> markAllAsRead() {
        Long userId = Long.valueOf(GlobalContext.get().toString());
        Boolean result = notifyService.markAllAsRead(userId);
        return R.ok(result);
    }

    /**
     * 创建新通知（仅管理员使用）
     * @param notification 通知对象
     * @return 创建结果
     */
    @PostMapping("create")
    public R<Notification> createNotification(@RequestBody Notification notification) {
        Notification created = notifyService.createNotification(notification);
        return R.ok(created);
    }
}
