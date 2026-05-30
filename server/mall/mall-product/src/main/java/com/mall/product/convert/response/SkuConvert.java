package com.mall.product.convert.response;

import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.VO.SkuVO;

import java.util.List;

public class SkuConvert {

    private SkuConvert() {
    }

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

    public static List<SkuVO> toSkuVOList(List<MallProductSkuDO> skuDOList) {
        return skuDOList.stream().map(SkuConvert::toSkuVO).toList();
    }
}
