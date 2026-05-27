package com.mall.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.product.mapper.MallProductBrandMapper;
import com.mall.product.domain.MallProductBrand;
import com.mall.product.service.IMallProductBrandService;

/**
 * 品牌管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallProductBrandServiceImpl implements IMallProductBrandService 
{
    @Autowired
    private MallProductBrandMapper mallProductBrandMapper;

    /**
     * 查询品牌管理
     * 
     * @param id 品牌管理主键
     * @return 品牌管理
     */
    @Override
    public MallProductBrand selectMallProductBrandById(String id)
    {
        return mallProductBrandMapper.selectMallProductBrandById(id);
    }

    /**
     * 查询品牌管理列表
     * 
     * @param mallProductBrand 品牌管理
     * @return 品牌管理
     */
    @Override
    public List<MallProductBrand> selectMallProductBrandList(MallProductBrand mallProductBrand)
    {
        return mallProductBrandMapper.selectMallProductBrandList(mallProductBrand);
    }

    /**
     * 新增品牌管理
     * 
     * @param mallProductBrand 品牌管理
     * @return 结果
     */
    @Override
    public int insertMallProductBrand(MallProductBrand mallProductBrand)
    {
        mallProductBrand.setCreateTime(DateUtils.getNowDate());
        return mallProductBrandMapper.insertMallProductBrand(mallProductBrand);
    }

    /**
     * 修改品牌管理
     * 
     * @param mallProductBrand 品牌管理
     * @return 结果
     */
    @Override
    public int updateMallProductBrand(MallProductBrand mallProductBrand)
    {
        mallProductBrand.setUpdateTime(DateUtils.getNowDate());
        return mallProductBrandMapper.updateMallProductBrand(mallProductBrand);
    }

    /**
     * 批量删除品牌管理
     * 
     * @param ids 需要删除的品牌管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductBrandByIds(String[] ids)
    {
        return mallProductBrandMapper.deleteMallProductBrandByIds(ids);
    }

    /**
     * 删除品牌管理信息
     * 
     * @param id 品牌管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductBrandById(String id)
    {
        return mallProductBrandMapper.deleteMallProductBrandById(id);
    }
}
