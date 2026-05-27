package com.mall.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.TreeEntity;

/**
 * 商品类目对象 mall_product_category
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public class MallProductCategory extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 类目名称 */
    @Excel(name = "类目名称")
    private String name;

    /** 类目层级 */
    @Excel(name = "类目层级")
    private String level;

    /** 类目标识图标 URL */
    @Excel(name = "类目标识图标 URL")
    private String icon;

    /** 排序值，越小越靠前 */
    @Excel(name = "排序值，越小越靠前")
    private String sortOrder;

    /** 是否前端可见 */
    @Excel(name = "是否前端可见")
    private String isVisible;

    /** 类目路径，如 /1/10/100 */
    @Excel(name = "类目路径，如 /1/10/100")
    private String path;

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

    public void setLevel(String level) 
    {
        this.level = level;
    }

    public String getLevel() 
    {
        return level;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
    }

    public void setSortOrder(String sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public String getSortOrder() 
    {
        return sortOrder;
    }

    public void setIsVisible(String isVisible) 
    {
        this.isVisible = isVisible;
    }

    public String getIsVisible() 
    {
        return isVisible;
    }

    public void setPath(String path) 
    {
        this.path = path;
    }

    public String getPath() 
    {
        return path;
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
            .append("parentId", getParentId())
            .append("name", getName())
            .append("level", getLevel())
            .append("icon", getIcon())
            .append("sortOrder", getSortOrder())
            .append("isVisible", getIsVisible())
            .append("path", getPath())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
