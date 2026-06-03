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
 * 用户积分日志实体
 *
 * <p>对应数据库表 mall_user_points_log</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user_points_log")
public class MallUserPointsLogDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 业务类型（如：下单、签到、兑换） */
    @TableField("biz_type")
    private String bizType;

    /** 业务单号 */
    @TableField("biz_no")
    private String bizNo;

    /** 变更类型（0-增加 1-扣减 2-过期 3-冻结 4-解冻） */
    @TableField("change_type")
    private Integer changeType;

    /** 本次积分 */
    @TableField("points")
    private Integer points;

    /** 变更前积分 */
    @TableField("before_points")
    private Integer beforePoints;

    /** 变更后积分 */
    @TableField("after_points")
    private Integer afterPoints;

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
