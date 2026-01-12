package com.zhangzc.sharethinguserimpl.pojo.mongo;


import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;


import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 用户热度行为记录实体（对应MongoDB集合：user_behavior_record）
 */
@Data
@Document(collection = "user_behavior_record") // 映射到MongoDB的指定集合
public class HeatBehaviorRecordMongo implements Serializable {
    /**
     * MongoDB默认主键，自动生成ObjectId
     */
    @Id
    private String id;

    /**
     * 用户ID（大整数，对应MongoDB的NumberLong）
     */
    private Long userId;

    /**
     * 目标内容ID（如文章ID、视频ID）
     */
    private Long targetId;

    /**
     * 目标类型：1-文章，2-评论，3-视频（默认1）
     */
    private Integer targetType = 1;

    /**
     * 行为类型ID（关联HeatBehaviorType的behaviorTypeId）
     */
    private Integer behaviorTypeId;

    /**
     * 行为状态：1-有效，0-取消（默认1）
     */
    private Integer behaviorStatus = 1;

    /**
     * 行为发生时间（自动填充）
     */
    @CreatedDate
    private Date createTime;

    /**
     * 记录更新时间（自动更新）
     */
    @LastModifiedDate
    private Date updateTime;

    /**
     * 扩展字段（对应MongoDB的JSON对象，存储额外行为信息）
     * 用Map接收，自动映射为MongoDB的BSON对象，灵活存储键值对
     */
    private Map<String, Object> extInfo;
}