package com.zhangzc.sharethingfrontimpl.service;

import com.zhangzc.sharethingscommon.pojo.dto.LabelDTO;
import com.zhangzc.sharethingscommon.pojo.dto.LabelSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;

public interface LabelService {
    PageResponse<LabelDTO> getList(LabelSearchDTO labelSearchDTO);
}
