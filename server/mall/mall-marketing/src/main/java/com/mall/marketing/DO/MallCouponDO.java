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
 * 优惠券定义 DO
 *
 * <p>对应表 {@code mall_marketing_coupon}，商家创建的券模板，控制发行量、有效期与限领数。
 * 金额字段单位均为<strong>分</strong>。</p>
 *
 * <p>{@code coupon_type} 取值见 {@code CouponTypeEnum}（1=满减 / 2=折扣 / 3=无门槛），
 * {@code coupon_status} 取值见 {@code CouponStatusEnum}（0=草稿 / 1=已发布 / 2=已结束 / 3=已废弃）。</p>
 *
 * <p><b>{@code version} 的用法</b>：领券扣减走 {@code MallCouponMapper.decreaseRemainCount}
 * 的显式 SQL（{@code WHERE id=? AND version=? AND remain_count>0}），
 * <b>不</b>依赖 MyBatis-Plus 的 {@code @Version} 自动乐观锁——本项目各模块的
 * {@code MybatisPlusConfig} 均未注册 {@code OptimisticLockerInnerInterceptor}，
 * 该注解实际不生效，故此处仅作为普通字段声明。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_marketing_coupon")
public class MallCouponDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 优惠券名称 */
    @TableField("coupon_name")
    private String couponName;

    /** 优惠券类型，取值见 CouponTypeEnum */
    @TableField("coupon_type")
    private Integer couponType;

    /** 优惠面值（单位：分），满减券与无门槛券使用 */
    @TableField("face_value")
    private Long faceValue;

    /** 折扣率（百分比），折扣券使用，如 85 表示 8.5 折 */
    @TableField("discount_rate")
    private Integer discountRate;

    /** 折扣上限（单位：分），折扣券使用，防止大额订单折扣过猛 */
    @TableField("discount_limit")
    private Long discountLimit;

    /** 最低订单金额门槛（单位：分），低于该值不可用 */
    @TableField("min_order_amount")
    private Long minOrderAmount;

    /** 发行总量 */
    @TableField("total_count")
    private Integer totalCount;

    /** 剩余可领取数量，领券时乐观锁扣减，取消订单时回补 */
    @TableField("remain_count")
    private Integer remainCount;

    /** 每人限领数量 */
    @TableField("per_user_limit")
    private Integer perUserLimit;

    /** 有效期开始时间 */
    @TableField("use_start_time")
    private LocalDateTime useStartTime;

    /** 有效期截止时间，领券记录的 expire_time 由此冗余 */
    @TableField("use_end_time")
    private LocalDateTime useEndTime;

    /** 优惠券状态，取值见 CouponStatusEnum */
    @TableField("coupon_status")
    private Integer couponStatus;

    /** 逻辑删除标志（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 乐观锁版本号，领券扣减库存时作为 CAS 条件 */
    @TableField("version")
    private Integer version;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
