package com.zhangzc.sharethinguserimpl.service;

import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import com.zhangzc.sharethinguserimpl.pojo.req.ArticleQueryRequestDto;

import java.util.List;

public interface HistoryService {
    public List<ArticleDTO> getHistory(ArticleQueryRequestDto articleQueryRequestDto);

    Boolean clearHistory(String id);

    Boolean clearAllHistory();
}
