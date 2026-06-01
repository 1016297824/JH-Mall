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

/**
 * SPU 转换器（DO → VO / DetailVO）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class SpuConvert {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 批量 SPU DO 转 VO 列表
     *
     * @param spuDOList SPU DO 列表
     * @return SPU VO 列表
     */
    public static List<SpuVO> toSpuVOList(List<MallProductSpuDO> spuDOList) {
        if (spuDOList == null || spuDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return spuDOList.stream().map(SpuConvert::toSpuVO).toList();
    }

    private SpuConvert() {
    }

    /**
     * SPU DO 转 VO（列表用）
     *
     * @param spuDO SPU DO
     * @return SPU VO
     */
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
        vo.setHotScore(0L);
        return vo;
    }

    /**
     * SPU DO 转详情 VO（含图片列表、SKU 列表）
     *
     * @param spuDO    SPU DO
     * @param skuDOList SKU DO 列表
     * @return SPU 详情 VO
     */
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

    /**
     * 解析图片 JSON 为 URL 列表
     */
    private static List<String> parseImagesJson(String imagesJson) {
        // 空值直接返回空列表
        if (imagesJson == null || imagesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // JSON 数组字符串解析为 List<String>
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 解析失败时静默返回空列表，不阻断主流程
            return Collections.emptyList();
        }
    }

    /**
     * SKU DO 转简要 VO
     */
    private static SkuBriefVO toSkuBriefVO(MallProductSkuDO skuDO) {
        SkuBriefVO vo = new SkuBriefVO();
        vo.setSkuId(String.valueOf(skuDO.getId()));
        vo.setSkuName(skuDO.getSkuName());
        vo.setPrice(skuDO.getPrice());
        vo.setImage(skuDO.getImage());
        return vo;
    }
}
