package com.zhangzc.sharethingfrontimpl.controller;


import com.zhangzc.sharethingfrontimpl.service.SlideshowService;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateSearchDTO;
import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/bbs/carousel/")
public class CarouselController {

    private final SlideshowService slideshowService;


    @PostMapping("getList")
    public R<List<SlideshowDTO>> getList() {
        return R.ok(slideshowService.getList());
    }




}
