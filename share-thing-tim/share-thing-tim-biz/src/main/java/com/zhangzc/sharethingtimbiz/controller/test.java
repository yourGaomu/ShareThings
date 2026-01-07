package com.zhangzc.sharethingtimbiz.controller;

import com.zhangzc.kafkaspringbootstart.utills.KafkaUtills;
import com.zhangzc.sharethingtimbiz.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/test")
@RestController
public class test {
    private final KafkaUtills kafkaUtills;

    @RequestMapping("/test")
    public String test() {
        User user = new User();
        user.setId("1");
        user.setName("test");
        kafkaUtills.sendMessage("test-topic", user);
        return "ok";
    }



}
