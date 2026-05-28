package com.mall.user.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户会员信息实体
 *
 * <p>对应数据库表 mall_user_member</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@TableName("mall_user_member")
public class MallUserMemberDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 会员等级ID */
    @TableField("level_id")
    private Long levelId;

    /** 当前成长值 */
    @TableField("growth")
    private Integer growth;

    /** 累计成长值 */
    @TableField("total_growth")
    private Integer totalGrowth;

    /** 等级有效期开始时间 */
    @TableField("level_start_time")
    private LocalDateTime levelStartTime;

    /** 等级有效期结束时间 */
    @TableField("level_end_time")
    private LocalDateTime levelEndTime;

    /** 成为会员时间 */
    @TableField("become_time")
    private LocalDateTime becomeTime;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLevelId() {
        return levelId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public Integer getGrowth() {
        return growth;
    }

    public void setGrowth(Integer growth) {
        this.growth = growth;
    }

    public Integer getTotalGrowth() {
        return totalGrowth;
    }

    public void setTotalGrowth(Integer totalGrowth) {
        this.totalGrowth = totalGrowth;
    }

    public LocalDateTime getLevelStartTime() {
        return levelStartTime;
    }

    public void setLevelStartTime(LocalDateTime levelStartTime) {
        this.levelStartTime = levelStartTime;
    }

    public LocalDateTime getLevelEndTime() {
        return levelEndTime;
    }

    public void setLevelEndTime(LocalDateTime levelEndTime) {
        this.levelEndTime = levelEndTime;
    }

    public LocalDateTime getBecomeTime() {
        return becomeTime;
    }

    public void setBecomeTime(LocalDateTime becomeTime) {
        this.becomeTime = becomeTime;
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
