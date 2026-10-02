package com.mall.marketing.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 用户优惠券记录响应
 *
 * <p>「我的优惠券」列表项。金额字段单位均为<strong>分</strong>。</p>
 *
 * <p>券名称 / 类型 / 门槛等来自 {@code mall_marketing_coupon}，由 Service 层批量补齐，
 * 避免逐条查库（N+1）。{@code faceValue} / {@code expireTime} 取自券记录自身的
 * <strong>快照</strong>（领取时刻的值），而非券定义的当前值——券定义后续被修改
 * 不影响已领出的券。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class CouponRecordResp {

    /** 券记录 ID */
    private Long id;

    /** 关联优惠券定义 ID */
    private Long couponId;

    /** 优惠券名称（来自券定义） */
    private String couponName;

    /** 优惠券类型（来自券定义），取值见 CouponTypeEnum */
    private Integer couponType;

    /** 优惠券编码（全局唯一） */
    private String couponCode;

    /** 券面值快照（单位：分） */
    private Long faceValue;

    /** 最低订单金额门槛（单位：分，来自券定义） */
    private Long minOrderAmount;

    /** 记录状态，取值见 CouponRecordStatusEnum */
    private Integer recordStatus;

    /** 记录状态描述，便于前端直接展示 */
    private String recordStatusDesc;

    /** 锁定 / 使用的订单号，未占用时为 null */
    private String orderNo;

    /** 锁定时间 */
    private LocalDateTime lockTime;

    /** 使用（核销）时间 */
    private LocalDateTime useTime;

    /** 过期时间（领取时的快照） */
    private LocalDateTime expireTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
