package com.mall.common.DTO.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索索引重建专用 SPU DTO
 *
 * <p>比 SpuDTO 多包含类目名、品牌名、SKU 规格拼接、创建/更新时间，
 * 供 mall-search 全量重建 ProductIndex 时使用。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpuSearchDTO {

    /** SPU ID */
    private Long spuId;

    /** SPU 名称 */
    private String spuName;

    /** 副标题（取 spu_description 截断 200 字） */
    private String subTitle;

    /** 主图 */
    private String mainImage;

    /** 最低价（分） */
    private Long priceMin;

    /** 上下架状态（0=下架，1=上架） */
    private Integer publishStatus;

    /** 累计销量 */
    private Integer salesCount;

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称 */
    private String categoryName;

    /** 品牌 ID */
    private Long brandId;

    /** 品牌名称 */
    private String brandName;

    /** 标签（逗号分隔，暂无） */
    private String tags;

    /** SKU 规格文本拼接（取自 sku.attrs_json） */
    private String spuSpecs;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间（增量回补比较用） */
    private LocalDateTime updateTime;
}
