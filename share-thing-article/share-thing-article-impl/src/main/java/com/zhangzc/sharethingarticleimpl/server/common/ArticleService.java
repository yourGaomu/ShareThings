package com.zhangzc.sharethingarticleimpl.server.common;

import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArticleService {
    void create(MultipartFile picture, ArticleDTO articleDTO, List<Integer> labelIds);

    PageResponse<ArticleDTO> getList(ArticleSearchDTO articleSearchDTO);
}
