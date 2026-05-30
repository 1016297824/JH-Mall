package com.mall.product.convert.response;

import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.VO.SkuVO;

import java.util.List;

/**
 * SKU 转换器（DO → VO）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class SkuConvert {

    private SkuConvert() {
    }

    /**
     * SKU DO 转 VO
     *
     * @param skuDO SKU DO
     * @return SKU VO
     */
    public static SkuVO toSkuVO(MallProductSkuDO skuDO) {
        if (skuDO == null) {
            return null;
        }
        SkuVO vo = new SkuVO();
        vo.setSkuId(String.valueOf(skuDO.getId()));
        vo.setSpuId(String.valueOf(skuDO.getSpuId()));
        vo.setSkuCode(skuDO.getSkuCode());
        vo.setSkuName(skuDO.getSkuName());
        vo.setAttrsJson(skuDO.getAttrsJson());
        vo.setPrice(skuDO.getPrice());
        vo.setMarketPrice(skuDO.getMarketPrice());
        vo.setImage(skuDO.getImage());
        vo.setWeight(skuDO.getWeight());
        return vo;
    }

    /**
     * SKU DO 列表转 VO 列表
     *
     * @param skuDOList SKU DO 列表
     * @return SKU VO 列表
     */
    public static List<SkuVO> toSkuVOList(List<MallProductSkuDO> skuDOList) {
        return skuDOList.stream().map(SkuConvert::toSkuVO).toList();
    }
}
