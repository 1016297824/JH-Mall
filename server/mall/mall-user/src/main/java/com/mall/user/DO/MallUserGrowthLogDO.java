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
 * 用户成长值日志实体
 *
 * <p>对应数据库表 mall_user_growth_log</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user_growth_log")
public class MallUserGrowthLogDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 业务类型（如：下单、签到、评价） */
    @TableField("biz_type")
    private String bizType;

    /** 业务单号 */
    @TableField("biz_no")
    private String bizNo;

    /** 变更类型（0-增加 1-扣减 2-过期） */
    @TableField("change_type")
    private Integer changeType;

    /** 本次成长值 */
    @TableField("growth")
    private Integer growth;

    /** 变更前成长值 */
    @TableField("before_growth")
    private Integer beforeGrowth;

    /** 变更后成长值 */
    @TableField("after_growth")
    private Integer afterGrowth;

    /** 备注 */
    @TableField("remark")
    private String remark;

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
