package com.zhangzc.sharethingfrontimpl.service;

import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;

public interface ResourceNavigateService {
    PageResponse<ResourceNavigateDTO> getList(ResourceNavigateSearchDTO resourceNavigateSearchDTO);
}
