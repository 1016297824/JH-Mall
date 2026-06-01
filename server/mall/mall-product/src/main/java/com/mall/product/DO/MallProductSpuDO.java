package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 商品 SPU DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getSpuName() { return spuName; }
    public void setSpuName(String spuName) { this.spuName = spuName; }
    public String getSpuDescription() { return spuDescription; }
    public void setSpuDescription(String spuDescription) { this.spuDescription = spuDescription; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public Long getPriceMin() { return priceMin; }
    public void setPriceMin(Long priceMin) { this.priceMin = priceMin; }
    public Long getPriceMax() { return priceMax; }
    public void setPriceMax(Long priceMax) { this.priceMax = priceMax; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Integer getPublishStatus() { return publishStatus; }
    public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
    public Integer getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(Integer verifyStatus) { this.verifyStatus = verifyStatus; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
