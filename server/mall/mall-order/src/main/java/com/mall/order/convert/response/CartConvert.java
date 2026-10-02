package com.mall.order.convert.response;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.order.DO.MallCartDO;
import com.mall.order.VO.CartVO;

import java.util.List;
import java.util.Map;

/**
 * 购物车转换器
 *
 * <p>静态方法，无状态。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class CartConvert {

    /** 实时数据不可用时的库存占位值 */
    public static final int UNKNOWN_STOCK = -1;

    private CartConvert() {
    }

    /**
     * 单项转换
     *
     * @param cartDO购物车项
     * @param sku   mall-product 实时 SKU 信息；为空表示 Feign 降级
     * @return 视图对象
     */
    public static CartVO toVO(MallCartDO cartDO, ProductSkuDTO sku) {
        CartVO vo = new CartVO();
        vo.setId(cartDO.getId());
        vo.setSkuId(cartDO.getSkuId());
        vo.setSpuId(cartDO.getSpuId());
        vo.setSkuCode(cartDO.getSkuCode());
        vo.setSkuName(cartDO.getSkuName());
        vo.setMainImage(cartDO.getMainImage());
        vo.setQuantity(cartDO.getQuantity());
        vo.setIsSelected(cartDO.getIsSelected());

        if (sku != null) {
            // 实时数据优先
            vo.setPrice(sku.getPrice());
            vo.setAvailableQty(sku.getAvailableQty());
            vo.setOnSale(sku.getIsOnSale());
            vo.setPurchasable(Boolean.TRUE.equals(sku.getIsOnSale())
                    && sku.getAvailableQty() != null
                    && sku.getAvailableQty() >= cartDO.getQuantity());
        } else {
            // 降级：用购物车表冗余价格，库存与在售状态标记为不可知
            vo.setPrice(cartDO.getPrice());
            vo.setAvailableQty(UNKNOWN_STOCK);
            vo.setOnSale(null);
            vo.setPurchasable(null);
        }
        return vo;
    }

    /**
     * 批量转换
     *
     * @param cartList 购物车项
     * @param skuMap   skuId → SKU 信息，可为空 Map
     * @return 视图对象列表
     */
    public static List<CartVO> toVOList(List<MallCartDO> cartList, Map<Long, ProductSkuDTO> skuMap) {
        return cartList.stream()
                .map(cart -> toVO(cart, skuMap.get(cart.getSkuId())))
                .toList();
    }
}