package com.zhangzc.sharethingarticleimpl.pojo.mongoDomain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

// 对应MongoDB的集合（表），需指定集合名（比如叫"articles"）
@Document(collection = "bbs_article_markdown_info")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MgArticle {
    // 对应MongoDB的_id字段（自动生成ObjectId）
    @Id
    private String id;

    // 对应articleHtml字段
    private String articleHtml;

    // 对应articleId字段（Int32）
    private Integer articleId;

    // 对应articleMarkdown字段
    private String articleMarkdown;

    // 对应time字段（ISODate）
    private Instant time;

    // 对应userId字段（Int64）
    private Long userId;

}