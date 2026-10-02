package com.mall.order.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后单视图对象
 *
 * <p>金额单位为分。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class AfterSaleVO {

    /** 售后单 ID */
    private Long id;

    /** 售后单号 */
    private String afterSaleNo;

    /** 关联订单号 */
    private String orderNo;

    /** 关联订单项 ID */
    private Long orderItemId;

    /** 售后类型描述，取值见 AfterSaleTypeEnum */
    private String afterSaleTypeDesc;

    /** 售后状态描述，取值见 AfterSaleStatusEnum */
    private String afterSaleStatusDesc;

    /** 退款原因 */
    private String reason;

    /** 退款金额 */
    private Long amount;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime approveTime;

    /** 审核意见 */
    private String approveRemark;

    /** 退货物流公司 */
    private String returnExpressCompany;

    /** 退货物流单号 */
    private String returnExpressNo;
}