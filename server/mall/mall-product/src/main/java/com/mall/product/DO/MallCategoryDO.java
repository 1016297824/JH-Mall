package com.mall.product.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 类目 DO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_product_category")
public class MallCategoryDO {

    /** 类目 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父类目 ID（0=顶级） */
    @TableField("parent_id")
    private Long parentId;

    /** 类目名称 */
    @TableField("name")
    private String name;

    /** 层级（1/2/3） */
    @TableField("level")
    private Integer level;

    /** 图标 URL */
    @TableField("icon")
    private String icon;

    /** 排序值 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否可见（0=隐藏，1=可见） */
    @TableField("is_visible")
    private Integer isVisible;

    /** 路径，如 /1/2/3（从根到当前节点的 ID 链，用于快速查询子类目） */
    @TableField("path")
    private String path;

    /** 逻辑删除标识（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
