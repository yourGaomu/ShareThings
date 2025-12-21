package com.zhangzc.sharethinguserimpl.canal.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValueAndFlagVo {
    private Object value;
    private boolean updated;

}
