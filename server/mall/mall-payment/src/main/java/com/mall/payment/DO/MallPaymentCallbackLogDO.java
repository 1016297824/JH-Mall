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
 * 支付回调记录 DO
 *
 * <p>对应表 {@code mall_payment_callback_log}，完整留痕每一次渠道回调原始报文，
 * 用于对账、问题复现与审计（不可篡改的支付证据）。</p>
 *
 * <p>{@code is_verified}：0=未验签 / 1=验签通过 / 2=验签失败；
 * {@code process_status}：0=待处理 / 1=处理成功 / 2=处理失败。</p>
 *
 * <p>该表<b>没有</b> {@code version} 与 {@code create_by}/{@code update_by} 列——
 * 回调日志只增不改（仅回填处理结果），不参与并发状态推进。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@TableName("mall_payment_callback_log")
public class MallPaymentCallbackLogDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联支付单号，退款回调时为 null */
    @TableField("payment_no")
    private String paymentNo;

    /** 关联退款单号，支付回调时为 null */
    @TableField("refund_no")
    private String refundNo;

    /** 渠道编码 */
    @TableField("channel_code")
    private String channelCode;

    /** 回调类型（支付回调 / 退款回调） */
    @TableField("callback_type")
    private String callbackType;

    /** 原始回调报文 JSON，问题排查时可直接复现 */
    @TableField("raw_body")
    private String rawBody;

    /** 验签结果：0=未验签 1=验签通过 2=验签失败 */
    @TableField("is_verified")
    private Integer isVerified;

    /** 处理状态：0=待处理 1=处理成功 2=处理失败 */
    @TableField("process_status")
    private Integer processStatus;

    /** 处理完成时间 */
    @TableField("process_time")
    private LocalDateTime processTime;

    /** 处理结果说明 */
    @TableField("process_result")
    private String processResult;

    /** 回调防重放 nonce，DB 唯一约束作为 Redis 不可用时的兜底 */
    @TableField("nonce")
    private String nonce;

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
