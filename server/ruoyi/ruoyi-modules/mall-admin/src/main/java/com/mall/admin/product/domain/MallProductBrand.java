package com.mall.admin.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 品牌管理对象 mall_product_brand
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallProductBrand extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 品牌名称 */
    @Excel(name = "品牌名称")
    private String name;

    /** 品牌 Logo URL */
    @Excel(name = "品牌 Logo URL")
    private String logo;

    /** 品牌简介 */
    @Excel(name = "品牌简介")
    private String description;

    /** 排序值 */
    @Excel(name = "排序值")
    private String sortOrder;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setLogo(String logo)
    {
        this.logo = logo;
    }

    public String getLogo()
    {
        return logo;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setSortOrder(String sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getSortOrder()
    {
        return sortOrder;
    }

    public void setIsDeleted(String isDeleted)
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted()
    {
        return isDeleted;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("logo", getLogo())
            .append("description", getDescription())
            .append("sortOrder", getSortOrder())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
