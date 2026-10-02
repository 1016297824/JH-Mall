package com.mall.marketing.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 优惠券定义响应
 *
 * <p>C 端「可领券列表」与管理端券定义列表共用。金额字段单位均为<strong>分</strong>。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class CouponDefResp {

    /** 券定义 ID */
    private Long id;

    /** 优惠券名称 */
    private String couponName;

    /** 优惠券类型，取值见 CouponTypeEnum */
    private Integer couponType;

    /** 优惠券类型描述，便于前端直接展示 */
    private String couponTypeDesc;

    /** 优惠面值（单位：分），满减券与无门槛券使用 */
    private Long faceValue;

    /** 折扣率（百分比），折扣券使用，如 85 表示 8.5 折 */
    private Integer discountRate;

    /** 折扣上限（单位：分） */
    private Long discountLimit;

    /** 最低订单金额门槛（单位：分），0 表示无门槛 */
    private Long minOrderAmount;

    /** 发行总量 */
    private Integer totalCount;

    /** 剩余可领取数量 */
    private Integer remainCount;

    /** 每人限领数量 */
    private Integer perUserLimit;

    /** 有效期开始时间 */
    private LocalDateTime useStartTime;

    /** 有效期截止时间 */
    private LocalDateTime useEndTime;

    /** 优惠券状态，取值见 CouponStatusEnum */
    private Integer couponStatus;

    /** 优惠券状态描述，便于前端直接展示 */
    private String couponStatusDesc;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
