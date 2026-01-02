package com.zhangzc.sharethingnotifyimpl.service;

import com.zhangzc.sharethingnotifyimpl.pojo.domain.FsNotify;
import com.zhangzc.sharethingnotifyimpl.pojo.mongo.Notification;
import com.zhangzc.sharethingnotifyimpl.pojo.vo.NotifySearchVo;
import com.zhangzc.sharethingscommon.utils.PageResponse;

public interface NotifyService {
    PageResponse<Notification> getList(NotifySearchVo notifySearchDTO);
}
