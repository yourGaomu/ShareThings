package com.zhangzc.sharethingarticleimpl.server.rpc;


import com.fasterxml.jackson.core.type.TypeReference;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingarticleapi.rpc.ArticleRpc;
import com.zhangzc.sharethingarticleimpl.consts.RedisUser4ArticleConst;
import com.zhangzc.sharethingarticleimpl.pojo.domain.FsArticle;
import com.zhangzc.sharethingarticleimpl.server.FsArticleService;
import com.zhangzc.sharethingscommon.pojo.dto.ArticleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
@Slf4j
public class ArticleRpcImpl implements ArticleRpc {
    private final FsArticleService fsArticleService;
    private final RedisUtil redisUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Map<String, String> getUserIdsByArticleIds(List<String> articleIds) {
        // 入参校验：articleIds 不能为空
        if (CollectionUtils.isEmpty(articleIds)) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        String user4ArticleKey = RedisUser4ArticleConst.USER_4_ARTICLE;

        // 1. 从Redis批量获取articleId->userId
        List<Object> hmgetResult = redisUtil.hmget(user4ArticleKey, articleIds);
        // 存储需要从DB查询的articleId
        List<String> needQueryArticleIds = new ArrayList<>();

        // 2. 解析Redis返回结果，区分已存在和需查询的articleId
        for (int i = 0; i < articleIds.size(); i++) {
            String articleId = articleIds.get(i);
            // 防止hmgetResult长度与articleIds不一致（极端情况）
            Object userIdObj = i < hmgetResult.size() ? hmgetResult.get(i) : null;

            if (userIdObj != null) {
                // Redis中存在，直接存入结果（转为String）
                result.put(articleId, String.valueOf(userIdObj));
            } else {
                // Redis中不存在，加入待查询列表
                needQueryArticleIds.add(articleId);
            }
        }

        // 3. 若无需查询DB，直接返回
        if (CollectionUtils.isEmpty(needQueryArticleIds)) {
            return result;
        }

        // 4. 从DB查询缺失的articleId->userId
        List<FsArticle> articleList = fsArticleService.lambdaQuery()
                .eq(FsArticle::getIsDeleted, 0)
                .in(FsArticle::getId, needQueryArticleIds)
                .list();

        // 5. 处理DB查询结果（过滤null，避免空指针）
        Map<String, String> dbResultMap = articleList.stream()
                // 过滤id或createUser为null的无效数据
                .filter(article -> article.getId() != null && article.getCreateUser() != null)
                .collect(Collectors.toMap(
                        article -> article.getId().toString(),
                        article -> article.getCreateUser().toString(),
                        (oldVal, newVal) -> newVal // 重复id保留新值
                ));

        // 6. 合并DB结果到最终map
        result.putAll(dbResultMap);

        // 7. 异步将DB查询结果写入Redis（追加模式，避免覆盖原有数据）
        if (!CollectionUtils.isEmpty(dbResultMap)) {
            // 转换为Map<String, Object>适配redisUtil.hmset
            Map<String, Object> redisMap = new HashMap<>(dbResultMap);
            CompletableFuture.runAsync(() -> {
                try {
                    redisUtil.hmset(user4ArticleKey, redisMap);
                } catch (Exception e) {
                    // 异步任务异常需捕获，避免线程池吞异常
                    log.error("写入Redis失败", e);
                    e.printStackTrace();
                }
            }, threadPoolTaskExecutor);
        }

        return result;
    }

    @Override
    public List<ArticleDTO> getArticleDtoByArticleIds(List<String> articleIds) {
        return fsArticleService.lambdaQuery()
                .eq(FsArticle::getState, 1)
                .eq(FsArticle::getIsDeleted, 0)
                .in(FsArticle::getId, articleIds).list().stream().map(article -> {
            ArticleDTO articleDTO = new ArticleDTO();
            BeanUtils.copyProperties(article, articleDTO);
            return articleDTO;
        }).toList();
    }
}
