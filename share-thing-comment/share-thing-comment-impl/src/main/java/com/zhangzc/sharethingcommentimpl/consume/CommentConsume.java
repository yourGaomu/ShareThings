package com.zhangzc.sharethingcommentimpl.consume;


import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.kafkaspringbootstart.annotation.AutoInserByRedis;
import com.zhangzc.sensitivewordspringbootstart.utills.SensitiveWordUtil;
import com.zhangzc.sharethingcommentimpl.consts.KafkaComment;
import com.zhangzc.sharethingcommentimpl.pojo.domain.FsComment;
import com.zhangzc.sharethingcommentimpl.service.FsCommentService;
import com.zhangzc.sharethingscommon.pojo.dto.CommentDTO;
import com.zhangzc.sharethingscommon.utils.TimeUtil;
import com.zhangzc.sharethinguserapi.pojo.dto.FsUserInfoDto;
import com.zhangzc.sharethinguserapi.rpc.userInfoSerach;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentConsume {
    @Autowired(required = false)
    private final SensitiveWordUtil sensitiveWordUtil;
    private final FsCommentService fsCommentService;
    private final TransactionTemplate transactionTemplate;

    @DubboReference(check = false, timeout = 5000)
    private final userInfoSerach userInfoSerach;

    @KafkaListener(topics = KafkaComment.SENT_COMMENT_TOPIC)
    @AutoInserByRedis(
            strategy = AutoInserByRedis.DuplicateStrategy.SKIP, // 重复消息跳过
            enableAlert = true,                                   // 启用告警
            redisKeyPrefix = "kafka:offset"                      // Redis key前缀
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        //开始消费
        if (record.value() == null) {
            ack.acknowledge();
        }
        try {
            Object value = record.value();
            CommentDTO commentDTO = JsonUtils.parseObject((String) value, CommentDTO.class);
            //进行敏感词替换
            commentDTO.setContent(sensitiveWordUtil.replaceSensitiveWord(commentDTO.getContent()));
            //进行转换
            FsComment fsComment = new FsComment();
            BeanUtils.copyProperties(commentDTO, fsComment);
            fsComment.setCreateTime(TimeUtil.getDateTime(LocalDateTime.now()));
            fsComment.setUpdateTime(TimeUtil.getDateTime(LocalDateTime.now()));
            //获取用户信息保存
            FsUserInfoDto fsUserInfoDto = userInfoSerach
                    .getUserInfoByUserId(List.of(commentDTO.getCommentUser().toString()))
                    .get(commentDTO.getCommentUser().toString());
            fsComment.setPicture(fsComment.getPicture());
            fsComment.setCommentUserName(fsUserInfoDto.getNickname());
            fsComment.setLevel(fsUserInfoDto.getLevel());
            //存入数据库
            Boolean execute = transactionTemplate.execute(status -> {
                try {
                    fsCommentService.save(fsComment);
                    return true;
                } catch (Exception e) {
                    log.error(e.getMessage());
                    status.setRollbackOnly();
                }
                return false;
            });
            if (Boolean.TRUE.equals(execute)) {
                ack.acknowledge();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
