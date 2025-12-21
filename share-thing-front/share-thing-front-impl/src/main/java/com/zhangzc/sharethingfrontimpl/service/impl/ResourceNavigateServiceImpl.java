package com.zhangzc.sharethingfrontimpl.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingfrontimpl.consts.ResourceNavigateConst;
import com.zhangzc.sharethingfrontimpl.pojo.domain.FsResourceNavigate;
import com.zhangzc.sharethingfrontimpl.service.ResourceNavigateService;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ResourceNavigateSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ResourceNavigateServiceImpl implements ResourceNavigateService {

    private final FsResourceNavigateServiceImpl fsResourceNavigateServiceImpl;
    private final RedisUtil redisUtil;


    @Override
    public PageResponse<ResourceNavigateDTO> getList(ResourceNavigateSearchDTO resourceNavigateSearchDTO) {
        String category = resourceNavigateSearchDTO.getCategory();
        String rediskey;
        if (category == null) {
            rediskey = ResourceNavigateConst.RESOURCE_NAVIGATE_NONECATEGORY;
        } else {
            rediskey = ResourceNavigateConst.RESOURCE_NAVIGATE_CATEGORY + category.toUpperCase();
        }
        int pageSize = resourceNavigateSearchDTO.getPageSize() == null || resourceNavigateSearchDTO.getPageSize() <= 1 ? 32 : resourceNavigateSearchDTO.getPageSize();

        int currentPage = resourceNavigateSearchDTO.getCurrentPage() == null || resourceNavigateSearchDTO.getCurrentPage() <= 1 ? 1 : resourceNavigateSearchDTO.getCurrentPage();

        IPage<FsResourceNavigate> page = new Page<>(currentPage, pageSize);
        // 先去redis查询
        Object data = redisUtil.hmget(rediskey, Integer.toString(currentPage));
        if (data != null) {
            try {
                // 方式1: 如果Redis返回的是List<Map>结构
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rawList = (List<Object>) data;

                    List<ResourceNavigateDTO> list = rawList.stream()
                            .map(item -> {
                                try {
                                    // 将Map转换为JSON字符串,再反序列化为对象
                                    String json = JsonUtils.toJsonString(item);
                                    FsResourceNavigate navigate = JsonUtils.parseObject(json, FsResourceNavigate.class);

                                    ResourceNavigateDTO dto = new ResourceNavigateDTO();
                                    BeanUtils.copyProperties(navigate, dto);
                                    return dto;
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                            .filter(dto -> dto != null)
                            .toList();

                    if (!list.isEmpty()) {
                        return PageResponse.success(list, currentPage, list.size());
                    }
                }
            } catch (Exception e) {
                // Redis数据解析失败,降级到数据库查询
                System.err.println("Redis数据解析失败: " + e.getMessage());
            }
        }
        //去数据库查询
        IPage<FsResourceNavigate> fsResourceNavigateIPage = fsResourceNavigateServiceImpl
                .lambdaQuery()
                .eq(FsResourceNavigate::getIsDeleted, 0)
                .eq(category != null, FsResourceNavigate::getCategory, category)
                .page(page);
        if (fsResourceNavigateIPage.getRecords() == null) {
            return PageResponse.success(Collections.emptyList(), currentPage, 0);
        }
        List<FsResourceNavigate> records = fsResourceNavigateIPage.getRecords();
        List<ResourceNavigateDTO> list = records.stream().map(sign -> {
            ResourceNavigateDTO resourceNavigateDTO = new ResourceNavigateDTO();
            BeanUtils.copyProperties(sign, resourceNavigateDTO);
            return resourceNavigateDTO;
        }).toList();
        // 异步存入Redis
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, List<FsResourceNavigate>> redisMap = new HashMap<>();
                redisMap.put(String.valueOf(currentPage), records);
                redisUtil.hmsetGeneric(rediskey, redisMap);
            } catch (Exception e) {
                System.err.println("Redis缓存写入失败: " + e.getMessage());
            }
        });
        return PageResponse.success(list, currentPage, fsResourceNavigateIPage.getTotal());
    }


}

