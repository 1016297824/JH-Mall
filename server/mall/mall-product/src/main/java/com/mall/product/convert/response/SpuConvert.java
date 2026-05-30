package com.mall.product.convert.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SkuBriefVO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;

import java.util.Collections;
import java.util.List;

public class SpuConvert {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private SpuConvert() {
    }

    public static SpuVO toSpuVO(MallProductSpuDO spuDO) {
        if (spuDO == null) {
            return null;
        }
        SpuVO vo = new SpuVO();
        vo.setSpuId(String.valueOf(spuDO.getId()));
        vo.setSpuName(spuDO.getSpuName());
        vo.setMainImage(spuDO.getMainImage());
        vo.setPriceMin(spuDO.getPriceMin());
        vo.setPriceMax(spuDO.getPriceMax());
        vo.setSalesCount(spuDO.getSalesCount());
        vo.setCategoryId(String.valueOf(spuDO.getCategoryId()));
        vo.setBrandId(String.valueOf(spuDO.getBrandId()));
        return vo;
    }

    public static SpuDetailVO toSpuDetailVO(MallProductSpuDO spuDO, List<MallProductSkuDO> skuDOList) {
        if (spuDO == null) {
            return null;
        }
        SpuDetailVO vo = new SpuDetailVO();
        vo.setSpuId(String.valueOf(spuDO.getId()));
        vo.setSpuName(spuDO.getSpuName());
        vo.setMainImage(spuDO.getMainImage());
        vo.setPriceMin(spuDO.getPriceMin());
        vo.setPriceMax(spuDO.getPriceMax());
        vo.setSalesCount(spuDO.getSalesCount());
        vo.setCategoryId(String.valueOf(spuDO.getCategoryId()));
        vo.setBrandId(String.valueOf(spuDO.getBrandId()));
        vo.setDescription(spuDO.getSpuDescription());
        vo.setReviewCount(spuDO.getReviewCount());
        vo.setImages(parseImagesJson(spuDO.getImagesJson()));

        if (skuDOList != null) {
            vo.setSkus(skuDOList.stream().map(SpuConvert::toSkuBriefVO).toList());
        } else {
            vo.setSkus(Collections.emptyList());
        }
        return vo;
    }

    private static List<String> parseImagesJson(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static SkuBriefVO toSkuBriefVO(MallProductSkuDO skuDO) {
        SkuBriefVO vo = new SkuBriefVO();
        vo.setSkuId(String.valueOf(skuDO.getId()));
        vo.setSkuName(skuDO.getSkuName());
        vo.setPrice(skuDO.getPrice());
        vo.setImage(skuDO.getImage());
        return vo;
    }
}
