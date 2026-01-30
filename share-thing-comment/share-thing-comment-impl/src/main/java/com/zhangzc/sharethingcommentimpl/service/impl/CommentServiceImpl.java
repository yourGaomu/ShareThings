package com.zhangzc.sharethingcommentimpl.service.impl;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.kafkaspringbootstart.utills.KafkaUtills;
import com.zhangzc.sharethingcommentimpl.consts.KafkaComment;
import com.zhangzc.sharethingcommentimpl.service.CommentService;
import com.zhangzc.sharethingscommon.pojo.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final KafkaUtills kafkaUtills;

    @Override
    public void creat(CommentDTO commentDTO, String s) {
        //发送消息去保存
        try {
            Object o = GlobalContext.get();
            commentDTO.setCommentUser((Long)o);

            kafkaUtills.sendMessage(KafkaComment.SENT_COMMENT_TOPIC, commentDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
