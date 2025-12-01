package com.zhangzc.sharethingarticleimpl.server.common.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.zhangzc.sharethingarticleimpl.pojo.mongoDomain.MgArticle;
import com.zhangzc.sharethingarticleimpl.server.common.ArticleService;
import com.zhangzc.sharethingarticleimpl.server.common.FsArticleLabelService;
import com.zhangzc.sharethingarticleimpl.server.common.FsArticleService;
import com.zhangzc.sharethingarticleimpl.server.common.FsLabelService;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleSearchDTO;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final TransactionTemplate transactionTemplate;
    private final FsArticleService fsArticleService;
    private final MongoUtil mongoUtil;
    private final FsArticleLabelService fsArticleLabelService;
    private final FsLabelService fsLabelService;
    @DubboReference
    private final likeCount likeCount;
    @DubboReference
    private final followCount followCount;

    @Qualifier("threadPoolTaskExecutor")
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Override
    public void create(MultipartFile picture, ArticleDTO articleDTO, List<Integer> labelIds) {
        if (picture != null) {
            articleDTO.setPicture(picture.getOriginalFilename());
        }
        if (StringUtils.isBlank(articleDTO.getTitle()) || StringUtils.isBlank(articleDTO.getHtml())) {
            //标题和内容不能为空
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        //当前的操作用户
        String userId = GlobalContext.get().toString();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        //开启事物
        Boolean execute = transactionTemplate.execute(status -> {
            try {
                //保存进入文章数据库
                //创建保存对象
                FsArticle fsArticle = new FsArticle();
                BeanUtils.copyProperties(articleDTO, fsArticle);
                fsArticle.setContent(articleDTO.getContent().length() > 200 ? articleDTO.getContent().substring(0, 200) : articleDTO.getContent());
                fsArticle.setCreate_user(Long.valueOf(userId));
                fsArticle.setUpdate_user(Long.valueOf(userId));
                fsArticle.setCreate_time(new Date());
                fsArticle.setUpdate_time(new Date());
                fsArticle.setIs_deleted(0);
                fsArticle.setState(0);
                fsArticle.setPv(0);
                fsArticle.setTop(0);
                fsArticleService.save(fsArticle);
                //保存进入MongoDb
                MgArticle mgArticle = new MgArticle();
                mgArticle.setArticleId(fsArticle.getId());
                mgArticle.setArticleHtml(articleDTO.getHtml());
                mgArticle.setArticleMarkdown(articleDTO.getMarkdown());
                mgArticle.setUserId(Long.valueOf(userId));
                mongoUtil.insert(mgArticle, "bbs_article_markdown_info");
                //保存进入标签数据库
                fsArticleLabelService.saveBatchByArticleIdAndLabelIds(fsArticle.getId(), labelIds, userId);
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                return false;
            }
        });
        if (Boolean.FALSE.equals(execute)) {
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public PageResponse<ArticleDTO> getList(ArticleSearchDTO articleSearchDTO) {
        Long userId = Long.valueOf(GlobalContext.get().toString());
        List<ArticleDTO> result = new ArrayList<>();
        //当前的页
        Integer currentPage = articleSearchDTO.getCurrentPage();
        //需要的数量
        Integer pageSize = articleSearchDTO.getPageSize();
        //文章Id集合
        List<Integer> articleIds = new ArrayList<>();
        if (articleSearchDTO.getLabelIds() != null && !articleSearchDTO.getLabelIds().isEmpty()) {
            //查询该标签下的文章
            List<Integer> labelIds = articleSearchDTO.getLabelIds();
            List<FsArticleLabel> list = fsArticleLabelService.lambdaQuery().in(FsArticleLabel::getLabel_id, labelIds).list();
            if (list != null && !list.isEmpty()) {
                list.forEach(fsArticleLabel -> articleIds.add(fsArticleLabel.getArticle_id()));
            }else {
                return PageResponse.success(result, currentPage, 0);
            }
        }
        //去查看数据
        Boolean execute = transactionTemplate.execute(status -> {
            try {
                Page<FsArticle> page = new Page(currentPage, pageSize);
                Page<FsArticle> articlePage = fsArticleService.lambdaQuery()
                        .in(!articleIds.isEmpty(), FsArticle::getId, articleIds)
                        .like(articleSearchDTO.getTitle() != null, FsArticle::getTitle, articleSearchDTO.getTitle())
                        .eq(articleSearchDTO.getCreateUser() != null, FsArticle::getCreate_user, articleSearchDTO.getCreateUser())
                        .page(page);
                if (articlePage != null){
                    //查到了数据
                    List<FsArticle> records = articlePage.getRecords();
                    //收集文章id
                    List<Integer> list = records.stream().map(FsArticle::getId).toList();
                    articleIds.addAll(list);
                    CompletableFuture.runAsync(()->{
                       //给结果赋值
                        records.forEach(fsArticle -> {
                            ArticleDTO articleDTO = new ArticleDTO();
                            BeanUtils.copyProperties(fsArticle, articleDTO);
                            //markdonw ,html, labelDTOS,ArticleCountDTo,picture,level
                            result.add(articleDTO);
                        });
                        //查询ArticleCountDTo
                        //是否点赞
                        List<String> articleIdStrings = articleIds.stream().map(String::valueOf).toList();
                        List<Map<Long, Boolean>> likeCountByArticleIdAndUserId = likeCount
                                .getLikeCountByArticleIdAndUserId(articleIdStrings, userId.toString());
                        //是否关注
                        List<Long> toUserIds = records.stream().map(FsArticle::getCreate_user).distinct().toList();
                        Map<String, Boolean> followCountByUserIdAndToUserId = followCount.getFollowCountByUserIdAndToUserId(toUserIds, userId);
                        //查询文章的点赞的数量，评论的数量


                    });

                    CompletableFuture<Boolean> userInfoFurture = CompletableFuture.supplyAsync(() -> {
                        //去查询用户信息
                        records.forEach(fsArticle -> {
                            //查询用户信息
                        });
                        return true;
                    },threadPoolTaskExecutor);

                    CompletableFuture<List<MgArticle>> uCompletableFuture = CompletableFuture.supplyAsync(() -> {
                        //查询一些信息
                        //先去mongo寻找
                        Query query = new Query();
                        query.addCriteria(Criteria.where("articleId").in(articleIds));
                        return mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
                    },threadPoolTaskExecutor);
                }
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                return false;
            }
        });
        if (Boolean.FALSE.equals(execute)) {
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        return PageResponse.success(result, currentPage, result.size());
    }
}

