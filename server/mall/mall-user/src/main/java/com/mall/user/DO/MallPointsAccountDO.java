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
 * 积分账户实体
 *
 * <p>对应数据库表 mall_user_points_account</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user_points_account")
public class MallPointsAccountDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 累计积分 */
    @TableField("total_points")
    private Integer totalPoints;

    /** 可用积分 */
    @TableField("available_points")
    private Integer availablePoints;

    /** 已用积分 */
    @TableField("used_points")
    private Integer usedPoints;

    /** 已过期积分 */
    @TableField("expired_points")
    private Integer expiredPoints;

    /** 版本号（乐观锁） */
    @TableField("version")
    private Integer version;

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
