package com.mall.admin.product.domain;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * SPU 管理对象 mall_product_spu
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallProductSpu extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 所属三级类目 ID */
    @Excel(name = "所属三级类目 ID")
    private String categoryId;

    /** 所属品牌 ID */
    @Excel(name = "所属品牌 ID")
    private String brandId;

    /** SPU 名称（商品标题） */
    @Excel(name = "SPU 名称", readConverterExp = "商=品标题")
    private String spuName;

    /** 商品详情描述（富文本 HTML） */
    @Excel(name = "商品详情描述", readConverterExp = "富=文本,H=TML")
    private String spuDescription;

    /** 商品主图 URL */
    @Excel(name = "商品主图 URL")
    private String mainImage;

    /** 轮播图 JSON 数组 */
    @Excel(name = "轮播图 JSON 数组")
    private String imagesJson;

    /** 最低销售价（单位：分） */
    @Excel(name = "最低销售价", readConverterExp = "单=位：分")
    private String priceMin;

    /** 最高销售价（单位：分） */
    @Excel(name = "最高销售价", readConverterExp = "单=位：分")
    private String priceMax;

    /** 累计销量 */
    @Excel(name = "累计销量")
    private String salesCount;

    /** 评价条数 */
    @Excel(name = "评价条数")
    private String reviewCount;

    /** 上下架状态 */
    @Excel(name = "上下架状态")
    private String publishStatus;

    /** 审核状态 */
    @Excel(name = "审核状态")
    private String verifyStatus;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    /** 乐观锁版本号 */
    @Excel(name = "乐观锁版本号")
    private String version;

    /** SKU 管理信息 */
    private List<MallProductSku> mallProductSkuList;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setCategoryId(String categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryId()
    {
        return categoryId;
    }

    public void setBrandId(String brandId)
    {
        this.brandId = brandId;
    }

    public String getBrandId()
    {
        return brandId;
    }

    public void setSpuName(String spuName)
    {
        this.spuName = spuName;
    }

    public String getSpuName()
    {
        return spuName;
    }

    public void setSpuDescription(String spuDescription)
    {
        this.spuDescription = spuDescription;
    }

    public String getSpuDescription()
    {
        return spuDescription;
    }

    public void setMainImage(String mainImage)
    {
        this.mainImage = mainImage;
    }

    public String getMainImage()
    {
        return mainImage;
    }

    public void setImagesJson(String imagesJson)
    {
        this.imagesJson = imagesJson;
    }

    public String getImagesJson()
    {
        return imagesJson;
    }

    public void setPriceMin(String priceMin)
    {
        this.priceMin = priceMin;
    }

    public String getPriceMin()
    {
        return priceMin;
    }

    public void setPriceMax(String priceMax)
    {
        this.priceMax = priceMax;
    }

    public String getPriceMax()
    {
        return priceMax;
    }

    public void setSalesCount(String salesCount)
    {
        this.salesCount = salesCount;
    }

    public String getSalesCount()
    {
        return salesCount;
    }

    public void setReviewCount(String reviewCount)
    {
        this.reviewCount = reviewCount;
    }

    public String getReviewCount()
    {
        return reviewCount;
    }

    public void setPublishStatus(String publishStatus)
    {
        this.publishStatus = publishStatus;
    }

    public String getPublishStatus()
    {
        return publishStatus;
    }

    public void setVerifyStatus(String verifyStatus)
    {
        this.verifyStatus = verifyStatus;
    }

    public String getVerifyStatus()
    {
        return verifyStatus;
    }

    public void setIsDeleted(String isDeleted)
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted()
    {
        return isDeleted;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public List<MallProductSku> getMallProductSkuList()
    {
        return mallProductSkuList;
    }

    public void setMallProductSkuList(List<MallProductSku> mallProductSkuList)
    {
        this.mallProductSkuList = mallProductSkuList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("categoryId", getCategoryId())
            .append("brandId", getBrandId())
            .append("spuName", getSpuName())
            .append("spuDescription", getSpuDescription())
            .append("mainImage", getMainImage())
            .append("imagesJson", getImagesJson())
            .append("priceMin", getPriceMin())
            .append("priceMax", getPriceMax())
            .append("salesCount", getSalesCount())
            .append("reviewCount", getReviewCount())
            .append("publishStatus", getPublishStatus())
            .append("verifyStatus", getVerifyStatus())
            .append("isDeleted", getIsDeleted())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("version", getVersion())
            .append("mallProductSkuList", getMallProductSkuList())
            .toString();
    }
}
