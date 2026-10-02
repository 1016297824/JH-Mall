package com.mall.marketing.DO;

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
 * 促销规则 DO
 *
 * <p>对应表 {@code mall_marketing_promotion_rule}，一个活动可挂多条规则，
 * 按 {@code sort_order ASC} 逐条匹配。</p>
 *
 * <p>{@code rule_type} 取值见 {@code RuleTypeEnum}（1=满减 / 2=满折 / 3=免邮）：
 * 满减取 {@code benefit_amount}，满折取 {@code benefit_rate}。</p>
 *
 * <p>{@code is_exclusive=1} 表示互斥规则：多条互斥规则同时命中时只保留优惠最大的那条；
 * 非互斥规则可叠加（设计文档 §3.5）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_marketing_promotion_rule")
public class MallPromotionRuleDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联活动 ID */
    @TableField("promotion_id")
    private Long promotionId;

    /** 规则类型，取值见 RuleTypeEnum */
    @TableField("rule_type")
    private Integer ruleType;

    /** 门槛金额（单位：分），订单总金额需达到该值 */
    @TableField("threshold_amount")
    private Long thresholdAmount;

    /** 优惠金额（单位：分），满减规则使用 */
    @TableField("benefit_amount")
    private Long benefitAmount;

    /** 折扣率（百分比），满折规则使用，如 85 表示 8.5 折 */
    @TableField("benefit_rate")
    private Integer benefitRate;

    /** 是否互斥（0=可叠加，1=互斥），取值见 DDL 注释 */
    @TableField("is_exclusive")
    private Integer isExclusive;

    /** 优先级，越小越先匹配 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 逻辑删除标志（0=未删，1=已删） */
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
