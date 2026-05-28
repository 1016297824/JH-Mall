package com.mall.user.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户成长值日志实体
 *
 * <p>对应数据库表 mall_user_growth_log</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@TableName("mall_user_growth_log")
public class MallUserGrowthLogDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_no")
    private String bizNo;

    @TableField("change_type")
    private Integer changeType;

    @TableField("growth")
    private Integer growth;

    @TableField("before_growth")
    private Integer beforeGrowth;

    @TableField("after_growth")
    private Integer afterGrowth;

    @TableField("remark")
    private String remark;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getChangeType() {
        return changeType;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public Integer getGrowth() {
        return growth;
    }

    public void setGrowth(Integer growth) {
        this.growth = growth;
    }

    public Integer getBeforeGrowth() {
        return beforeGrowth;
    }

    public void setBeforeGrowth(Integer beforeGrowth) {
        this.beforeGrowth = beforeGrowth;
    }

    public Integer getAfterGrowth() {
        return afterGrowth;
    }

    public void setAfterGrowth(Integer afterGrowth) {
        this.afterGrowth = afterGrowth;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
