package com.zhangzc.sharethingarticleimpl.pojo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文章标签
 * @TableName fs_article_label
 */
@TableName(value ="fs_article_label")
@Data
public class FsArticleLabel implements Serializable {
    /**
     * 文章标签编号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 文章id
     */
    @TableField(value = "article_id")
    private Integer article_id;

    /**
     * 标签id
     */
    @TableField(value = "label_id")
    private Integer label_id;

    /**
     * 逻辑删除(0正常,1删除)
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    /**
     * 创建用户id
     */
    @TableField(value = "create_user")
    private Long create_user;

    /**
     * 更新用户id
     */
    @TableField(value = "update_user")
    private Long update_user;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date create_time;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date update_time;

    @TableField(exist = false)
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
        FsArticleLabel other = (FsArticleLabel) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getArticle_id() == null ? other.getArticle_id() == null : this.getArticle_id().equals(other.getArticle_id()))
            && (this.getLabel_id() == null ? other.getLabel_id() == null : this.getLabel_id().equals(other.getLabel_id()))
            && (this.getIs_deleted() == null ? other.getIs_deleted() == null : this.getIs_deleted().equals(other.getIs_deleted()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getUpdate_user() == null ? other.getUpdate_user() == null : this.getUpdate_user().equals(other.getUpdate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()))
            && (this.getUpdate_time() == null ? other.getUpdate_time() == null : this.getUpdate_time().equals(other.getUpdate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getArticle_id() == null) ? 0 : getArticle_id().hashCode());
        result = prime * result + ((getLabel_id() == null) ? 0 : getLabel_id().hashCode());
        result = prime * result + ((getIs_deleted() == null) ? 0 : getIs_deleted().hashCode());
        result = prime * result + ((getCreate_user() == null) ? 0 : getCreate_user().hashCode());
        result = prime * result + ((getUpdate_user() == null) ? 0 : getUpdate_user().hashCode());
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
        sb.append(", article_id=").append(article_id);
        sb.append(", label_id=").append(label_id);
        sb.append(", is_deleted=").append(is_deleted);
        sb.append(", create_user=").append(create_user);
        sb.append(", update_user=").append(update_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", update_time=").append(update_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}