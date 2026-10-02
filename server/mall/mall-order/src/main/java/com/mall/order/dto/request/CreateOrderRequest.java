package com.mall.order.dto.request;

import lombok.Data;

/**
 * 创建订单请求
 *
 * <p>商品明细不在请求体中——下单流程从<b>当前用户已选中的购物车项</b>
 * （{@code mall_order_cart.is_selected = 1}）读取，见设计文档 §5.3。</p>
 *
 * <p>幂等键从请求头 {@code Idempotent-Key} 取，不放请求体，见 §5.2。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class CreateOrderRequest {

    /** 收货地址 ID，需属于当前用户 */
    private Long addressId;

    /**
     * 使用的优惠券记录 ID（可选）
     *
     * <p>为空则由 mall-marketing 试算引擎在用户可用券中自动择优；
     * 非空则只试算这一张。</p>
     */
    private Long couponRecordId;

    /** 买家备注，最长 200字符 */
    private String remark;
}