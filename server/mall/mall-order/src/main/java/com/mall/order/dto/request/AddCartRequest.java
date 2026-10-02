package com.mall.order.dto.request;

import lombok.Data;

/**
 * 加入购物车请求
 *
 * <p>校验在 {@code CartServiceImpl.addItem} 中显式完成
 * （未使用 jakarta.validation 注解，因mall-order 依赖链未确认引入 validation starter）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class AddCartRequest {

    /** SKU ID */
    private Long skuId;

    /** 加入数量，必须大于 0 */
    private Integer quantity;
}