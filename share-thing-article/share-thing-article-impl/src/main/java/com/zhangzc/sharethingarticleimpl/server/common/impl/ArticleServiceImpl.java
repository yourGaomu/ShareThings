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
import com.zhangzc.sharethingcommentapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcommentapi.rpc.commentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;
import com.zhangzc.sharethingcountapi.rpc.commentAndLike4Article;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
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

import java.util.*;
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
    private likeCount likeCount;
    @DubboReference
    private followCount followCount;
    @DubboReference
    private commentSearch commentSearch;
    @DubboReference
    private commentAndLike4Article commentAndLike4Article;


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
            } else {
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
                if (articlePage != null) {
                    //查到了数据
                    List<FsArticle> records = articlePage.getRecords();
                    //收集文章id
                    List<Integer> list = records.stream().map(FsArticle::getId).toList();
                    articleIds.addAll(list);
                    CompletableFuture.runAsync(() -> {
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
                        //todo
                        Map<Long, Map<String, Long>> articleLikeAndCommentNumbersByArticleIds = commentAndLike4Article.getArticleLikeAndCommentNumbersByArticleIds(articleIdStrings);







                    });

                    CompletableFuture<Boolean> userInfoFurture = CompletableFuture.supplyAsync(() -> {
                        //去查询用户信息
                        records.forEach(fsArticle -> {
                            //查询用户信息
                        });
                        return true;
                    }, threadPoolTaskExecutor);

                    CompletableFuture<List<MgArticle>> uCompletableFuture = CompletableFuture.supplyAsync(() -> {
                        //查询一些信息
                        //先去mongo寻找
                        Query query = new Query();
                        query.addCriteria(Criteria.where("articleId").in(articleIds));
                        return mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
                    }, threadPoolTaskExecutor);
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

    @Override
    public R<Boolean> deleteArticleById(Integer id) {
        //获取当前的
        String userId = GlobalContext.get().toString();
        FsArticle one = fsArticleService.lambdaQuery().eq(FsArticle::getId, id).one();
        if (one == null)
            return R.ok(false);
        Integer isDeleted = one.getIs_deleted();
        if (isDeleted == 1) {
            return R.ok(false);
        }
        if (!one.getCreate_user().equals(Long.valueOf(userId))) {
            return R.ok(false);
        }
        fsArticleService.lambdaUpdate()
                .eq(FsArticle::getId, id)
                .set(FsArticle::getUpdate_user, Long.valueOf(userId))
                .set(FsArticle::getUpdate_time, new Date())
                .set(FsArticle::getIs_deleted, 1).update();
        return R.ok(true);
    }

    @Override
    public R<Boolean> articleTop(Integer id, Boolean top) {
        //获取当前的
        String userId = GlobalContext.get().toString();
        FsArticle one = fsArticleService.lambdaQuery().eq(FsArticle::getId, id).one();
        if (one == null)
            return R.ok(false);
        Integer isDeleted = one.getIs_deleted();
        if (isDeleted == 1) {
            return R.ok(false);
        }
        if (!one.getCreate_user().equals(Long.valueOf(userId))) {
            return R.ok(false);
        }
        fsArticleService.lambdaUpdate()
                .eq(FsArticle::getId, id)
                .set(FsArticle::getUpdate_user, Long.valueOf(userId))
                .set(FsArticle::getUpdate_time, new Date())
                .set(FsArticle::getTop, top ? one.getTop() + 1 : 0)
                .update();
        return R.ok(true);
    }

    @Override
    public R<ArticleCheckCountDTO> getArticleCheckCount(String title) {
        ArticleCheckCountDTO result = new ArticleCheckCountDTO();
        List<FsArticle> list = fsArticleService.lambdaQuery().eq(FsArticle::getIs_deleted, 0).list();
        Map<Integer, Long> collect = list.stream().collect(Collectors.groupingBy(FsArticle::getState, Collectors.counting()));
        result.setEnableCount(collect.getOrDefault(1, 0L));
        result.setDisabledCount(collect.getOrDefault(0, 0L));
        result.setPendingReviewCount(collect.getOrDefault(-1, 0L));
        return R.ok(result);
    }

    @Override
    public R<TotalDTO> getArticleCommentVisitTotal() {
        TotalDTO result = new TotalDTO();
        //todo 优化
        //文章数量
        int size = fsArticleService.lambdaQuery().list().size();
        //评论数量
        Long commentNumbers = commentSearch.getCommentNumbers().get(articleCommentAndLike.commentNumbers);
        result.setArticleCount((long) size);
        result.setCommentCount(commentNumbers);
        return R.ok(result);
    }

    @Override
    public R<ArticleCountDTO> getCountById(Integer articleId) {
        String userID = GlobalContext.get().toString();
        ArticleCountDTO result = new ArticleCountDTO();
        //查询是否点赞
        //查询是否关注
        commentAndLike4Article.getLikeAndFollowByArticleIdAndUserId(List.of(articleId.toString())
                , List.of(userID)).forEach((k, v) -> {
            result.setIsLike(v.get(LikeAndFollowEnums.isLike));
            result.setIsFollow(v.get(LikeAndFollowEnums.isFollow));
        });
        //查询点赞数量
        //查询评论数量
        Map<Long, Map<String, Long>> articleLikeAndCommentNumbersByArticleIds = commentAndLike4Article.getArticleLikeAndCommentNumbersByArticleIds(List.of(articleId.toString()));
        articleLikeAndCommentNumbersByArticleIds
                .get(Long.valueOf(articleId)).forEach((k, v) -> {
                    if (k.equals(articleCommentAndLike.commentNumber)) {
                        result.setCommentCount(v);
                    }
                    if (k.equals(articleCommentAndLike.likeNumber)) {
                        result.setLikeCount(v);
                    }
                });
        return R.ok(result);
    }

    @Override
    public PageResponse<ArticleDTO> getLikesArticle(LikeSearchDTO likeSearchDTO) {
        String userID = GlobalContext.get().toString();
        if (likeSearchDTO.getLikeUser() == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        //获取文章id
        Integer articleId = likeSearchDTO.getArticleId();
        if (articleId != null) {
            //todo
        }
        //获取当前的页数
        Integer currentPage = likeSearchDTO.getCurrentPage();
        //获取每页的数量
        Integer pageSize = likeSearchDTO.getPageSize();
        List<FsLikeDto> likeCountByLikeUser = likeCount.getLikeCountByLikeUser(currentPage, pageSize, likeSearchDTO.getLikeUser());
        if (likeCountByLikeUser.isEmpty()) {
            return PageResponse.success(Collections.emptyList(), currentPage, 0);
        }
        //获取文章ID
        List<Integer> articleIds = likeCountByLikeUser.stream().map(FsLikeDto::getArticle_id).toList();
        //去查询文章信息


    }


    private

}

