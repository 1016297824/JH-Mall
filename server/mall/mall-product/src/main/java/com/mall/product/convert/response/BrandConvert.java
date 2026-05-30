package com.mall.product.convert.response;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;

import java.util.List;

/**
 * 品牌转换器（DO → VO）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class BrandConvert {

    private BrandConvert() {
    }

    /**
     * 品牌 DO 转 VO
     *
     * @param brandDO 品牌 DO
     * @return 品牌 VO
     */
    public static BrandVO toBrandVO(MallBrandDO brandDO) {
        if (brandDO == null) {
            return null;
        }
        BrandVO vo = new BrandVO();
        vo.setBrandId(String.valueOf(brandDO.getId()));
        vo.setName(brandDO.getName());
        vo.setLogo(brandDO.getLogo());
        vo.setDescription(brandDO.getDescription());
        vo.setSortOrder(brandDO.getSortOrder());
        return vo;
    }

    /**
     * 品牌 DO 列表转 VO 列表
     *
     * @param brandDOList 品牌 DO 列表
     * @return 品牌 VO 列表
     */
    public static List<BrandVO> toBrandVOList(List<MallBrandDO> brandDOList) {
        return brandDOList.stream().map(BrandConvert::toBrandVO).toList();
    }
}
