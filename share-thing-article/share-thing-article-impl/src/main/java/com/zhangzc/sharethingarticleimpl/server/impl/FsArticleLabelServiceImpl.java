package com.zhangzc.sharethingarticleimpl.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.zhangzc.sharethingarticleimpl.server.FsArticleLabelService;
import com.zhangzc.sharethingarticleimpl.mapper.FsArticleLabelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 吃饭
* @description 针对表【fs_article_label(文章标签)】的数据库操作Service实现
* @createDate 2025-11-26 16:50:48
*/
@Service
public class FsArticleLabelServiceImpl extends ServiceImpl<FsArticleLabelMapper, FsArticleLabel>
    implements FsArticleLabelService{

    @Override
    public void saveBatchByArticleIdAndLabelIds(Integer id, List<Integer> labelIds, String userId) {
        this.baseMapper.saveBatchByArticleIdAndLabelIds(id, labelIds, userId);
    }
}




