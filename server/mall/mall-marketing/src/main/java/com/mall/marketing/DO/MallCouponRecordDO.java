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
 * 用户优惠券记录 DO
 *
 * <p>对应表 {@code mall_marketing_coupon_record}，记录用户从领券到核销/释放的完整生命周期。</p>
 *
 * <p>{@code record_status} 取值见 {@code CouponRecordStatusEnum}：
 * 1=AVAILABLE 可用 / 2=LOCKED 已锁定 / 3=USED 已使用 / 4=RELEASED 已释放 / 5=EXPIRED 已过期。
 * 状态只能经 {@code CouponStateMachine.transition()} 变更。</p>
 *
 * <p>{@code face_value} 与 {@code expire_time} 是领取时刻的<strong>快照</strong>，
 * 由 {@code mall_marketing_coupon} 冗余而来，券定义后续被修改不影响已领出的券。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_marketing_coupon_record")
public class MallCouponRecordDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联优惠券定义 ID */
    @TableField("coupon_id")
    private Long couponId;

    /** 领取用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 优惠券编码（全局唯一，uk_coupon_code），格式 CPN + 时间戳 + 随机数 */
    @TableField("coupon_code")
    private String couponCode;

    /** 记录状态，取值见 CouponRecordStatusEnum */
    @TableField("record_status")
    private Integer recordStatus;

    /** 使用/锁定的订单号，释放时清空 */
    @TableField("order_no")
    private String orderNo;

    /** 券面值快照（单位：分） */
    @TableField("face_value")
    private Long faceValue;

    /** 锁定时间，下单锁券时记录 */
    @TableField("lock_time")
    private LocalDateTime lockTime;

    /** 使用（核销）时间 */
    @TableField("use_time")
    private LocalDateTime useTime;

    /** 释放时间，订单取消解锁时记录 */
    @TableField("release_time")
    private LocalDateTime releaseTime;

    /** 过期时间，从券定义的 use_end_time 冗余 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 逻辑删除标志（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间，即领券时间 */
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
