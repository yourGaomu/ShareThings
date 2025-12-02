package com.zhangzc.sharethingarticleimpl.server.common;

import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArticleService {
    void create(MultipartFile picture, ArticleDTO articleDTO, List<Integer> labelIds);

    PageResponse<ArticleDTO> getList(ArticleSearchDTO articleSearchDTO);

    R<Boolean> deleteArticleById(Integer id);

    R<Boolean> articleTop(Integer id, Boolean top);

    R<ArticleCheckCountDTO> getArticleCheckCount(String title);

    R<TotalDTO> getArticleCommentVisitTotal();

    R<ArticleCountDTO> getCountById(Integer id);

    PageResponse<ArticleDTO> getLikesArticle(LikeSearchDTO likeSearchDTO);
}
