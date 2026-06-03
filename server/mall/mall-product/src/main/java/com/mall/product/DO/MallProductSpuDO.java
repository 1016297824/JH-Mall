package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 商品 SPU DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_product_spu")
public class MallProductSpuDO {

    /** SPU ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属类目 ID */
    @TableField("category_id")
    private Long categoryId;

    /** 所属品牌 ID */
    @TableField("brand_id")
    private Long brandId;

    /** SPU 名称 */
    @TableField("spu_name")
    private String spuName;

    /** SPU 详情描述（HTML） */
    @TableField("spu_description")
    private String spuDescription;

    /** 主图 URL */
    @TableField("main_image")
    private String mainImage;

    /** 轮播图 JSON 数组（存储为 JSON 字符串，如 ["url1","url2"]） */
    @TableField("images_json")
    private String imagesJson;

    /** 最低价（分，同 SPU 下 SKU 最低销售价） */
    @TableField("price_min")
    private Long priceMin;

    /** 最高价（分，同 SPU 下 SKU 最高销售价） */
    @TableField("price_max")
    private Long priceMax;

    /** 累计销量 */
    @TableField("sales_count")
    private Integer salesCount;

    /** 评价条数 */
    @TableField("review_count")
    private Integer reviewCount;

    /** 上架状态（0=未上架，1=已上架） */
    @TableField("publish_status")
    private Integer publishStatus;

    /** 审核状态（0=未审核，1=已审核） */
    @TableField("verify_status")
    private Integer verifyStatus;

    /** 逻辑删除标识（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
