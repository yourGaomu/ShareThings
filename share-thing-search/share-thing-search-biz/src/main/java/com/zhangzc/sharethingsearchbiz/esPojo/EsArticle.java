package com.zhangzc.sharethingsearchbiz.esPojo;

import lombok.Data;
import org.dromara.easyes.annotation.HighLight;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.Analyzer;
import org.dromara.easyes.annotation.rely.FieldType;

import java.util.Date;
import java.util.List;

@Data
@IndexName("article_index")
public class EsArticle {
    @IndexId
    @IndexField(fieldType = FieldType.KEYWORD)
    private String id;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    @HighLight
    private String title;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    @HighLight
    private String summary;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    private String contentHtmlPlain;

    @IndexField(fieldType = FieldType.INTEGER)
    private List<Integer> labelIds;

    @IndexField(fieldType = FieldType.KEYWORD_TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    private String labelNames;

    @IndexField(fieldType = FieldType.INTEGER)
    private Integer state;

    @IndexField(fieldType = FieldType.BOOLEAN)
    private Boolean isDeleted;

    @IndexField(fieldType = FieldType.LONG)
    private Long authorId;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    private String authorName;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String authorLevel;

    @IndexField(fieldType = FieldType.LONG)
    private Long pv;

    @IndexField(fieldType = FieldType.LONG)
    private Long likeCount;

    @IndexField(fieldType = FieldType.LONG)
    private Long commentCount;

    @IndexField(fieldType = FieldType.INTEGER)
    private Integer top;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String titleMap;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String authorAvatar;

    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_SMART)
    private String authorIntro;

    @IndexField(fieldType = FieldType.KEYWORD)
    private String articleUrl;

    @IndexField(fieldType = FieldType.DATE, dateFormat = "strict_date_optional_time||epoch_millis")
    private Date createTime;

    @IndexField(fieldType = FieldType.DATE, dateFormat = "strict_date_optional_time||epoch_millis")
    private Date updateTime;
}