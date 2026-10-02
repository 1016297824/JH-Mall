package com.mall.marketing.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 修改优惠券定义请求
 *
 * <p>字段可见范围由券的当前状态决定（设计文档 §3.1）：</p>
 * <ul>
 *   <li><b>草稿</b>（{@code coupon_status=0}）：可修改全部字段</li>
 *   <li><b>已发布</b>（{@code coupon_status=1}）：<b>仅允许修改名称</b>——改类型或面值
 *       会让已领出的券与新定义不一致，故一律忽略其余字段</li>
 *   <li><b>已结束 / 已废弃</b>：不允许修改，抛 A0503</li>
 * </ul>
 *
 * <p>注：设计 §3.1 提到「已发布可改名称、描述」，但 {@code mall_marketing_coupon}
 * 表<b>没有</b> {@code description} 列，故只保留名称。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class UpdateCouponReq {

    /** 券定义 ID，必填 */
    private Long id;

    /** 优惠券名称 */
    private String couponName;

    /** 优惠券类型，取值见 CouponTypeEnum，仅草稿态可改 */
    private Integer couponType;

    /** 优惠面值（单位：分），仅草稿态可改 */
    private Long faceValue;

    /** 折扣率（百分比），仅草稿态可改 */
    private Integer discountRate;

    /** 折扣上限（单位：分），仅草稿态可改 */
    private Long discountLimit;

    /** 最低订单金额门槛（单位：分），仅草稿态可改 */
    private Long minOrderAmount;

    /** 发行总量，仅草稿态可改 */
    private Integer totalCount;

    /** 每人限领数量，仅草稿态可改 */
    private Integer perUserLimit;

    /** 有效期开始时间，仅草稿态可改 */
    private LocalDateTime useStartTime;

    /** 有效期截止时间，仅草稿态可改 */
    private LocalDateTime useEndTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
