package com.zhangzc.sharethinguserimpl.pojo.vo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author maliangnansheng
 * @date 2022/5/5 18:43
 */

@Data
public class UserForumDTO implements Serializable {
    private Long id;
    private String name;
    private Integer gender;
    private String birthday;
    private Integer age;
    private String phone;
    private String email;
    private String picture;
    private String position;
    private String company;
    private String homePage;
    private String intro;
    private List<RoleOutDTO> roles;
    private Integer orgId;
    private List<Map<String, Object>> org;
    private Boolean state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /**
     * 获得的点赞数
     */
    private Long likeCount;
    /**
     * 获得的阅读量
     */
    private Long readCount;
    /**
     * 积分
     */
    private Integer points;
    /**
     * 等级（Lv6）
     */
    private String level;
    /**
     * 是否关注
     */
    private Boolean isFollow;
}
