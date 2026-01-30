package com.zhangzc.sharethingcommentimpl.pojo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 评论
 * @TableName fs_comment
 */
@TableName(value ="fs_comment")
@Data
public class FsComment implements Serializable {
    /**
     * 评论编号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 父评论id
     */
    @TableField(value = "pre_id")
    private Integer preId;

    /**
     * 评论内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 被评论帖子id
     */
    @TableField(value = "article_id")
    private Integer articleId;

    /**
     * 状态(0禁用,1启用)
     */
    @TableField(value = "state")
    private Integer state;

    /**
     * 逻辑删除(0正常,1删除)
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 评论用户id
     */
    @TableField(value = "comment_user")
    private Long commentUser;

    /**
     * 评论用户名称
     */
    @TableField(value = "comment_user_name")
    private String commentUserName;

    /**
     * 用户头像
     */
    @TableField(value = "picture")
    private String picture;

    /**
     * 等级（Lv6）
     */
    @TableField(value = "level")
    private String level;

    /**
     * 点赞数量
     */
    @TableField(value = "like_count")
    private Long likeCount;

    /**
     * 回复数量
     */
    @TableField(value = "replies_count")
    private Integer repliesCount;

    /**
     * 评论深度
     */
    @TableField(value = "depth")
    private Integer depth;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;


}