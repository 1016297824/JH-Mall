package com.mall.search.DO;

import java.time.LocalDateTime;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

/**
 * ES 商品搜索索引实体
 *
 * <p>索引别名 {@code mall_product}，通过别名读写。非 MySQL DO，不涉及 JPA。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@Document(indexName = "mall_product")
public class ProductIndexDO {

    /** SPU ID */
    @Id
    private Long productId;

    /** 商品名称（ik_max_word，权重 3.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuName;

    /** 副标题（ik_max_word，权重 1.5） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String subTitle;

    /** 精确匹配关键词 */
    @Field(type = FieldType.Keyword)
    private String keyword;

    /** 类目 ID */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 类目名称 */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 品牌 ID */
    @Field(type = FieldType.Long)
    private Long brandId;

    /** 品牌名称 */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /** 最低售价（分），整数存储避免浮点精度 */
    @Field(type = FieldType.Integer)
    private Integer price;

    /** 累计销量 */
    @Field(type = FieldType.Integer)
    private Integer salesCount;

    /** 标签数组 */
    @Field(type = FieldType.Keyword)
    private String[] tags;

    /** 商品主图（不索引，仅存储返回） */
    @Field(type = FieldType.Keyword, index = false)
    private String image;

    /** 上架状态 */
    @Field(type = FieldType.Boolean)
    private Boolean isOnSale;

    /** 创建时间 */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    /** SKU 规格文本拼接（ik_max_word，权重 1.0） */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuSpecs;

    /** 搜索补全字段 */
    @CompletionField(maxInputLength = 50)
    private Completion suggest;
}
