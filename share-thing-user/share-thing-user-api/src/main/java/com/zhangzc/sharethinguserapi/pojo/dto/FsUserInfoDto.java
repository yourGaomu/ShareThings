package com.zhangzc.sharethinguserapi.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息表
 * @TableName fs_user_info
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FsUserInfoDto implements Serializable {

    private Long id;

    private String userId;

    private String nickname;

    private String avatar;

    private Date birthday;

    private String backgroundImg;

    private String phone;

    private Integer sex;

    private Integer status;

    private String introduction;

    private Date createTime;

    private Date updateTime;

    private Boolean isDeleted;

    private String level;

    private Integer points;

    private static final long serialVersionUID = 1L;

}