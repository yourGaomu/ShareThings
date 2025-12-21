package com.zhangzc.sharethingcountapi.rpc;

import java.util.List;
import java.util.Map;

public interface pvCount {
    Map<String,Double> getPVCountByUserIds(List<String> userIds);

}
