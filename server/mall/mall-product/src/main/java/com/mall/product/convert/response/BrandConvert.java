package com.mall.product.convert.response;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;

import java.util.List;

public class BrandConvert {

    private BrandConvert() {
    }

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

    public static List<BrandVO> toBrandVOList(List<MallBrandDO> brandDOList) {
        return brandDOList.stream().map(BrandConvert::toBrandVO).toList();
    }
}
