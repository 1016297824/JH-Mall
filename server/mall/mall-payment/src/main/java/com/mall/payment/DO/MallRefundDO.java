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
 * 退款单 DO
 *
 * <p>对应表 {@code mall_payment_refund}，由 mall-order 售后审核通过后经 Feign 触发创建。
 * 金额字段单位均为<strong>分</strong>。</p>
 *
 * <p>{@code refund_status} 取值见 {@code RefundStatusEnum}（0=PROCESSING / 1=SUCCESS / 2=FAILED）。</p>
 *
 * <p><b>{@code version} 的用法</b>同 {@link MallPaymentDO}：走显式 CAS SQL，不依赖 {@code @Version}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@TableName("mall_payment_refund")
public class MallRefundDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 退款单号，前缀 REF + 雪花，全局唯一 */
    @TableField("refund_no")
    private String refundNo;

    /** 关联支付单 ID */
    @TableField("payment_id")
    private Long paymentId;

    /** 关联订单号 */
    @TableField("order_no")
    private String orderNo;

    /** 关联售后单号，与渠道编码共同构成幂等键 */
    @TableField("after_sale_no")
    private String afterSaleNo;

    /** 退款用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 退款金额（单位：分），累计退款不得超过原支付金额 */
    @TableField("refund_amount")
    private Long refundAmount;

    /** 退款原因 */
    @TableField("refund_reason")
    private String refundReason;

    /** 退款渠道编码，必须与原始支付渠道一致 */
    @TableField("channel_code")
    private String channelCode;

    /** 渠道侧退款单号，退款回调按此定位退款单 */
    @TableField("channel_refund_no")
    private String channelRefundNo;

    /** 渠道侧退款状态原文，仅作留痕 */
    @TableField("channel_refund_status")
    private String channelRefundStatus;

    /** 退款单状态，取值见 RefundStatusEnum */
    @TableField("refund_status")
    private Integer refundStatus;

    /** 退款成功时间 */
    @TableField("refund_success_time")
    private LocalDateTime refundSuccessTime;

    /** 幂等键，格式 {@code afterSaleNo_channelCode}，DB 唯一约束保证重复调用返回同一退款单 */
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
