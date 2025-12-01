package com.zhangzc.sharethingarticleimpl.mapper;

import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 吃饭
* @description 针对表【fs_article_label(文章标签)】的数据库操作Mapper
* @createDate 2025-11-26 16:50:48
* @Entity generator.domain.FsArticleLabel
*/
public interface FsArticleLabelMapper extends BaseMapper<FsArticleLabel> {

    void saveBatchByArticleIdAndLabelIds(@Param("id") Integer id, @Param("labelIds") List<Integer> labelIds, @Param("userId") String userId);
}




