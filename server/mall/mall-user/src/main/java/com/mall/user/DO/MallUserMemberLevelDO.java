package com.mall.user.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户会员等级实体
 *
 * <p>对应数据库表 mall_user_member_level</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public Integer getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(Integer levelValue) {
        this.levelValue = levelValue;
    }

    public Integer getMinGrowth() {
        return minGrowth;
    }

    public void setMinGrowth(Integer minGrowth) {
        this.minGrowth = minGrowth;
    }

    public Integer getMaxGrowth() {
        return maxGrowth;
    }

    public void setMaxGrowth(Integer maxGrowth) {
        this.maxGrowth = maxGrowth;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getBenefitsJson() {
        return benefitsJson;
    }

    public void setBenefitsJson(String benefitsJson) {
        this.benefitsJson = benefitsJson;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
