package com.zhangzc.sharethingfrontimpl.controller;


import com.zhangzc.sharethingfrontimpl.service.ResourceNavigateService;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bbs/resource/")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceNavigateService resourceNavigateService;


    @PostMapping("getList")
    public R<PageResponse<ResourceNavigateDTO>> getList(@RequestBody ResourceNavigateSearchDTO resourceNavigateSearchDTO) {
        PageResponse<ResourceNavigateDTO> result = resourceNavigateService.getList(resourceNavigateSearchDTO);
        return R.ok(result);
    }

}
