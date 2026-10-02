package com.mall.marketing.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 创建优惠券定义请求
 *
 * <p>新建的券一律落为「草稿」态（{@code coupon_status=0}），发布是独立操作——
 * 详见设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.1。</p>
 *
 * <p>字段分组取值：{@code couponType=1}（满减）用 {@code faceValue}；
 * {@code couponType=2}（折扣）用 {@code discountRate} + {@code discountLimit}；
 * {@code couponType=3}（无门槛）用 {@code faceValue} 且 {@code minOrderAmount=0}。</p>
 *
 * <p>本类只承载数据，<b>不</b>用 Jakarta Validation 注解——类型相关的组合校验
 * （如「满减券面值必须 > 0」）在 Service 层统一判定，避免校验规则散落两处。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class CreateCouponReq {

    /** 优惠券名称 */
    private String couponName;

    /** 优惠券类型，取值见 CouponTypeEnum */
    private Integer couponType;

    /** 优惠面值（单位：分），满减券与无门槛券必填且 > 0 */
    private Long faceValue;

    /** 折扣率（百分比），折扣券必填，取值 1~99 */
    private Integer discountRate;

    /** 折扣上限（单位：分），折扣券可选 */
    private Long discountLimit;

    /** 最低订单金额门槛（单位：分），默认 0 表示无门槛 */
    private Long minOrderAmount;

    /** 发行总量 */
    private Integer totalCount;

    /** 每人限领数量，须 > 0 且不超过发行总量 */
    private Integer perUserLimit;

    /** 有效期开始时间 */
    private LocalDateTime useStartTime;

    /** 有效期截止时间，须晚于开始时间 */
    private LocalDateTime useEndTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
