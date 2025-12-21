package com.zhangzc.sharethingcommentimpl.controller;

import com.zhangzc.sensitivewordspringbootstart.utills.SensitiveWordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
public class test {
    
    @Autowired(required = false)
    private SensitiveWordUtil sensitiveWordUtil;

    @PostMapping("/sensitive")
    public String testSensitive(@RequestBody String str) {
        log.info("测试敏感词过滤，输入: {}", str);
        if (sensitiveWordUtil == null) {
            log.error("SensitiveWordUtil 注入失败！");
            return "SensitiveWordUtil is null";
        }
        String result = sensitiveWordUtil.replaceSensitiveWord(str);
        log.info("过滤后结果: {}", result);
        return result;
    }
}
