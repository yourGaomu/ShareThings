package com.zhangzc.sharethingarticleimpl.server;

import com.zhangzc.sharethingarticleimpl.pojo.req.GetArticleInfoVo;
import com.zhangzc.sharethingarticleimpl.pojo.req.LikeSearchVo;
import com.zhangzc.sharethingscommon.enums.ArticleStateEnum;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface ArticleService {
    ArticleDTO create(MultipartFile picture, ArticleDTO articleDTO, List<Integer> labelIds);

    Boolean update(MultipartFile picture, ArticleDTO articleDTO, List<Integer> labelIds);

    PageResponse<ArticleDTO> getList(ArticleSearchDTO articleSearchDTO);

    R<Boolean> deleteArticleById(Integer id);

    R<Boolean> articleTop(Integer id, Boolean top);

    R<ArticleCheckCountDTO> getArticleCheckCount(String title);

    R<TotalDTO> getArticleCommentVisitTotal();

    R<ArticleCountDTO> getCountById(Integer id);

    PageResponse<ArticleDTO> getLikesArticle(LikeSearchVo likeSearchVo) throws ExecutionException
            , InterruptedException;

    R<ArticleDTO> getArticleByLabelId(GetArticleInfoVo getArticleInfoVo) throws ExecutionException, InterruptedException;

    PageResponse<ArticleDTO> getPersonalArticles(ArticleSearchDTO articleSearchDTO, ArticleStateEnum articleStateEnum);

}
