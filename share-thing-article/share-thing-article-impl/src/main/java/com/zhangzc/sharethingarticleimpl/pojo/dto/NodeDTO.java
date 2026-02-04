package com.zhangzc.sharethingarticleimpl.pojo.dto;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * 目录/文章节点实体类
 * 对应接口入参的节点信息定义
 */
@Data
public class NodeDTO {

    /**
     * 节点ID
     * 更新时必填
     */
    private Long id;

    /**
     * 父节点ID
     * null 或 0 表示根目录，非必填
     */
    private Long parentId;

    /**
     * 节点类型
     * 枚举值：FOLDER (文件夹), ARTICLE (文章)，必填
     */
    private NodeType nodeType;

    /**
     * 文件夹名称或文章标题
     * 长度限制255，必填
     */
    private String name;

    /**
     * 关联的文章ID
     * 当 nodeType = ARTICLE 时 必填
     */
    private Long articleId;

    /**
     * 排序权重值
     * 数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 节点类型枚举
     */
    public enum NodeType {
        FOLDER,  // 文件夹
        ARTICLE  // 文章
    }

    /**
     * 自定义校验方法（可选）
     * 用于校验：当节点类型为ARTICLE时，articleId不能为空
     * 可结合Spring Validation的自定义校验注解使用
     */
    public boolean validateArticleId() {
        if (NodeType.ARTICLE.equals(nodeType)) {
            return articleId != null && articleId > 0;
        }
        return true;
    }
}