package com.zhangzc.sharethingarticleimpl.server.common.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil;
import com.zhangzc.redisspringbootstart.redisConst.RedisZHashConst;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsLabel;
import com.zhangzc.sharethingarticleimpl.pojo.mongoDomain.MgArticle;
import com.zhangzc.sharethingarticleimpl.server.common.ArticleService;
import com.zhangzc.sharethingarticleimpl.server.common.FsArticleLabelService;
import com.zhangzc.sharethingarticleimpl.server.common.FsArticleService;
import com.zhangzc.sharethingarticleimpl.server.common.FsLabelService;
import com.zhangzc.sharethingcommentapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcommentapi.rpc.CommentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;
import com.zhangzc.sharethingcountapi.rpc.CommentAndLike4Article;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserapi.pojo.dto.FsUserInfoDto;
import com.zhangzc.sharethinguserapi.rpc.userInfoSerach;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final TransactionTemplate transactionTemplate;
    private final FsArticleService fsArticleService;
    private final MongoUtil mongoUtil;
    private final RedisUtil redisUtil;
    private final FsArticleLabelService fsArticleLabelService;
    private final FsLabelService fsLabelService;
    @Qualifier("threadPoolTaskExecutor")
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @DubboReference(check = false)
    private likeCount likeCount;
    @DubboReference(check = false)
    private followCount followCount;
    @DubboReference(check = false)
    private CommentSearch commentSearch;
    @DubboReference(check = false)
    private CommentAndLike4Article commentAndLike4Article;
    @DubboReference(check = false)
    private userInfoSerach userInfoSerach;

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
                fsArticle.setCreateUser(Long.valueOf(userId));
                fsArticle.setUpdateUser(Long.valueOf(userId));
                fsArticle.setCreateTime(new Date());
                fsArticle.setUpdateTime(new Date());
                fsArticle.setIsDeleted(0);
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
            List<FsArticleLabel> list = fsArticleLabelService.lambdaQuery().in(FsArticleLabel::getLabelId, labelIds).list();
            if (list != null && !list.isEmpty()) {
                list.forEach(fsArticleLabel -> articleIds.add(fsArticleLabel.getArticleId()));
            } else {
                return PageResponse.success(result, currentPage, 0);
            }
        }
        Page<FsArticle> page = new Page(currentPage, pageSize);
        Page<FsArticle> articlePage = fsArticleService.lambdaQuery()
                .in(!articleIds.isEmpty(), FsArticle::getId, articleIds)
                .eq(FsArticle::getIsDeleted, 0)
                .eq(FsArticle::getState, 1)
                .like(articleSearchDTO.getTitle() != null, FsArticle::getTitle, articleSearchDTO.getTitle())
                .eq(articleSearchDTO.getCreateUser() != null, FsArticle::getCreateUser, articleSearchDTO.getCreateUser())
                .page(page);
        //去查看数据
        Boolean execute = transactionTemplate.execute(status -> {
            try {
                if (articlePage != null && !articlePage.getRecords().isEmpty()) {
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
                        Map<Long, Boolean> likeCountByArticleIdAndUserId = likeCount
                                .getLikeCountByArticleIdAndUserId(articleIdStrings, userId.toString());
                        //是否关注
                        List<Long> toUserIds = records.stream().map(FsArticle::getCreateUser).distinct().toList();
                        Map<String, Boolean> followCountByUserIdAndToUserId = followCount.getFollowCountByUserIdAndToUserId(toUserIds, userId);
                        //查询文章的点赞的数量，评论的数量
                        Map<Long, Map<String, Long>> articleLikeAndCommentNumbersByArticleIds = commentAndLike4Article.getArticleLikeAndCommentNumbersByArticleIds(articleIdStrings);
                        //开始赋值
                        result.forEach(articleDTO -> {
                            //防止空指针
                            if (articleDTO.getArticleCountDTO() == null) {
                                articleDTO.setArticleCountDTO(new ArticleCountDTO());
                            }
                            //是否点赞
                            articleDTO.getArticleCountDTO()
                                    .setIsLike(likeCountByArticleIdAndUserId
                                            .get(articleDTO.getId().longValue()));
                            //是否关注
                            articleDTO.getArticleCountDTO()
                                    .setIsFollow(followCountByUserIdAndToUserId
                                            .get(articleDTO.getCreateUser().toString()));
                            //点赞数量
                            articleDTO.getArticleCountDTO()
                                    .setLikeCount(articleLikeAndCommentNumbersByArticleIds
                                            .get(articleDTO.getId().longValue())
                                            .get(articleCommentAndLike.likeNumber));
                            //评论数量
                            articleDTO.getArticleCountDTO()
                                    .setCommentCount(articleLikeAndCommentNumbersByArticleIds
                                            .get(articleDTO.getId().longValue())
                                            .get(articleCommentAndLike.commentNumber));
                        });
                    }).join();

                    CompletableFuture<Map<String, FsUserInfoDto>> userInfoFurture = CompletableFuture.supplyAsync(() -> {
                        //去查询用户信息
                        List<String> list1 = records.stream().map(
                                record -> record.getCreateUser().toString()
                        ).toList();
                        //查询信息
                        Map<String, FsUserInfoDto> userInfoByUserId = userInfoSerach.getUserInfoByUserId(list1);
                        return userInfoByUserId;
                    }, threadPoolTaskExecutor);

                    CompletableFuture<List<MgArticle>> uCompletableFuture = CompletableFuture.supplyAsync(() -> {
                        //查询一些信息
                        //去mongo寻找
                        //先从redis里面查询
                        List<MgArticle> mgArticles = new ArrayList<>();
                        Set<Integer> articleSetIds = new HashSet<>(articleIds);
                        List<Integer> needQuery = new ArrayList<>();
                        List<Object> hmget = redisUtil.hmget(RedisZHashConst.ARTICLE_MONGO_INFO, articleSetIds)
                                .stream()
                                .filter(Objects::nonNull).toList();
                        if (!hmget.isEmpty()) {
                            //查询到了数据
                            hmget.forEach(item -> {
                                try {
                                    MgArticle item1 = (MgArticle) item;
                                    mgArticles.add(item1);
                                    if (!articleSetIds.contains(item1.getArticleId())) {
                                        needQuery.add(item1.getArticleId());
                                    }
                                } catch (Exception e) {
                                    log.error("==> 查询redis数据失败: {}", e.getMessage());
                                }
                            });
                        }
                        if (!needQuery.isEmpty()) {
                            Query query = new Query();
                            query.addCriteria(Criteria.where("articleId").in(articleIds));
                            List<MgArticle> bbsArticleMarkdownInfo = mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
                            mgArticles.addAll(bbsArticleMarkdownInfo);
                            CompletableFuture.runAsync(()->{
                                Map<String, Object> collect = bbsArticleMarkdownInfo.stream()
                                        .collect(Collectors.toMap(
                                                mgArticle -> mgArticle.getArticleId().toString(),
                                                Function.identity()
                                        ));
                                redisUtil.hmset(RedisZHashConst.ARTICLE_MONGO_INFO, collect);
                            },threadPoolTaskExecutor);
                        }
                        return mgArticles;
                    }, threadPoolTaskExecutor);

                    userInfoFurture.thenAccept(userInfo -> {
                        //赋值
                        result.forEach(articleDTO -> {
                            FsUserInfoDto fsUserInfoDto = userInfo.get(articleDTO.getCreateUser().toString());
                            articleDTO.setPicture(fsUserInfoDto.getAvatar());
                        });
                    });

                    uCompletableFuture.thenAccept(mgArticles -> {
                        //赋值
                        result.forEach(articleDTO -> {
                            MgArticle mgArticle = mgArticles.stream().filter(
                                    mgArticle1 -> mgArticle1.getArticleId().equals(articleDTO.getId())
                            ).findFirst().orElse(null);
                            if (mgArticle != null) {
                                articleDTO.setMarkdown(mgArticle.getArticleMarkdown());
                            }
                            if (mgArticle != null) {
                                articleDTO.setHtml(mgArticle.getArticleHtml());
                            }
                        });
                    });

                    CompletableFuture.allOf(
                            userInfoFurture,
                            uCompletableFuture
                    ).join(); // 阻塞等待所有异步任务完成

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

        return PageResponse.success(result, currentPage, articlePage.getTotal());
    }

    @Override
    public R<Boolean> deleteArticleById(Integer id) {
        //获取当前的
        String userId = GlobalContext.get().toString();
        FsArticle one = fsArticleService.lambdaQuery().eq(FsArticle::getId, id).one();
        if (one == null)
            return R.ok(false);
        Integer isDeleted = one.getIsDeleted();
        if (isDeleted == 1) {
            return R.ok(false);
        }
        if (!one.getCreateUser().equals(Long.valueOf(userId))) {
            return R.ok(false);
        }
        fsArticleService.lambdaUpdate()
                .eq(FsArticle::getId, id)
                .set(FsArticle::getUpdateUser, Long.valueOf(userId))
                .set(FsArticle::getUpdateTime, new Date())
                .set(FsArticle::getIsDeleted, 1).update();
        return R.ok(true);
    }

    @Override
    public R<Boolean> articleTop(Integer id, Boolean top) {
        //获取当前的
        String userId = GlobalContext.get().toString();
        FsArticle one = fsArticleService.lambdaQuery().eq(FsArticle::getId, id).one();
        if (one == null)
            return R.ok(false);
        Integer isDeleted = one.getIsDeleted();
        if (isDeleted == 1) {
            return R.ok(false);
        }
        if (!one.getCreateUser().equals(Long.valueOf(userId))) {
            return R.ok(false);
        }
        fsArticleService.lambdaUpdate()
                .eq(FsArticle::getId, id)
                .set(FsArticle::getUpdateUser, Long.valueOf(userId))
                .set(FsArticle::getUpdateTime, new Date())
                .set(FsArticle::getTop, top ? one.getTop() + 1 : 0)
                .update();
        return R.ok(true);
    }

    @Override
    public R<ArticleCheckCountDTO> getArticleCheckCount(String title) {
        ArticleCheckCountDTO result = new ArticleCheckCountDTO();
        List<FsArticle> list = fsArticleService.lambdaQuery().eq(FsArticle::getIsDeleted, 0).list();
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
    public PageResponse<ArticleDTO> getLikesArticle(LikeSearchDTO likeSearchDTO) throws ExecutionException, InterruptedException {
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
        List<FsLikeDto> likeCountByLikeUser = likeCount
                .getLikeCountByLikeUser(currentPage, pageSize, likeSearchDTO.getLikeUser());
        //用户没有点赞任何文章
        if (likeCountByLikeUser.isEmpty()) {
            return PageResponse.success(Collections.emptyList(), currentPage, 0);
        }
        //获取文章ID
        List<String> articleIds = likeCountByLikeUser.stream().map(fsLikeDto -> fsLikeDto.getArticleId().toString()).toList();
        //去查询文章信息
        List<ArticleDTO> articleDTOList = getArticleDTOList(articleIds, userID);
        return PageResponse.success(articleDTOList, currentPage, likeCountByLikeUser.size());
    }


    private List<ArticleDTO> getArticleDTOList(List<String> articleIds, String userId) throws ExecutionException, InterruptedException {
        List<String> articleAuthorIds = new ArrayList<>();
        List<ArticleDTO> result = new ArrayList<>();
        //查询文章的内容
        CompletableFuture<List<MgArticle>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //从mongodb里面查询文章的html和markdown
            Query query = new Query();
            query.addCriteria(Criteria.where("articleId").in(articleIds));
            List<MgArticle> bbsArticleMarkdownInfo = mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
            //过滤出作者的id
            bbsArticleMarkdownInfo.forEach(mgArticle -> {
                articleAuthorIds.add(mgArticle.getArticleId().toString());
            });
            //从mysql里面查询文章的属性
            List<FsArticle> list = fsArticleService.lambdaQuery().in(FsArticle::getId, articleAuthorIds).list();
            list.forEach(fsArticle -> {
                ArticleDTO articleDTO = new ArticleDTO();
                BeanUtils.copyProperties(fsArticle, articleDTO);
                result.add(articleDTO);
            });
            return bbsArticleMarkdownInfo;
        }, threadPoolTaskExecutor);
        //查询文章所拥有的标签
        CompletableFuture<Map<String, List<FsLabel>>> mapCompletableFuture2 = CompletableFuture.supplyAsync(() -> {
            Map<String, List<FsLabel>> labelMap = new HashMap<>();
            List<FsArticleLabel> list = fsArticleLabelService
                    .lambdaQuery().in(FsArticleLabel::getArticleId, articleIds).list();
            if (list.isEmpty()) {
                return null;
            }
            List<Integer> list1 = list.stream().map(FsArticleLabel::getLabelId).distinct().toList();
            //根据文章id分组
            Map<Integer, List<FsArticleLabel>> collect = list.stream().collect(Collectors.groupingBy(FsArticleLabel::getArticleId));
            //根据标签id查询标签
            List<FsLabel> labels = fsLabelService
                    .lambdaQuery()
                    .in(FsLabel::getId, list).list();
            collect.forEach((articleId, fsArticleLabels) -> {
                List<Integer> labelIds = fsArticleLabels.stream().map(FsArticleLabel::getLabelId).toList();
                List<FsLabel> fsLabels = labels.stream()
                        .filter(fsLabel -> labelIds.contains(fsLabel.getId())).toList();
                labelMap.put(articleId.toString(), fsLabels);
            });
            return labelMap;
        }, threadPoolTaskExecutor);
        //查询文章的作者信息
        CompletableFuture<Map<String, FsUserInfoDto>> mapCompletableFuture1 = CompletableFuture.supplyAsync(() -> {
            //查询文章的作者信息
            return userInfoSerach.getUserInfoByUserId(articleAuthorIds);
        }, threadPoolTaskExecutor);
        //查询对应的文章是否有点赞或者关注
        CompletableFuture<Map<Long, Map<String, Boolean>>> mapCompletableFuture = CompletableFuture.supplyAsync(() ->
                commentAndLike4Article.getLikeAndFollowByArticleIdAndUserId(articleIds, List.of(userId)), threadPoolTaskExecutor);
        //必须优先完成这个任务
        List<MgArticle> mgArticles = listCompletableFuture.get();
        CompletableFuture.allOf(mapCompletableFuture2, mapCompletableFuture1, mapCompletableFuture).join();
        Map<String, List<FsLabel>> stringListMap = Optional.ofNullable(mapCompletableFuture2.join()).orElse(Collections.emptyMap());
        Map<String, FsUserInfoDto> stringFsUserInfoDtoMap = Optional.ofNullable(mapCompletableFuture1.join()).orElse(Collections.emptyMap());
        Map<Long, Map<String, Boolean>> longMapMap = Optional.ofNullable(mapCompletableFuture.join()).orElse(Collections.emptyMap());
        //开始赋值
        mgArticles.forEach(mgArticle -> {
            ArticleDTO articleDTO = new ArticleDTO();
            BeanUtils.copyProperties(mgArticle, articleDTO);
            //查询标签
            List<LabelDTO> labelDTOS = stringListMap.get(articleDTO.getId().toString()).stream().map(fsLabel -> {
                LabelDTO labelDTO = new LabelDTO();
                BeanUtils.copyProperties(fsLabel, labelDTO);
                return labelDTO;
            }).toList();
            articleDTO.setLabelDTOS(labelDTOS);
            //查询作者信息
            FsUserInfoDto fsUserInfoDto = stringFsUserInfoDtoMap.get(articleDTO.getCreateUser().toString());
            articleDTO.setCreateUserName(fsUserInfoDto.getNickname());
            articleDTO.setPicture(fsUserInfoDto.getAvatar());
            //查询点赞和关注
            Map<String, Boolean> stringBooleanMap = longMapMap.get(articleDTO.getId().longValue());
            articleDTO.getArticleCountDTO().setIsLike(stringBooleanMap.get(LikeAndFollowEnums.isLike));
            articleDTO.getArticleCountDTO().setIsFollow(stringBooleanMap.get(LikeAndFollowEnums.isFollow));
            result.add(articleDTO);
        });
        return result;
    }
}

