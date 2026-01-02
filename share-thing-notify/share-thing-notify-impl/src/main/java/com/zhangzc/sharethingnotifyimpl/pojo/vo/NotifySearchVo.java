package com.zhangzc.sharethingnotifyimpl.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifySearchVo {
    /**
     * 0 : 任务提醒 1 : 系统通知
    * */
    private Integer type;
    private Integer current;
    private Integer size;
    private Boolean isRead;
}
