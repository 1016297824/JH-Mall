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
 * @date 2026/05/28
 */
@TableName("mall_user_member_level")
public class MallUserMemberLevelDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("level_name")
    private String levelName;

    @TableField("level_value")
    private Integer levelValue;

    @TableField("min_growth")
    private Integer minGrowth;

    @TableField("max_growth")
    private Integer maxGrowth;

    @TableField("icon")
    private String icon;

    @TableField("benefits_json")
    private String benefitsJson;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

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
