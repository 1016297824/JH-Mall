package com.mall.product.service.impl;

import com.mall.product.DO.MallBrandDO;
import com.mall.product.VO.BrandVO;
import com.mall.product.convert.response.BrandConvert;
import com.mall.product.mapper.MallBrandMapper;
import com.mall.product.service.IBrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 品牌服务实现
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements IBrandService {

    private final MallBrandMapper mallBrandMapper;

    @Override
    public List<BrandVO> list(Long categoryId) {
        // 查询全部未删除品牌（按排序值升序）
        List<MallBrandDO> brandDOList = mallBrandMapper.selectAll();
        // DO 转 VO
        return BrandConvert.toBrandVOList(brandDOList);
    }
}
