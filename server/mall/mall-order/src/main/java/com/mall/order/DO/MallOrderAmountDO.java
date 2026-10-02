package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 订单金额快照 DO
 *
 * <p>对应表 {@code mall_order_amount}。固化下单时刻的金额构成，
 * 便于售后对账与争议追溯。</p>
 *
 * <p>表无 version 列，故不做乐观锁。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order_amount")
public class MallOrderAmountDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    @TableField("order_id")
    private Long orderId;

    /** 商品明细快照 JSON */
    @TableField("items_json")
    private String itemsJson;

    /** 优惠券使用快照 JSON */
    @TableField("coupon_snapshot_json")
    private String couponSnapshotJson;

    /** 活动优惠快照 JSON */
    @TableField("promotion_snapshot_json")
    private String promotionSnapshotJson;

    /** 积分抵扣金额（单位：分） */
    @TableField("points_discount")
    private Long pointsDiscount;

    /** 商品总金额（单位：分） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 优惠总金额（单位：分） */
    @TableField("discount_amount")
    private Long discountAmount;

    /** 运费（单位：分） */
    @TableField("freight_amount")
    private Long freightAmount;

    /** 实付金额（单位：分） */
    @TableField("pay_amount")
    private Long payAmount;

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