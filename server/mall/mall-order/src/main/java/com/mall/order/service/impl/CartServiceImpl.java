package com.mall.order.service.impl;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallCartDO;
import com.mall.order.VO.CartVO;
import com.mall.order.convert.response.CartConvert;
import com.mall.order.dto.request.AddCartRequest;
import com.mall.order.dto.request.UpdateCartRequest;
import com.mall.order.infrastructure.feign.RemoteProductAdapter;
import com.mall.order.mapper.MallCartMapper;
import com.mall.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现
 *
 * <p>对应设计文档 §4.1~4.5。</p>
 *
 * <p><b>关于 §4.6 的 Redis Hash 缓存：本实现未采用。</b>
 * §4.6 约定 key={@code mall:order:cart:{userId}}、field=skuId、value=quantity，
 * 但 §4.5 的读路径与降级路径都必须读 MySQL 拿冗余价格（price 字段仅存于
 * {@code mall_order_cart}），Redis 里只有数量，省不掉那次查询；真正能省掉
 * MySQL 的做法是把整个 VO 列表序列化缓存，但那会引入「双写一致性」风险。
 * 购物车数据量小、单次查询本身很轻，且 MySQL 已是唯一事实来源，
 * 故本实现选择不缓存。此为对设计文档的偏离，**待张坤确认**。</p>
 *
 * <p>设计文档 §9.3 的 {@code mall.order.cart-max-items}（购物车最多项数）配置项
 * 在 §4.1 加购流程中并无对应校验，本实现未使用该配置。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final MallCartMapper cartMapper;
    private final RemoteProductAdapter productAdapter;

    // ═══════════════════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<CartVO> listCart(Long userId) {
        List<MallCartDO> cartList = cartMapper.selectByUserId(userId);
        if (cartList.isEmpty()) {
            return List.of();
        }
        List<Long> skuIds = cartList.stream()
                .map(MallCartDO::getSkuId)
                .distinct()
                .toList();

        // Feign 失败时返回空 Map，自动降级为冗余字段
        Map<Long, ProductSkuDTO> skuMap = productAdapter.batchGetSkuSafely(skuIds);
        return CartConvert.toVOList(cartList, skuMap);
    }

    // ═══════════════════════════════════════════════════════════
    // 增删改
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO addItem(Long userId, AddCartRequest req) {
        Long skuId = req.getSkuId();
        Integer qty = req.getQuantity();
        if (skuId == null || qty == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }
        if (qty <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        // ① 校验 SKU 存在、在售、库存充足
        ProductSkuDTO sku = productAdapter.getSkuSafely(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(sku.getIsOnSale())) {
            throw new BusinessException(ErrorCode.PRODUCT_OFFLINE);
        }
        if (sku.getAvailableQty() == null || sku.getAvailableQty() < qty) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }

        // ② 同 SKU 已存在则合并数量，否则新增
        MallCartDO existing = cartMapper.selectByUserIdAndSkuId(userId, skuId);
        MallCartDO target;
        if (existing != null) {
            int finalQty = existing.getQuantity() + qty;
            if (sku.getAvailableQty() < finalQty) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
            cartMapper.updateQuantity(existing.getId(), finalQty);
            existing.setQuantity(finalQty);
            target = existing;
        } else {
            MallCartDO cartDO = new MallCartDO();
            cartDO.setUserId(userId);
            cartDO.setSkuId(skuId);
            cartDO.setSpuId(sku.getSpuId());
            cartDO.setSkuCode(sku.getSkuCode());
            cartDO.setSkuName(sku.getSkuName());
            cartDO.setMainImage(sku.getImage());
            cartDO.setPrice(sku.getPrice());
            cartDO.setQuantity(qty);
            cartDO.setIsSelected(1);
            cartDO.setIsDeleted(0);
            cartMapper.insert(cartDO);
            target = cartDO;
        }

        // ③ 回显最新价格与库存
        return CartConvert.toVO(target, productAdapter.getSkuSafely(skuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, Long cartId, UpdateCartRequest req) {
        Integer qty = req.getQuantity();
        if (qty == null) {
            return;
        }
        if (qty <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }
        // ① 校验归属
        MallCartDO cartDO = requireOwnedCart(userId, cartId);

        // ② 新数量不得超实时库存
        ProductSkuDTO sku = productAdapter.getSkuSafely(cartDO.getSkuId());
        if (sku != null && sku.getAvailableQty() != null && sku.getAvailableQty() < qty) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }
        cartMapper.updateQuantity(cartId, qty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelected(Long userId, Long cartId, Integer isSelected) {
        if (isSelected == null || (isSelected != 0 && isSelected != 1)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }
        requireOwnedCart(userId, cartId);
        cartMapper.updateSelected(cartId, isSelected);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long cartId) {
        requireOwnedCart(userId, cartId);
        cartMapper.hardDeleteById(cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        cartMapper.deleteByUserId(userId);
    }

    // ═══════════════════════════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════════════════════════

    /**
     * 取购物车项并校验归属，防止越权操作他人购物车
     */
    private MallCartDO requireOwnedCart(Long userId, Long cartId) {
        MallCartDO cartDO = cartMapper.selectById(cartId);
        if (cartDO == null || !userId.equals(cartDO.getUserId())) {
            // 不区分「不存在」与「非本人」，避免探测
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return cartDO;
    }
}