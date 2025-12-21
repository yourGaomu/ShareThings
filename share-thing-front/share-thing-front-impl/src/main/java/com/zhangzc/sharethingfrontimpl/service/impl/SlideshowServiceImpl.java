package com.zhangzc.sharethingfrontimpl.service.impl;

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.redisspringbootstart.utills.LuaUtil;
import com.zhangzc.sharethingfrontimpl.consts.SlideshowConst;
import com.zhangzc.sharethingfrontimpl.pojo.domain.FsSlideshow;
import com.zhangzc.sharethingfrontimpl.service.FsSlideshowService;
import com.zhangzc.sharethingfrontimpl.service.SlideshowService;
import com.zhangzc.sharethingscommon.pojo.dto.SlideshowDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class SlideshowServiceImpl implements SlideshowService {
    private final FsSlideshowService fsSlideshowService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final LuaUtil luaUtil;

    @Override
    public List<SlideshowDTO> getList() {
        List<SlideshowDTO> result = new ArrayList<>();
        // 从redis里面获取
        String slideshow = SlideshowConst.SLIDESHOW;
        Object execute = luaUtil.execute("get_all_Slideshow", slideshow, null);
        
        // 检查Redis数据是否存在且非空
        if (execute != null && !"{}".equals(execute.toString())) {
            try {
                // 方式1: 如果Redis返回的是Map对象,直接强转
                if (execute instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) execute;

                    List<SlideshowDTO> finalResult = result;
                    dataMap.forEach((key, value) -> {
                        try {
                            // 将每个value转换为JSON字符串,再反序列化为FsSlideshow
                            String json = JsonUtils.toJsonString(value);
                            FsSlideshow fsSlideshow = JsonUtils.parseObject(json, FsSlideshow.class);
                            
                            SlideshowDTO slideshowDTO = new SlideshowDTO();
                            BeanUtils.copyProperties(fsSlideshow, slideshowDTO);
                            finalResult.add(slideshowDTO);
                        } catch (Exception e) {
                            log.warn("解析Redis中的轮播图数据失败, key: {}, error: {}", key, e.getMessage());
                        }
                    });
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (Exception e) {
                log.error("从Redis解析轮播图数据失败,将从数据库查询", e);
                // 发生异常时继续执行,从数据库查询
            }
        }
        //查数据库
        List<FsSlideshow> list = fsSlideshowService.lambdaQuery()
                .eq(FsSlideshow::getIsDeleted, 0)
                .eq(FsSlideshow::getState, 1)
                .list();
        result = list.stream().map(sign -> {
            SlideshowDTO slideshowDTO = new SlideshowDTO();
            BeanUtils.copyProperties(sign,slideshowDTO );
            return slideshowDTO;
        }).toList();
        //存入redis
        CompletableFuture.runAsync(() -> {
            List<Object> data = new ArrayList<>();
            list.forEach(fsSlideshow -> {
                data.add(fsSlideshow.getId());
                data.add(fsSlideshow);
            });
            Object execute1 = luaUtil.execute("add_all_Slideshow", slideshow, data);
            System.out.println(execute1);
        }, threadPoolTaskExecutor);

        return result;
    }
}
