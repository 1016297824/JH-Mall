package com.mall.order.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 售后单 DO
 *
 * <p>对应表 {@code mall_order_after_sale}。</p>
 *
 * <p>{@code after_sale_type} 取值见 {@code mall-common} 的 {@code AfterSaleTypeEnum}，
 * {@code after_sale_status} 取值见 {@code AfterSaleStatusEnum}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_order_after_sale")
public class MallAfterSaleDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 售后单号 */
    @TableField("after_sale_no")
    private String afterSaleNo;

    /** 关联订单 ID */
    @TableField("order_id")
    private Long orderId;

    /** 关联订单项 ID（整单退款时为空） */
    @TableField("order_item_id")
    private Long orderItemId;

    /** 申请人用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 售后类型：1=仅退款 2=退货退款，取值见 AfterSaleTypeEnum */
    @TableField("after_sale_type")
    private Integer afterSaleType;

    /** 退款原因 */
    @TableField("reason")
    private String reason;

    /** 退款金额（单位：分） */
    @TableField("amount")
    private Long amount;

    /** 售后状态，取值见 AfterSaleStatusEnum */
    @TableField("after_sale_status")
    private Integer afterSaleStatus;

    /** 申请时间 */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 审核时间 */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 审核意见 */
    @TableField("approve_remark")
    private String approveRemark;

    /** 退货物流公司 */
    @TableField("return_express_company")
    private String returnExpressCompany;

    /** 退货物流单号 */
    @TableField("return_express_no")
    private String returnExpressNo;

    /** 商家确认收货时间（退货退款回补库存的依据） */
    @TableField("receipt_time")
    private LocalDateTime receiptTime;

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}