package com.zhangzc.sharethingarticleimpl.controller;


import com.zhangzc.miniospringbootstart.utills.MinioUtil;
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil;
import com.zhangzc.sharethingarticleimpl.interfaces.GetArticleInfodAddPV;
import com.zhangzc.sharethingarticleimpl.pojo.mongoDomain.MgArticle;
import com.zhangzc.sharethingarticleimpl.pojo.req.GetArticleInfoVo;
import com.zhangzc.sharethingarticleimpl.pojo.req.LikeSearchVo;
import com.zhangzc.sharethingarticleimpl.server.ArticleService;
import com.zhangzc.sharethingscommon.enums.ArticleStateEnum;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/bbs/article/")
@RequiredArgsConstructor

public class ArticleController {
    private final MongoUtil mongoUtil;
    private final MinioUtil minioUtil;
    private final ArticleService articleService;

    /**
     * 上传图片
     *
     * @param picture
     * @return
     * @throws Exception
     */


    @PostMapping("/uploadPicture")
    public R<String> uploadPicture(@RequestParam(value = "file", required = false) MultipartFile picture) throws Exception {
        return R.ok("成功", minioUtil.uploadFile(picture));
    }

    @PostMapping("/create")
    public R<Boolean> create(@RequestParam(value = "file", required = false) MultipartFile picture,
                             @RequestBody ArticleDTO articleDTO,
                             @RequestBody List<Integer> labelIds) {
        articleService.create(picture, articleDTO, labelIds);
        return R.ok();
    }

    @PostMapping("getList")
    public R<PageResponse<ArticleDTO>> getList(@RequestBody ArticleSearchDTO articleSearchDTO) {
        PageResponse<ArticleDTO> result = articleService.getList(articleSearchDTO);
        return R.ok(result);
    }

    @PostMapping("delete/{id}")
    public R<Boolean> delete(@PathVariable Integer id) {
        return articleService.deleteArticleById(id);
    }

    @PostMapping("articleTop")
    public R<Boolean> articleTop(@RequestParam Integer id, @RequestParam Boolean top) {
        return articleService.articleTop(id, top);
    }

    @PostMapping("getArticleCheckCount")
    public R<ArticleCheckCountDTO> getArticleCheckCount(@RequestParam(required = false) String title) {
        return articleService.getArticleCheckCount(title);
    }

    @PostMapping("getArticleCommentVisitTotal")
    public R<TotalDTO> getArticleCommentVisitTotal() {
        return articleService.getArticleCommentVisitTotal();
    }

    @PostMapping("getCountById")
    public R<ArticleCountDTO> getCountById(@RequestParam Integer id) {
        return articleService.getCountById(id);
    }


    @PostMapping("getLikesArticle")
    public R<PageResponse<ArticleDTO>> getLikesArticle(@RequestBody LikeSearchVo likeSearchVo) throws ExecutionException, InterruptedException {
        PageResponse<ArticleDTO> result = articleService.getLikesArticle(likeSearchVo);
        return R.ok(result);
    }

    @PostMapping("getById")
    @GetArticleInfodAddPV() // 移除双引号，规范SpEL表达式
    public R<ArticleDTO> getArticleByLabelId(@RequestBody GetArticleInfoVo getArticleInfoVo) throws ExecutionException, InterruptedException {
        return articleService.getArticleByLabelId(getArticleInfoVo);
    }

    @PostMapping("/getPersonalArticles")
    public R<PageResponse<ArticleDTO>> getPersonalArticles(@RequestBody ArticleSearchDTO articleSearchDTO
            , @RequestBody(required = false) ArticleStateEnum articleStateEnum) {
        PageResponse<ArticleDTO> result = articleService.getPersonalArticles(articleSearchDTO, articleStateEnum);
        return R.ok(result);
    }

    @PostMapping("/test")
    public void text() {
        Query query = new Query();
        // ✅ 使用Integer类型,与MongoDB字段类型一致
        List<Integer> articleIds = List.of(28);
        query.addCriteria(Criteria.where("articleId").in(articleIds));

        // 打印查询条件
        System.out.println("查询条件: " + query);

        List<MgArticle> bbsArticleMarkdownInfo = mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
        System.out.println("查询结果数量: " + bbsArticleMarkdownInfo.size());
        System.out.println("查询结果: " + bbsArticleMarkdownInfo);

        // 查看所有数据用于调试
        Query allQuery = new Query();
        List<MgArticle> allData = mongoUtil.find(allQuery, MgArticle.class, "bbs_article_markdown_info");
        System.out.println("MongoDB总数据量: " + allData.size());
    }

}
