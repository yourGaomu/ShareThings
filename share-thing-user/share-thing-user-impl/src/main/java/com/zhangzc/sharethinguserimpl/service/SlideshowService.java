package com.zhangzc.sharethinguserimpl.service;


import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import com.zhangzc.sharethingscommon.utils.R;

import java.util.List;

/**
 * @author maliangnansheng
 * @date 2022/4/6 14:33
 */
public interface SlideshowService {

    /**
     * 获取轮播图信息
     *
     * @return
     */
    R<List<SlideshowDTO>> getList();

}
