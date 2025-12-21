package com.zhangzc.sharethingarticleimpl.server;

import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 吃饭
* @description 针对表【fs_article_label(文章标签)】的数据库操作Service
* @createDate 2025-11-26 16:50:48
*/
public interface FsArticleLabelService extends IService<FsArticleLabel> {

    void saveBatchByArticleIdAndLabelIds(Integer id, List<Integer> labelIds, String userId);
}
