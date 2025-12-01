package com.zhangzc.sharethingarticleimpl.controller;


import com.zhangzc.sharethingarticleimpl.server.common.ArticleService;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleSearchDTO;
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

}
