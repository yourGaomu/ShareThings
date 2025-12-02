package com.zhangzc.sharethingarticleimpl.controller;


import com.zhangzc.sharethingarticleimpl.server.common.ArticleService;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/bbs/article/")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/create")
    public R<Boolean> create(@RequestParam(value = "file", required = false) MultipartFile picture,
                             ArticleDTO articleDTO, @RequestParam List<Integer> labelIds) throws IOException {
        articleService.create(picture, articleDTO, labelIds);
        return R.ok();
    }

    @GetMapping("getList")
    public R<PageResponse<ArticleDTO>> getList(@RequestBody ArticleSearchDTO articleSearchDTO){
        PageResponse<ArticleDTO> result =  articleService.getList(articleSearchDTO);
        return R.ok(result);
    }


    @PostMapping("delete/{id}")
    public R<Boolean> delete(@PathVariable Integer id) {
        return articleService.deleteArticleById(id);
    }

    @GetMapping("articleTop")
    public R<Boolean> articleTop(@RequestParam Integer id, @RequestParam Boolean top) {
        return articleService.articleTop(id, top);
    }

    @GetMapping("getArticleCheckCount")
    public R<ArticleCheckCountDTO> getArticleCheckCount(@RequestParam(required = false) String title) {
        return articleService.getArticleCheckCount(title);
    }

    @GetMapping("getArticleCommentVisitTotal")
    public R<TotalDTO> getArticleCommentVisitTotal() {
        return articleService.getArticleCommentVisitTotal();
    }

    @GetMapping("getCountById")
    public R<ArticleCountDTO> getCountById(@RequestParam Integer id) {
        return articleService.getCountById(id);
    }

    @GetMapping("getLikesArticle")
    public R<PageResponse<ArticleDTO>> getLikesArticle(LikeSearchDTO likeSearchDTO) {
        PageResponse<ArticleDTO> result = articleService.getLikesArticle(likeSearchDTO);
        return R.ok(result);
    }


}
