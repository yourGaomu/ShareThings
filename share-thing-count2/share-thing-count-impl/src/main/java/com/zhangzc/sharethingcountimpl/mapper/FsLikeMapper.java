package com.zhangzc.sharethingcountimpl.mapper;

import com.zhangzc.sharethingcountimpl.pojo.domain.FsLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 吃饭
* @description 针对表【fs_like(点赞)】的数据库操作Mapper
* @createDate 2025-11-27 21:51:36
* @Entity generator.domain.FsLike
*/
public interface FsLikeMapper extends BaseMapper<FsLike> {

    Boolean saveFslike(@Param("fsLike") FsLike fsLike);
}




