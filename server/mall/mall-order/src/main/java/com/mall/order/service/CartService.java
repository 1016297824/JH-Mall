package com.mall.order.service;

import com.mall.order.VO.CartVO;
import com.mall.order.dto.request.AddCartRequest;
import com.mall.order.dto.request.UpdateCartRequest;

import java.util.List;

/**
 * 购物车服务
 *
 * <p>对应设计文档 §4。MySQL 为唯一事实来源，Redis Hash 仅作查询加速。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface CartService {

    /**
     * 查询购物车
     *
     * <p>批量拉取mall-product 实时价格与库存；Feign 失败时降级用冗余字段，
     * 并将 {@code availableQty} 置为 -1、{@code onSale} 置 null 表示不可知。</p>
     *
     * @param userId 用户 ID
     * @return 购物车项列表
     */
    List<CartVO> listCart(Long userId);

    /**
     * 加入购物车
     *
     * <p>同一 SKU 已存在则合并数量，合并后不得超实时库存；
     * 购物车总项数不得超 {@code mall.order.cart-max-items}。</p>
     *
     * @param userId 用户 ID
     * @param req    加购请求
     * @return 加购后的购物车项
     */
    CartVO addItem(Long userId, AddCartRequest req);

    /**
     * 修改购物车项数量
     *
     * @param userId 用户 ID
     * @param cartId 购物车项 ID
     * @param req    更新请求（取其中的 quantity）
     */
    void updateQuantity(Long userId, Long cartId, UpdateCartRequest req);

    /**
     * 修改选中状态
     *
     * <p>下单仅处理 {@code is_selected = 1} 的项。</p>
     *
     * @param userId    用户 ID
     * @param cartId    购物车项 ID
     * @param isSelected 1=选中 0=未选
     */
    void updateSelected(Long userId, Long cartId, Integer isSelected);

    /**
     * 删除购物车项
     *
     * @param userId 用户 ID
     * @param cartId 购物车项 ID
     */
    void removeItem(Long userId, Long cartId);

    /**
     * 清空购物车
     *
     * @param userId 用户 ID
     */
    void clearCart(Long userId);
}