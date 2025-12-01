package com.zhangzc.sharethingsgateway.controller;


import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethingsgateway.service.SlideshowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author maliangnansheng
 * @date 2022/4/6 14:28
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bbs/carousel/")
public class SlideshowController {
    private final SlideshowService slideshowService;


    @GetMapping("getList")
    public R<List<SlideshowDTO>> getList() {
        return slideshowService.getList();
    }

}
