package com.mall.order.dto.request;

import lombok.Data;

/**
 * 提交售后申请请求
 *
 * <p>对应设计文档 §8.2。仅退款（未发货）与退货退款（已发货）的区别
 * 体现在 {@code afterSaleType}，由服务端根据订单状态校验合法性。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class SubmitAfterSaleRequest {

    /** 订单号 */
    private String orderNo;

    /** 关联订单项 ID；整单退款时可不传 */
    private Long orderItemId;

    /**
     * 售后类型
     *
     * <p>1=仅退款 2=退货退款，取值见 {@code AfterSaleTypeEnum}。</p>
     */
    private Integer afterSaleType;

    /** 退款原因 */
    private String reason;

    /**
     * 申请退款金额（单位：分）
     *
     * <p>不传则按订单实付金额全额退。</p>
     */
    private Long amount;
}