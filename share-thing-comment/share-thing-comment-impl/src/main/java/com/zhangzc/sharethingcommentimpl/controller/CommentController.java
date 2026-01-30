package com.zhangzc.sharethingcommentimpl.controller;

import com.zhangzc.sensitivewordspringbootstart.utills.SensitiveWordUtil;
import com.zhangzc.sharethingcommentimpl.service.CommentService;
import com.zhangzc.sharethingscommon.pojo.dto.CommentDTO;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@RestController
@RequestMapping("/bbs/comment/")
public class CommentController {
    private final CommentService commentService;
    @Autowired(required = false)
    private SensitiveWordUtil sensitiveWordUtil;

    @PostMapping("create")
    public R<Boolean> create(@RequestBody CommentDTO commentDTO) {
        String content = commentDTO.getContent();
        if (content == null) {
            return R.ok("评论成功");
        } else {
            //过滤后的评论
            String s = sensitiveWordUtil.replaceSensitiveWord(content);
            CompletableFuture.runAsync(() -> {
                commentService.creat(commentDTO, s);
            });
        }
        return R.ok();
    }





}
