package com.zhangzc.sharethinguserimpl.controller;


import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import com.zhangzc.sharethingscommon.utils.R;

import com.zhangzc.sharethinguserimpl.service.SlideshowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Reference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bbs/carousel/")
public class SlideshowController {
    private final SlideshowService slideshowService;


    @PostMapping("getList")
    public R<List<SlideshowDTO>> getList() {
        return slideshowService.getList();
    }

}
