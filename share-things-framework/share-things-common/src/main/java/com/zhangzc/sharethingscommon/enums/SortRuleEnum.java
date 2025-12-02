package com.zhangzc.sharethingscommon.enums;

import lombok.Getter;


@Getter
public enum SortRuleEnum {
    /**
     * 最热
     */
    hottest("最热"),
    newest("最新");

    /**
     * 说明
     */
    private String desc;

    SortRuleEnum(String name) {
        this.desc = name;
    }

}
