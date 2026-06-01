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
        // 查询全部未删除品牌（按排序值升序），暂未按 categoryId 过滤
        List<MallBrandDO> brandDOList = mallBrandMapper.selectAll();
        return BrandConvert.toBrandVOList(brandDOList);
    }
}
