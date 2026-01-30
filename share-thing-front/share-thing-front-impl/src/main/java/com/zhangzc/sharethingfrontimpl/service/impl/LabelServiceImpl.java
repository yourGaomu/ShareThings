package com.zhangzc.sharethingfrontimpl.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingfrontimpl.pojo.domain.FsLabel;
import com.zhangzc.sharethingfrontimpl.redisConst.RedisLabelConst;
import com.zhangzc.sharethingfrontimpl.service.FsLabelService;
import com.zhangzc.sharethingfrontimpl.service.LabelService;
import com.zhangzc.sharethingscommon.pojo.dto.LabelDTO;
import com.zhangzc.sharethingscommon.pojo.dto.LabelSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
    private final FsLabelService fsLabelService;
    private final RedisUtil redisUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public PageResponse<LabelDTO> getList(LabelSearchDTO labelSearchDTO) {
        //根据创建时间的来排序
        List<FsLabel> fsLabels;
        Long currentPage = Long.valueOf(labelSearchDTO.getCurrentPage());
        Long pageSize = Long.valueOf(labelSearchDTO.getPageSize());
        if (labelSearchDTO.getCurrentPage() <= 1) {
            currentPage = 1L;
        }
        if (labelSearchDTO.getPageSize() <= 0) {
            pageSize = 10L;
        }
        //优先redis里面查询
        String redisKey = RedisLabelConst.LABEL_REDIS;
        Object hmget = redisUtil.hmget(redisKey, currentPage.toString());
        if (hmget == null) {
            //没有查询到
            fsLabels = pageSerchByCurrentPage(currentPage, pageSize);
            Long finalCurrentPage = currentPage;
            CompletableFuture.runAsync(()->{
                //存入Redis中
                redisUtil.hset(redisKey, finalCurrentPage.toString(),JsonUtils.toJsonString(fsLabels));
            },threadPoolTaskExecutor);

        } else {
            //查询到了结果
            //进行反序列化
            try {
                fsLabels = JsonUtils.parseList((String) hmget, new TypeReference<>() {
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        //进行转换类型
        if (fsLabels != null && !fsLabels.isEmpty()) {
            //不为空
            List<LabelDTO> result = fsLabels.stream().map(fsLabel -> {
                LabelDTO labelDTO = new LabelDTO();
                BeanUtils.copyProperties(fsLabel, labelDTO);
                labelDTO.setCreateTime(TimeUtil.getLocalDateTime(fsLabel.getCreateTime()));
                labelDTO.setUpdateTime(TimeUtil.getLocalDateTime(fsLabel.getUpdateTime()));
                return labelDTO;
            }).toList();

            return PageResponse.success(result, currentPage, fsLabels.size());
        }
        return PageResponse.success(null, currentPage, 0);
    }

    private List<FsLabel> pageSerchByCurrentPage(Long currentPage, Long pageSize) {
        //分页查询
        IPage<FsLabel> result = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<FsLabel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.orderByDesc(FsLabel::getCreateTime);
        IPage<FsLabel> page = fsLabelService.page(result, lambdaQueryWrapper);
        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            //该分页下没有数据
            return  Collections.emptyList();
        }else {
            return page.getRecords();
        }

    }
}
