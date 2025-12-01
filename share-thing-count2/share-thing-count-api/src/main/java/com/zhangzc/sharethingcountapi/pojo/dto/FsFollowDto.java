package com.zhangzc.sharethingcountapi.pojo.dto;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注
 * @TableName fs_follow
 */

@Data
public class FsFollowDto implements Serializable {
    /**
     * 关注编号
     */

    private Integer id;

    /**
     * 发起关注的人
     */

    private Long from_user;

    /**
     * 状态(0取消,1关注)
     */

    private Integer state;

    /**
     * 被关注的人
     */

    private Long to_user;

    /**
     * 创建时间
     */

    private Date create_time;

    /**
     * 更新时间
     */

    private Date update_time;

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        FsFollowDto other = (FsFollowDto) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getFrom_user() == null ? other.getFrom_user() == null : this.getFrom_user().equals(other.getFrom_user()))
            && (this.getState() == null ? other.getState() == null : this.getState().equals(other.getState()))
            && (this.getTo_user() == null ? other.getTo_user() == null : this.getTo_user().equals(other.getTo_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()))
            && (this.getUpdate_time() == null ? other.getUpdate_time() == null : this.getUpdate_time().equals(other.getUpdate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getFrom_user() == null) ? 0 : getFrom_user().hashCode());
        result = prime * result + ((getState() == null) ? 0 : getState().hashCode());
        result = prime * result + ((getTo_user() == null) ? 0 : getTo_user().hashCode());
        result = prime * result + ((getCreate_time() == null) ? 0 : getCreate_time().hashCode());
        result = prime * result + ((getUpdate_time() == null) ? 0 : getUpdate_time().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", from_user=").append(from_user);
        sb.append(", state=").append(state);
        sb.append(", to_user=").append(to_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", update_time=").append(update_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}