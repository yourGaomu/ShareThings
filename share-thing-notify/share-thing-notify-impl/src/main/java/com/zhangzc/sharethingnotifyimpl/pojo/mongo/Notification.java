package com.zhangzc.sharethingnotifyimpl.pojo.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * MongoDB的notification集合对应的实体类
 * @Document指定集合名，与数据库中一致
 */
@Data // Lombok注解，自动生成getter/setter/toString等方法
@Document(collection = "notification") // 绑定到数据库中的notification集合
public class Notification {

    /**
     * MongoDB默认主键_id，类型为ObjectId
     * 若不需要自定义id，可直接用这个字段；若需要保留业务id，可同时保留id字段
     */
    @Id
    private String id; // MongoDB的ObjectId在Java中通常用String接收

    /**
     * 是否已读（对应isRead，Boolean类型）
     */
    private Boolean isRead;

    /**
     * 项目ID（对应projectId，Int32类型）
     */
    private Integer projectId;

    /**
     * 项目名称（对应projectName，String类型）
     */
    private String projectName;

    /**
     * 消息内容（对应message，String类型）
     */
    private String message;

    /**
     * 消息类型（对应type，Int32类型）
     */
    private Integer type;

    /**
     * 是否删除（对应isDeleted，Boolean类型）
     */
    private Boolean isDeleted;

    /**
     * 创建人ID（对应createUser，Int64类型，Java中用Long接收）
     */
    private Long createUser;

    /**
     * 创建人名称（对应createUserName，String类型）
     */
    private String createUserName;


    /**
     * 接收用户ID（谁接收这条通知）
     */
    private Long receiverUserId;

    /**
     * 接收用户名称
     */
    private String receiverUserName;

    /**
     * 创建时间（对应createTime，ISODate类型，Java中用LocalDateTime接收）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（对应updateTime，ISODate类型，Java中用LocalDateTime接收）
     */
    private LocalDateTime updateTime;
}