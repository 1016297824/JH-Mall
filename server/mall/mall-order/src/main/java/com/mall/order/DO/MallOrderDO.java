package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 订单 DO
 *
 * <p>对应表 {@code mall_order}。金额字段单位均为<strong>分</strong>。</p>
 *
 * <p>{@code order_status} 取值见 {@code mall-common} 的 {@code OrderStatusEnum}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order")
public class MallOrderDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号（业务唯一键） */
    @TableField("order_no")
    private String orderNo;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 订单状态，取值见 OrderStatusEnum */
    @TableField("order_status")
    private Integer orderStatus;

    /** 商品总金额（单位：分） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 优惠总金额（单位：分） */
    @TableField("discount_amount")
    private Long discountAmount;

    /** 运费金额（单位：分） */
    @TableField("freight_amount")
    private Long freightAmount;

    /** 实付金额（单位：分） */
    @TableField("pay_amount")
    private Long payAmount;

    /** 支付成功时间 */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 发货时间 */
    @TableField("delivery_time")
    private LocalDateTime deliveryTime;

    /** 交易完成时间 */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /** 取消时间 */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 取消类型：USER_CANCEL / PAY_TIMEOUT / FORCE_CANCEL */
    @TableField("cancel_type")
    private String cancelType;

    /** 取消原因 */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 退款前状态码
     *
     * <p>进入 {@code REFUNDING} 时由 {@code OrderStateMachine} 写入原状态，
     * 退款失败时据此精确回退，取值见 {@code OrderStatusEnum}。</p>
     *
     * <p>字段由 V1.0.6 迁移脚本新增，非退款中时为 {@code null}。</p>
     */
    @TableField("pre_refund_status")
    private Integer preRefundStatus;

    /** 物流公司（发货时填写） */
    @TableField("logistics_company")
    private String logisticsCompany;

    /** 物流单号（发货时填写） */
    @TableField("logistics_no")
    private String logisticsNo;

    /** 支付过期时间，WAIT_PAY 超过此时间应关单 */
    @TableField("pay_expire_time")
    private LocalDateTime payExpireTime;

    /** 买家备注 */
    @TableField("remark")
    private String remark;

    /** 幂等键 */
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

    /** 乐观锁版本号（超时关单竞态防护） */
    @Version
    @TableField("version")
    private Integer version;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}