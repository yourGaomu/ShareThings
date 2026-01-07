package com.zhangzc.sharethingarticleimpl.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.listenerspringbootstart.utills.OnlineUserUtil;
import com.zhangzc.mongodbspringbootstart.utills.MongoUtil;
import com.zhangzc.redisspringbootstart.redisConst.RedisZHashConst;
import com.zhangzc.redisspringbootstart.utills.RedisSetUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingarticleimpl.consts.RedisLabelConst;
import com.zhangzc.sharethingarticleimpl.consts.RedisUserGetArticleByStateConst;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticleLabel;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsLabel;
import com.zhangzc.sharethingarticleimpl.pojo.mongoDomain.MgArticle;
import com.zhangzc.sharethingarticleimpl.pojo.req.GetArticleInfoVo;
import com.zhangzc.sharethingarticleimpl.pojo.req.LikeSearchVo;
import com.zhangzc.sharethingarticleimpl.server.ArticleService;
import com.zhangzc.sharethingarticleimpl.server.FsArticleLabelService;
import com.zhangzc.sharethingarticleimpl.server.FsArticleService;
import com.zhangzc.sharethingarticleimpl.server.FsLabelService;
import com.zhangzc.sharethingcommentapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcommentapi.rpc.CommentSearch;
import com.zhangzc.sharethingcountapi.consts.LikeAndFollowEnums;
import com.zhangzc.sharethingcountapi.pojo.dto.FsLikeDto;
import com.zhangzc.sharethingcountapi.rpc.CommentAndLike4Article;
import com.zhangzc.sharethingcountapi.rpc.followCount;
import com.zhangzc.sharethingcountapi.rpc.likeCount;
import com.zhangzc.sharethingscommon.enums.ArticleStateEnum;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.pojo.dto.*;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethingscommon.utils.TimeUtil;
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

import javax.security.auth.login.LoginContext;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


@DubboService
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    // Redis缓存TTL常量（1小时）
    private static final int CACHE_TTL = 3600;

    private final TransactionTemplate transactionTemplate;
    private final FsArticleService fsArticleService;
    private final MongoUtil mongoUtil;
    private final OnlineUserUtil onlineUserUtil;
    private final RedisUtil redisUtil;
    private final RedisSetUtil redisSetUtil;
    private final FsArticleLabelService fsArticleLabelService;
    private final FsLabelService fsLabelService;
    @Qualifier("threadPoolTaskExecutor")
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @DubboReference(check = false, timeout = 5000)
    private likeCount likeCount;
    @DubboReference(check = false, timeout = 5000)
    private followCount followCount;
    @DubboReference(check = false, timeout = 5000)
    private CommentSearch commentSearch;
    @DubboReference(check = false, timeout = 5000)
    private CommentAndLike4Article commentAndLike4Article;
    @DubboReference(check = false, timeout = 5000)
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
                //计入Es数据库
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
                .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime) // 添加排序：置顶优先，然后按创建时间降序
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
                            //给时间赋值
                            articleDTO.setCreateTime(TimeUtil.getLocalDateTime(fsArticle.getCreateTime()));
                            articleDTO.setUpdateTime(TimeUtil.getLocalDateTime(fsArticle.getUpdateTime()));
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
                        //去 mongo寻找
                        //先从 redis 里面查询
                        List<MgArticle> mgArticles = new ArrayList<>();
                        Set<Integer> articleSetIds = new HashSet<>(articleIds);
                        List<Integer> needQuery = new ArrayList<>();

                        // 将 Integer 集合转换为 String 集合（Redis Hash key 必须是 String）
                        Set<String> articleSetIdsStr = articleSetIds.stream()
                                .map(String::valueOf)
                                .collect(Collectors.toSet());

                        List<Object> hmget = redisUtil.hmget(RedisZHashConst.ARTICLE_MONGO_INFO, articleSetIdsStr)
                                .stream()
                                .filter(Objects::nonNull).toList();
                        if (!hmget.isEmpty()) {
                            //查询到了数据
                            hmget.forEach(item -> {
                                try {
                                    // Redis 反序列化后是 LinkedHashMap，需要手动转换为 MgArticle
                                    MgArticle item1;
                                    if (item instanceof MgArticle) {
                                        item1 = (MgArticle) item;
                                    } else {
                                        // 使用 ObjectMapper 将 LinkedHashMap 转换为 MgArticle
                                        item1 = JsonUtils.parseObject(JsonUtils.toJsonString(item), MgArticle.class);
                                    }
                                    mgArticles.add(item1);
                                    if (!articleSetIds.contains(item1.getArticleId())) {
                                        needQuery.add(item1.getArticleId());
                                    }
                                } catch (Exception e) {
                                    log.error("==> 查询redis数据失败: {}", e.getMessage());
                                }
                            });
                        }
                        if (!needQuery.isEmpty() || mgArticles.isEmpty()) {
                            Query query = new Query();
                            query.addCriteria(Criteria.where("articleId").in(articleIds));
                            List<MgArticle> bbsArticleMarkdownInfo = mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
                            mgArticles.addAll(bbsArticleMarkdownInfo);
                            CompletableFuture.runAsync(() -> {
                                //存入redis
                                Map<String, Object> collect = bbsArticleMarkdownInfo.stream()
                                        .collect(Collectors.toMap(
                                                mgArticle -> mgArticle.getArticleId().toString(),
                                                Function.identity()
                                        ));
                                redisUtil.hmset(RedisZHashConst.ARTICLE_MONGO_INFO, collect, 3600L);
                            }, threadPoolTaskExecutor);
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
                log.info(e.getMessage());
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
        //文章数量
        int size = fsArticleService.lambdaQuery()
                .eq(FsArticle::getIsDeleted, 0)
                .list().size();
        //评论数量
        Long commentNumbers = commentSearch.getCommentNumbers().get(articleCommentAndLike.commentNumbers);
        result.setArticleCount((long) size);
        //查询用户在线数量
        Long onlineCount = onlineUserUtil.getOnlineCount();
        result.setVisitCount(onlineCount);
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
                , userID).forEach((k, v) -> {
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
    public PageResponse<ArticleDTO> getLikesArticle(LikeSearchVo likeSearchVo) throws ExecutionException, InterruptedException {
        String userID = GlobalContext.get().toString();
        if (likeSearchVo.getLikeUser() == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        //获取当前的页数
        Integer currentPage = likeSearchVo.getCurrentPage();
        //获取每页的数量
        Integer pageSize = likeSearchVo.getPageSize();
        List<FsLikeDto> likeCountByLikeUser = likeCount
                .getLikeCountByLikeUser(currentPage, pageSize, likeSearchVo.getLikeUser());
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

    @Override
    public R<ArticleDTO> getArticleByLabelId(GetArticleInfoVo getArticleInfoVo) throws ExecutionException, InterruptedException {
        //检查参数
        if (getArticleInfoVo == null || getArticleInfoVo.getId() == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        String useId;
        try {
            useId = GlobalContext.get().toString();
        } catch (Exception e) {
            //当前没有用户登录
            useId = "1030";
        }
        //查询文章信息
        ArticleDTO result = getArticleDTOList(List.of(getArticleInfoVo.getId()), useId).get(0);
        return R.ok(result);
    }

    @Override
    public PageResponse<ArticleDTO> getPersonalArticles(ArticleSearchDTO articleSearchDTO, ArticleStateEnum articleStateEnum) {
        //判断是否是本人在操作
        boolean equals = GlobalContext.get().toString().equals(articleSearchDTO.getCreateUser().toString());
        if (!equals) {
            return PageResponse.success(Collections.emptyList(), 1, 0);
        }
        //是本人操作
        Integer code = articleStateEnum.getCode();
        switch (code) {
            case -1:
                return getPendingReviewArticles(articleSearchDTO);
            case 0:
                return getDisabledArticles(articleSearchDTO);
            case 1:
                return getEnableArticles(articleSearchDTO);
            default:
                return PageResponse.success(Collections.emptyList(), 1, 0);
        }
    }

    //获取启用的文章
    private PageResponse<ArticleDTO> getEnableArticles(ArticleSearchDTO articleSearchDTO) {
        //获取当前的页，当前的页面大小
        Integer currentPage = articleSearchDTO.getCurrentPage();
        if (currentPage == null || currentPage <= 0) {
            currentPage = 1;
        }
        Integer pageSize = articleSearchDTO.getPageSize();
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        
        Long userId = articleSearchDTO.getCreateUser();
        Integer state = 1; // 启用状态
        
        // 1. 尝试从Redis缓存获取文章ID列表
        //user:articles:{userId}:{state}
        String cacheKey = RedisUserGetArticleByStateConst.USER_ARTICLES_BY_STATE.replace("{userId}", userId.toString())
                .replace("{state}", state.toString());

        try {
            // 计算分页的起止位置
            long start = (long) (currentPage - 1) * pageSize;
            long end = start + pageSize - 1;
            // 从ZSet中按score倒序获取文章ID列表（ZREVRANGE）
            Set<Object> cachedArticleIds = redisUtil.zReverseRange(cacheKey, start, end);
            
            if (cachedArticleIds != null && !cachedArticleIds.isEmpty()) {
                // 缓存命中，获取总数
                Long total = redisUtil.zCard(cacheKey);
                
                // 转换为文章ID列表
                List<String> articleIdList = cachedArticleIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                
                // 获取文章详情（已在getArticleDTOList中有缓存）
                List<ArticleDTO> articleDTOList = getArticleDTOList(articleIdList, userId.toString());
                
                int totalPages = (int) Math.ceil((double) total / pageSize);
                return PageResponse.success(articleDTOList, totalPages, total);
            }
        } catch (Exception e) {
            log.warn("从Redis获取个人文章列表失败，降级查询数据库: {}", e.getMessage());
        }
        
        // 2. 缓存未命中或异常，查询数据库
        IPage<FsArticle> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<FsArticle> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(FsArticle::getIsDeleted, 0)
                .eq(FsArticle::getState, state)
                .eq(FsArticle::getCreateUser, userId)
                .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime);
        
        IPage<FsArticle> result = fsArticleService.page(page, lambdaQueryWrapper);
        
        if (result == null || result.getRecords().isEmpty()) {
            return PageResponse.success(Collections.emptyList(), 1, 0);
        }
        
        // 3. 查询成功，智能缓存策略
        List<FsArticle> records = result.getRecords();
        long totalCount = result.getTotal();
        CompletableFuture.runAsync(() -> {
            try {
                // 根据数据量决定缓存策略：小数据量缓存全部，大数据量只缓存当前页
                final int CACHE_THRESHOLD = 100; // 缓存阈值：100篇文章
                
                if (totalCount <= CACHE_THRESHOLD) {
                    // 数据量小，一次性缓存所有文章（提高后续查询命中率）
                    List<FsArticle> allArticles = fsArticleService.lambdaQuery()
                            .eq(FsArticle::getIsDeleted, 0)
                            .eq(FsArticle::getState, state)
                            .eq(FsArticle::getCreateUser, userId)
                            .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime)
                            .list();
                    
                    if (!allArticles.isEmpty()) {
                        Map<Object, Double> zsetData = new LinkedHashMap<>();
                        for (FsArticle article : allArticles) {
                            double score = article.getCreateTime().getTime() / 1000.0;
                            if (article.getTop() != null && article.getTop() > 0) {
                                score += 10000000L * article.getTop();
                            }
                            zsetData.put(article.getId().toString(), score);
                        }
                        redisUtil.zAdd(cacheKey, zsetData);
                        redisUtil.expire(cacheKey, CACHE_TTL);
                        log.debug("用户{}缓存全部文章，共{}篇", userId, allArticles.size());
                    }
                } else {
                    // 数据量大，只缓存当前页（避免性能浪费）
                    Map<Object, Double> zsetData = new LinkedHashMap<>();
                    for (FsArticle article : records) {
                        double score = article.getCreateTime().getTime() / 1000.0;
                        if (article.getTop() != null && article.getTop() > 0) {
                            score += 10000000L * article.getTop();
                        }
                        zsetData.put(article.getId().toString(), score);
                    }
                    
                    if (!zsetData.isEmpty()) {
                        redisUtil.zAdd(cacheKey, zsetData);
                        // 数据量大时设置较短过期时间（10分钟）
                        redisUtil.expire(cacheKey, 600);
                        log.debug("用户{}文章数{}超过阈值，仅缓存当前页", userId, totalCount);
                    }
                }
            } catch (Exception e) {
                log.error("更新个人文章列表缓存失败: {}", e.getMessage(), e);
            }
        }, threadPoolTaskExecutor);
        
        // 4. 转换为DTO并返回
        List<String> articleIds = records.stream()
                .map(FsArticle::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        
        try {
            List<ArticleDTO> articleDTOList = getArticleDTOList(articleIds, userId.toString());
            int totalPages = (int) Math.ceil((double) result.getTotal() / pageSize);
            return PageResponse.success(articleDTOList, totalPages, result.getTotal());
        } catch (ExecutionException | InterruptedException e) {
            log.error("获取文章详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文章列表失败", e);
        }
    }
    //获取禁用的文章
    private PageResponse<ArticleDTO> getDisabledArticles(ArticleSearchDTO articleSearchDTO) {
        //获取当前的页，当前的页面大小
        Integer currentPage = articleSearchDTO.getCurrentPage();
        if (currentPage == null || currentPage <= 0) {
            currentPage = 1;
        }
        Integer pageSize = articleSearchDTO.getPageSize();
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        
        Long userId = articleSearchDTO.getCreateUser();
        Integer state = 0; // 禁用状态
        
        // 1. 尝试从Redis缓存获取文章ID列表
        String cacheKey = RedisUserGetArticleByStateConst.USER_ARTICLES_BY_STATE.replace("{userId}", userId.toString())
                .replace("{state}", state.toString());
        
        try {
            // 计算分页的起止位置
            long start = (long) (currentPage - 1) * pageSize;
            long end = start + pageSize - 1;
            
            // 从ZSet中按score倒序获取文章ID列表（ZREVRANGE）
            Set<Object> cachedArticleIds = redisUtil.zReverseRange(cacheKey, start, end);
            
            if (cachedArticleIds != null && !cachedArticleIds.isEmpty()) {
                // 缓存命中，获取总数
                Long total = redisUtil.zCard(cacheKey);
                
                // 转换为文章ID列表
                List<String> articleIdList = cachedArticleIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                
                // 获取文章详情（已在getArticleDTOList中有缓存）
                List<ArticleDTO> articleDTOList = getArticleDTOList(articleIdList, userId.toString());
                
                int totalPages = (int) Math.ceil((double) total / pageSize);
                return PageResponse.success(articleDTOList, totalPages, total);
            }
        } catch (Exception e) {
            log.warn("从Redis获取禁用文章列表失败，降级查询数据库: {}", e.getMessage());
        }
        
        // 2. 缓存未命中或异常，查询数据库
        IPage<FsArticle> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<FsArticle> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(FsArticle::getIsDeleted, 0)
                .eq(FsArticle::getState, state)
                .eq(FsArticle::getCreateUser, userId)
                .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime);
        
        IPage<FsArticle> result = fsArticleService.page(page, lambdaQueryWrapper);
        
        if (result == null || result.getRecords().isEmpty()) {
            return PageResponse.success(Collections.emptyList(), 1, 0);
        }
        
        // 3. 查询成功，智能缓存策略
        List<FsArticle> records = result.getRecords();
        long totalCount = result.getTotal();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 根据数据量决定缓存策略：小数据量缓存全部，大数据量只缓存当前页
                final int CACHE_THRESHOLD = 100; // 缓存阈值：100篇文章
                
                if (totalCount <= CACHE_THRESHOLD) {
                    // 数据量小，一次性缓存所有文章（提高后续查询命中率）
                    List<FsArticle> allArticles = fsArticleService.lambdaQuery()
                            .eq(FsArticle::getIsDeleted, 0)
                            .eq(FsArticle::getState, state)
                            .eq(FsArticle::getCreateUser, userId)
                            .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime)
                            .list();
                    
                    if (!allArticles.isEmpty()) {
                        Map<Object, Double> zsetData = new LinkedHashMap<>();
                        for (FsArticle article : allArticles) {
                            double score = article.getCreateTime().getTime() / 1000.0;
                            if (article.getTop() != null && article.getTop() > 0) {
                                score += 10000000L * article.getTop();
                            }
                            zsetData.put(article.getId().toString(), score);
                        }
                        redisUtil.zAdd(cacheKey, zsetData);
                        redisUtil.expire(cacheKey, CACHE_TTL);
                        log.debug("用户{}缓存全部禁用文章，共{}篇", userId, allArticles.size());
                    }
                } else {
                    // 数据量大，只缓存当前页（避免性能浪费）
                    Map<Object, Double> zsetData = new LinkedHashMap<>();
                    for (FsArticle article : records) {
                        double score = article.getCreateTime().getTime() / 1000.0;
                        if (article.getTop() != null && article.getTop() > 0) {
                            score += 10000000L * article.getTop();
                        }
                        zsetData.put(article.getId().toString(), score);
                    }
                    
                    if (!zsetData.isEmpty()) {
                        redisUtil.zAdd(cacheKey, zsetData);
                        // 数据量大时设置较短过期时间（10分钟）
                        redisUtil.expire(cacheKey, 600);
                        log.debug("用户{}禁用文章数{}超过阈值，仅缓存当前页", userId, totalCount);
                    }
                }
            } catch (Exception e) {
                log.error("更新禁用文章列表缓存失败: {}", e.getMessage(), e);
            }
        }, threadPoolTaskExecutor);
        
        // 4. 转换为DTO并返回
        List<String> articleIds = records.stream()
                .map(FsArticle::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        
        try {
            List<ArticleDTO> articleDTOList = getArticleDTOList(articleIds, userId.toString());
            int totalPages = (int) Math.ceil((double) result.getTotal() / pageSize);
            return PageResponse.success(articleDTOList, totalPages, result.getTotal());
        } catch (ExecutionException | InterruptedException e) {
            log.error("获取禁用文章详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取禁用文章列表失败", e);
        }
    }
    //获取待审核的文章
    private PageResponse<ArticleDTO> getPendingReviewArticles(ArticleSearchDTO articleSearchDTO) {
        //获取当前的页，当前的页面大小
        Integer currentPage = articleSearchDTO.getCurrentPage();
        if (currentPage == null || currentPage <= 0) {
            currentPage = 1;
        }
        Integer pageSize = articleSearchDTO.getPageSize();
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        
        Long userId = articleSearchDTO.getCreateUser();
        Integer state = -1; // 待审核状态
        
        // 1. 尝试从Redis缓存获取文章ID列表
        String cacheKey = RedisUserGetArticleByStateConst.USER_ARTICLES_BY_STATE.replace("{userId}", userId.toString())
                .replace("{state}", state.toString());
        
        try {
            // 计算分页的起止位置
            long start = (long) (currentPage - 1) * pageSize;
            long end = start + pageSize - 1;
            
            // 从ZSet中按score倒序获取文章ID列表（ZREVRANGE）
            Set<Object> cachedArticleIds = redisUtil.zReverseRange(cacheKey, start, end);
            
            if (cachedArticleIds != null && !cachedArticleIds.isEmpty()) {
                // 缓存命中，获取总数
                Long total = redisUtil.zCard(cacheKey);
                
                // 转换为文章ID列表
                List<String> articleIdList = cachedArticleIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                
                // 获取文章详情（已在getArticleDTOList中有缓存）
                List<ArticleDTO> articleDTOList = getArticleDTOList(articleIdList, userId.toString());
                
                int totalPages = (int) Math.ceil((double) total / pageSize);
                return PageResponse.success(articleDTOList, totalPages, total);
            }
        } catch (Exception e) {
            log.warn("从Redis获取待审核文章列表失败，降级查询数据库: {}", e.getMessage());
        }
        
        // 2. 缓存未命中或异常，查询数据库
        IPage<FsArticle> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<FsArticle> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(FsArticle::getIsDeleted, 0)
                .eq(FsArticle::getState, state)
                .eq(FsArticle::getCreateUser, userId)
                .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime);
        
        IPage<FsArticle> result = fsArticleService.page(page, lambdaQueryWrapper);
        
        if (result == null || result.getRecords().isEmpty()) {
            return PageResponse.success(Collections.emptyList(), 1, 0);
        }
        
        // 3. 查询成功，智能缓存策略
        List<FsArticle> records = result.getRecords();
        long totalCount = result.getTotal();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 根据数据量决定缓存策略：小数据量缓存全部，大数据量只缓存当前页
                final int CACHE_THRESHOLD = 100; // 缓存阈值：100篇文章
                
                if (totalCount <= CACHE_THRESHOLD) {
                    // 数据量小，一次性缓存所有文章（提高后续查询命中率）
                    List<FsArticle> allArticles = fsArticleService.lambdaQuery()
                            .eq(FsArticle::getIsDeleted, 0)
                            .eq(FsArticle::getState, state)
                            .eq(FsArticle::getCreateUser, userId)
                            .orderByDesc(FsArticle::getTop, FsArticle::getCreateTime)
                            .list();
                    
                    if (!allArticles.isEmpty()) {
                        Map<Object, Double> zsetData = new LinkedHashMap<>();
                        for (FsArticle article : allArticles) {
                            double score = article.getCreateTime().getTime() / 1000.0;
                            if (article.getTop() != null && article.getTop() > 0) {
                                score += 10000000L * article.getTop();
                            }
                            zsetData.put(article.getId().toString(), score);
                        }
                        redisUtil.zAdd(cacheKey, zsetData);
                        redisUtil.expire(cacheKey, CACHE_TTL);
                        log.debug("用户{}缓存全部待审核文章，共{}篇", userId, allArticles.size());
                    }
                } else {
                    // 数据量大，只缓存当前页（避免性能浪费）
                    Map<Object, Double> zsetData = new LinkedHashMap<>();
                    for (FsArticle article : records) {
                        double score = article.getCreateTime().getTime() / 1000.0;
                        if (article.getTop() != null && article.getTop() > 0) {
                            score += 10000000L * article.getTop();
                        }
                        zsetData.put(article.getId().toString(), score);
                    }
                    
                    if (!zsetData.isEmpty()) {
                        redisUtil.zAdd(cacheKey, zsetData);
                        // 数据量大时设置较短过期时间（10分钟）
                        redisUtil.expire(cacheKey, 600);
                        log.debug("用户{}待审核文章数{}超过阈值，仅缓存当前页", userId, totalCount);
                    }
                }
            } catch (Exception e) {
                log.error("更新待审核文章列表缓存失败: {}", e.getMessage(), e);
            }
        }, threadPoolTaskExecutor);
        
        // 4. 转换为DTO并返回
        List<String> articleIds = records.stream()
                .map(FsArticle::getId)
                .map(String::valueOf)
                .collect(Collectors.toList());
        
        try {
            List<ArticleDTO> articleDTOList = getArticleDTOList(articleIds, userId.toString());
            int totalPages = (int) Math.ceil((double) result.getTotal() / pageSize);
            return PageResponse.success(articleDTOList, totalPages, result.getTotal());
        } catch (ExecutionException | InterruptedException e) {
            log.error("获取待审核文章详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取待审核文章列表失败", e);
        }
    }

    private List<ArticleDTO> getArticleDTOList(List<String> articleIds, String userId) throws ExecutionException, InterruptedException {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> articleAuthorIds = new ArrayList<>();
        List<Integer> articleIdsForMysql = new ArrayList<>();
        List<ArticleDTO> result = new ArrayList<>();
        //查询文章的内容
        CompletableFuture<List<MgArticle>> listCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //从redis里面查询mongo信息
            List<MgArticle> mgArticles = new ArrayList<>();
            Set<String> articleSetIdsStr = new HashSet<>(articleIds);
            List<Integer> needQuery = articleSetIdsStr.stream().map(Integer::parseInt).collect(Collectors.toList());
            List<Object> hmget = redisUtil.hmget(RedisZHashConst.ARTICLE_MONGO_INFO, articleSetIdsStr)
                    .stream()
                    .filter(Objects::nonNull).toList();
            //数据不为空进行处理
            if (!hmget.isEmpty()) {
                //查询到了数据
                hmget.forEach(item -> {
                    try {
                        // Redis 反序列化后是 LinkedHashMap，需要手动转换为 MgArticle
                        MgArticle item1;
                        if (item instanceof MgArticle) {
                            item1 = (MgArticle) item;
                        } else {
                            // 使用 ObjectMapper 将 LinkedHashMap 转换为 MgArticle
                            item1 = JsonUtils.parseObject(JsonUtils.toJsonString(item), MgArticle.class);
                        }
                        mgArticles.add(item1);
                        if (articleSetIdsStr.contains(item1.getArticleId().toString())) {
                            needQuery.remove(item1.getArticleId());
                        }
                    } catch (Exception e) {
                        log.error("==> 查询redis数据失败: {}", e.getMessage());
                    }
                });
            }
            //如果redis里面没有查询到或者需要查询id不为空
            if (!needQuery.isEmpty()) {
                Query query = new Query();
                //mongo里面对应的是Integer类型
                query.addCriteria(Criteria.where("articleId").in(needQuery));
                List<MgArticle> bbsArticleMarkdownInfo = mongoUtil.find(query, MgArticle.class, "bbs_article_markdown_info");
                mgArticles.addAll(bbsArticleMarkdownInfo);
                CompletableFuture.runAsync(() -> {
                    //存入redis
                    Map<String, Object> collect = bbsArticleMarkdownInfo.stream()
                            .collect(Collectors.toMap(
                                    mgArticle -> mgArticle.getArticleId().toString(),
                                    Function.identity()
                            ));
                    redisUtil.hmset(RedisZHashConst.ARTICLE_MONGO_INFO, collect, 3600L);
                }, threadPoolTaskExecutor);
            }
            //todo 可以存储作者-文章id 文章id-文章mysqlInfo
            //过滤出文章ID和作者ID
            mgArticles.forEach(mgArticle -> {
                if (mgArticle.getArticleId() != null) {
                    articleIdsForMysql.add(mgArticle.getArticleId());
                }
                if (mgArticle.getUserId() != null) {
                    articleAuthorIds.add(mgArticle.getUserId().toString());
                }
            });
            //从 mysql里面查询文章的属性
            List<FsArticle> list = fsArticleService.lambdaQuery()
                    .eq(FsArticle::getIsDeleted, 0)
                    .in(FsArticle::getId, articleIdsForMysql).list();
            //创建ArticleDTO并设置基础属性（不添加到result，等待后面统一组装）
            list.forEach(fsArticle -> {
                ArticleDTO articleDTO = new ArticleDTO();
                //设置文章的id
                articleDTO.setId(fsArticle.getId());
                //设置时间
                articleDTO.setCreateTime(TimeUtil.getLocalDateTime(fsArticle.getCreateTime()));
                articleDTO.setUpdateTime(TimeUtil.getLocalDateTime(fsArticle.getUpdateTime()));
                articleDTO.setIsDeleted(false);
                articleDTO.setTop(fsArticle.getTop());
                //初始化ArticleCountDTO，防止NPE
                articleDTO.setArticleCountDTO(new ArticleCountDTO());
                //设置文章的其他属性
                BeanUtils.copyProperties(fsArticle, articleDTO);
                result.add(articleDTO);
            });
            return mgArticles;
        }, threadPoolTaskExecutor);
        //查询文章所拥有的标签
        CompletableFuture<Map<String, List<FsLabel>>> mapCompletableFuture2 = CompletableFuture.supplyAsync(() -> {
            Map<String, List<FsLabel>> labelMap = new HashMap<>();
            //从redis里面查询
            String articleLabel = RedisLabelConst.ARTICLE_LABEL;
            List<Object> hmget = redisUtil.hmget(articleLabel, articleIds);
            //需要查询的文档id
            List<String> needQuery = new ArrayList<>(articleIds);
            //非空
            if (!hmget.isEmpty()) {
                try {
                    AtomicInteger index = new AtomicInteger();
                    hmget.forEach(item -> {
                        if (item != null) {
                            //说明redis里面没有数据
                            List<FsLabel> labels = JsonUtils.parseList(JsonUtils.toJsonString(item), new TypeReference<List<FsLabel>>() {
                            });
                            String currentArticleId = articleIds.get(index.get());
                            labelMap.put(currentArticleId, labels);
                            needQuery.remove(currentArticleId);
                        }
                        index.getAndIncrement();
                    });
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }
            //需要查询的id不为空
            if (!needQuery.isEmpty()) {
                List<FsArticleLabel> list = fsArticleLabelService
                        .lambdaQuery()
                        .eq(FsArticleLabel::getIsDeleted, 0)
                        .in(FsArticleLabel::getArticleId, needQuery).list();
                if (list.isEmpty()) {
                    return labelMap;
                }
                //过滤出标签id
                List<Integer> list1 = list.stream().map(FsArticleLabel::getLabelId).distinct().toList();
                //根据文章id分组
                Map<Integer, List<FsArticleLabel>> collect = list.stream().collect(Collectors.groupingBy(FsArticleLabel::getArticleId));
                //根据标签id查询标签
                List<FsLabel> labels = fsLabelService
                        .lambdaQuery()
                        .in(FsLabel::getId, list1).list();
                collect.forEach((articleId, fsArticleLabels) -> {
                    //获取该文章的标签id
                    List<Integer> labelIds = fsArticleLabels.stream().map(FsArticleLabel::getLabelId).toList();
                    //获取标签
                    List<FsLabel> fsLabels = labels.stream()
                            .filter(fsLabel -> labelIds.contains(fsLabel.getId()))
                            .toList();
                    labelMap.put(articleId.toString(), fsLabels);
                });
                //存入redis
                CompletableFuture.runAsync(() -> {
                    Map<String, Object> redisMap = labelMap.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
                    redisUtil.hmset(RedisLabelConst.ARTICLE_LABEL, redisMap, 3600L);
                }, threadPoolTaskExecutor);
            }
            return labelMap;
        }, threadPoolTaskExecutor);
        //必须优先完成文章内容查询任务（因为后续任务依赖articleAuthorIds）
        List<MgArticle> mgArticles = listCompletableFuture.get();

        //等待第一个任务完成后，再启动依赖articleAuthorIds的任务
        //查询文章的作者信息
        CompletableFuture<Map<String, FsUserInfoDto>> mapCompletableFuture1 = CompletableFuture.supplyAsync(() -> {
            //查询文章的作者信息
            return userInfoSerach.getUserInfoByUserId(articleAuthorIds);
        }, threadPoolTaskExecutor);
        //查询对应的文章是否有点赞或者是否对应的作者关注
        CompletableFuture<Map<Long, Map<String, Boolean>>> mapCompletableFuture = CompletableFuture.supplyAsync(() ->
                commentAndLike4Article.getLikeAndFollowByArticleIdAndUserId(articleIds, userId), threadPoolTaskExecutor);

        //等待所有任务完成
        CompletableFuture.allOf(mapCompletableFuture2, mapCompletableFuture1, mapCompletableFuture).join();
        Map<String, List<FsLabel>> stringListMap = Optional.ofNullable(mapCompletableFuture2.join()).orElse(Collections.emptyMap());
        Map<String, FsUserInfoDto> stringFsUserInfoDtoMap = Optional.ofNullable(mapCompletableFuture1.join()).orElse(Collections.emptyMap());
        Map<Long, Map<String, Boolean>> longMapMap = Optional.ofNullable(mapCompletableFuture.join()).orElse(Collections.emptyMap());
        //开始赋值：为已存在的ArticleDTO补充MongoDB数据和其他信息
        //先将mgArticles转换为Map，方便按articleId查找
        Map<Integer, MgArticle> mgArticleMap = mgArticles.stream()
                .filter(mg -> mg.getArticleId() != null)
                .collect(Collectors.toMap(MgArticle::getArticleId, Function.identity(), (old, newVal) -> newVal));

        result.forEach(articleDTO -> {
            //补充MongoDB数据（Markdown和HTML内容）
            if (articleDTO.getId() != null) {
                MgArticle mgArticle = mgArticleMap.get(articleDTO.getId());
                if (mgArticle != null) {
                    articleDTO.setHtml(mgArticle.getArticleHtml());
                    articleDTO.setMarkdown(mgArticle.getArticleMarkdown());
                }
            }
            //查询标签（防止NPE）
            if (articleDTO.getId() != null) {
                List<FsLabel> fsLabelList = stringListMap.get(articleDTO.getId().toString());
                if (fsLabelList != null && !fsLabelList.isEmpty()) {
                    List<LabelDTO> labelDTOS = fsLabelList.stream().map(fsLabel -> {
                        LabelDTO labelDTO = new LabelDTO();
                        BeanUtils.copyProperties(fsLabel, labelDTO);
                        return labelDTO;
                    }).toList();
                    articleDTO.setLabelDTOS(labelDTOS);
                } else {
                    articleDTO.setLabelDTOS(Collections.emptyList());
                }
            }
            //查询作者信息（防止NPE）
            if (articleDTO.getCreateUser() != null) {
                FsUserInfoDto fsUserInfoDto = stringFsUserInfoDtoMap.get(articleDTO.getCreateUser().toString());
                if (fsUserInfoDto != null) {
                    articleDTO.setCreateUserName(fsUserInfoDto.getNickname());
                    articleDTO.setPicture(fsUserInfoDto.getAvatar());
                }
            }
            //查询点赞和关注
            if (articleDTO.getId() != null) {
                Map<String, Boolean> stringBooleanMap = longMapMap.get(articleDTO.getId().longValue());
                if (stringBooleanMap != null) {
                    articleDTO.getArticleCountDTO().setIsLike(stringBooleanMap.get(LikeAndFollowEnums.isLike));
                    articleDTO.getArticleCountDTO().setIsFollow(stringBooleanMap.get(LikeAndFollowEnums.isFollow));
                } else {
                    //默认值
                    articleDTO.getArticleCountDTO().setIsLike(false);
                    articleDTO.getArticleCountDTO().setIsFollow(false);
                }
            }
        });
        return result;
    }
}

