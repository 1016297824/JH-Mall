package com.mall.user.DO;

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
 * 用户会员等级实体
 *
 * <p>对应数据库表 mall_user_member_level</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user_member_level")
public class MallUserMemberLevelDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 等级名称（如：普通会员、银卡会员、金卡会员） */
    @TableField("level_name")
    private String levelName;

    /** 等级数值（数字越大等级越高） */
    @TableField("level_value")
    private Integer levelValue;

    /** 最低成长值（含） */
    @TableField("min_growth")
    private Integer minGrowth;

    /** 最高成长值（含） */
    @TableField("max_growth")
    private Integer maxGrowth;

    /** 等级图标URL */
    @TableField("icon")
    private String icon;

    /** 等级权益JSON */
    @TableField("benefits_json")
    private String benefitsJson;

    /** 逻辑删除标记（0-未删除 1-已删除） */
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
