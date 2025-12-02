package com.zhangzc.sharethingcommentimpl.service.rpc;

import com.zhangzc.sharethingcommentapi.consts.articleCommentAndLike;
import com.zhangzc.sharethingcommentapi.rpc.CommentSearch;
import com.zhangzc.sharethingcommentimpl.pojo.domain.FsComment;
import com.zhangzc.sharethingcommentimpl.service.FsCommentLikeService;
import com.zhangzc.sharethingcommentimpl.service.FsCommentService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class commentSearchImpl implements CommentSearch {
    private final FsCommentService fsCommentService;
    private final FsCommentLikeService fsCommentLikeService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Map<Long, Map<String, Long>> getCommentNumbersByArticleIds(List<String> articleId) {
        // 把传入的 string id 转为 Integer 列表（若无法解析则忽略）
        List<Integer> ids = articleId.stream()
                .map(s -> {
                    try { return Integer.valueOf(s); } catch (NumberFormatException e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 异步查询评论数量（返回 Map<articleId, {commentNumber: n}>）
        CompletableFuture<Map<Long, Map<String, Long>>> commentsFuture = CompletableFuture.supplyAsync(() -> {
            List<FsComment> list = fsCommentService.lambdaQuery()
                    .in(FsComment::getArticleId, ids)
                    .eq(FsComment::getIsDeleted, 0)
                    .list();
            // 按文章分组计数
            Map<Long, Long> grouped = list.stream()
                    .collect(Collectors.groupingBy(c -> c.getArticleId().longValue(), Collectors.counting()));

            Map<Long, Map<String, Long>> result = new HashMap<>();
            grouped.forEach((k, v) -> result.put(k, Map.of(articleCommentAndLike.commentNumber, v)));
            // 确保存在默认 0
            ids.forEach(id -> result.putIfAbsent(id.longValue(), Map.of(articleCommentAndLike.commentNumber, 0L)));
            return result;
        }, threadPoolTaskExecutor);

        Map<Long, Map<String, Long>> join = commentsFuture.join();

        //保证每个文章id都有对应的评论数量
        ids.forEach(id -> join.putIfAbsent(id.longValue(), Map.of(articleCommentAndLike.commentNumber, 0L)));
        return join;
    }

    @Override
    public Map<String, Long> getCommentNumbers() {
        List<FsComment> list = fsCommentService.lambdaQuery().list();
        int size = list.size();
        Map<String,Long> result = new HashMap<>();
        result.put(articleCommentAndLike.commentNumbers, (long) size);
        return result;
    }
}
