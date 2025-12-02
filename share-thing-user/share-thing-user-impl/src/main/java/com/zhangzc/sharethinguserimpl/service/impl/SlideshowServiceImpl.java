package com.zhangzc.sharethinguserimpl.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsSlideshow;
import com.zhangzc.sharethinguserimpl.service.FsSlideshowService;
import com.zhangzc.sharethinguserimpl.service.SlideshowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author maliangnansheng
 * @date 2022/4/6 14:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlideshowServiceImpl implements SlideshowService {

    private final FsSlideshowService fsSlideshowService;


    @Override
    public R<List<SlideshowDTO>> getList() {
        List<FsSlideshow> list = fsSlideshowService.lambdaQuery().list();
        List<SlideshowDTO> slideshowDTOS = new ArrayList<>();
        list.stream().forEach(fsSlideshow -> {
            SlideshowDTO slideshowDTO = new SlideshowDTO();
            BeanUtil.copyProperties(fsSlideshow, slideshowDTO);
            slideshowDTOS.add(slideshowDTO);
        });
        return R.ok(slideshowDTOS);
    }

}
