package com.zhangzc.sharethinguserimpl.controller;

import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.pojo.req.ArticleQueryRequestDto;
import com.zhangzc.sharethinguserimpl.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bbs/history")
@RequiredArgsConstructor
public class UserHistoryController {
    private final HistoryService historyService;

    @PostMapping("/list")
    public R<List<ArticleDTO>> getHistory(ArticleQueryRequestDto articleQueryRequestDto) {
        List<ArticleDTO> result =  historyService.getHistory(articleQueryRequestDto);
        return R.ok("查询成功",result);
    }

    @PostMapping("/delete")
    public R<Boolean> clearHistory(@RequestBody Map<String, String> map) {
        Boolean result =  historyService.clearHistory(map.get("id"));
        return R.ok("清空成功",result);
    }

    @PostMapping("/clear")
    public  R<Boolean> clearAllHistory() {
        Boolean result =  historyService.clearAllHistory();
        return R.ok("清空成功",result);
    }

}
