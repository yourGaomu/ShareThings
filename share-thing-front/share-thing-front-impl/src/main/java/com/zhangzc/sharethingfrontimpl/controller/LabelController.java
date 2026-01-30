package com.zhangzc.sharethingfrontimpl.controller;

import com.zhangzc.sharethingfrontimpl.service.LabelService;
import com.zhangzc.sharethingscommon.pojo.dto.LabelDTO;
import com.zhangzc.sharethingscommon.pojo.dto.LabelSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bbs/label/")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;


    @PostMapping("getList")
    public R<PageResponse<LabelDTO>> getList(@RequestBody LabelSearchDTO labelSearchDTO) {
        PageResponse<LabelDTO> result = labelService.getList(labelSearchDTO);

        return R.ok("查找成功",result);
    }





}
