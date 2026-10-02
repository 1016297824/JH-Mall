package com.mall.payment.DO;

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
 * 支付单 DO
 *
 * <p>对应表 {@code mall_payment}，记录一次支付意图及其在渠道侧的进展。
 * 金额字段单位均为<strong>分</strong>。</p>
 *
 * <p>{@code payment_status} 取值见 {@code PaymentStatusEnum}
 * （0=UNPAID / 1=PAID / 2=FAILED / 3=CLOSED / 4=REFUNDING / 5=REFUNDED）。</p>
 *
 * <p><b>{@code version} 的用法</b>：支付/退款的状态推进走
 * {@code MallPaymentMapper} 上显式的 CAS SQL（{@code WHERE payment_status=? AND version=?}），
 * <b>不</b>依赖 MyBatis-Plus 的 {@code @Version} 自动乐观锁——本项目各模块的
 * {@code MybatisPlusConfig} 均未注册 {@code OptimisticLockerInnerInterceptor}，
 * 该注解实际不生效，故此处仅作为普通字段声明。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@TableName("mall_payment")
public class MallPaymentDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 支付单号，前缀 PAY + 雪花，全局唯一 */
    @TableField("payment_no")
    private String paymentNo;

    /** 关联订单号 */
    @TableField("order_no")
    private String orderNo;

    /** 付款用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 支付金额（单位：分），以订单 pay_amount 快照为准，不重新计算优惠 */
    @TableField("pay_amount")
    private Long payAmount;

    /** 支付渠道编码，与渠道配置表 channel_code 对应 */
    @TableField("channel_code")
    private String channelCode;

    /** 渠道侧支付单号（微信 transaction_id / 支付宝 trade_no），回调按此定位支付单 */
    @TableField("channel_payment_no")
    private String channelPaymentNo;

    /** 渠道侧支付状态原文（如微信的 NOTPAY/SUCCESS），仅作留痕 */
    @TableField("channel_pay_status")
    private String channelPayStatus;

    /** 支付单状态，取值见 PaymentStatusEnum */
    @TableField("payment_status")
    private Integer paymentStatus;

    /** 支付成功时间 */
    @TableField("pay_success_time")
    private LocalDateTime paySuccessTime;

    /** 支付过期时间，与订单 pay_expire_time 保持一致 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 异步通知地址，由渠道配置的 callback_base_url 拼接 */
    @TableField("notify_url")
    private String notifyUrl;

    /** 幂等键，格式 {@code userId_orderNo_channelCode}，DB 唯一约束保证重复发起返回同一支付单 */
    @TableField("idempotent_key")
    private String idempotentKey;

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

    /** 乐观锁版本号，状态推进时作为 CAS 条件 */
    @TableField("version")
    private Integer version;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
