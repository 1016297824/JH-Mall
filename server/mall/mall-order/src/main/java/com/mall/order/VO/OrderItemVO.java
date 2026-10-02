package com.mall.order.VO;

import lombok.Data;

/**
 * 订单项视图对象
 *
 * <p>商品名称、图片、售价均为下单时刻快照，后续商品变更不影响历史订单。
 * 金额单位为分。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class OrderItemVO {

    /** 订单项 ID */
    private Long id;

    /** SPU ID */
    private Long spuId;

    /** SKU ID */
    private Long skuId;

    /** SKU 编码（快照） */
    private String skuCode;

    /** SKU 名称（快照） */
    private String skuName;

    /** SPU 名称（快照） */
    private String spuName;

    /** 商品主图快照 URL */
    private String mainImage;

    /** 销售属性 JSON 快照 */
    private String attrsJson;

    /** 购买数量 */
    private Integer quantity;

    /** 成交单价 */
    private Long price;

    /** 单项总价 */
    private Long totalPrice;
}