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
 * 用户会员信息实体
 *
 * <p>对应数据库表 mall_user_member</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
