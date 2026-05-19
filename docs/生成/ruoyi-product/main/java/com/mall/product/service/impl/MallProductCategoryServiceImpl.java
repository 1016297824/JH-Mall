package com.mall.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.product.mapper.MallProductCategoryMapper;
import com.mall.product.domain.MallProductCategory;
import com.mall.product.service.IMallProductCategoryService;

/**
 * 商品类目Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallProductCategoryServiceImpl implements IMallProductCategoryService 
{
    @Autowired
    private MallProductCategoryMapper mallProductCategoryMapper;

    /**
     * 查询商品类目
     * 
     * @param id 商品类目主键
     * @return 商品类目
     */
    @Override
    public MallProductCategory selectMallProductCategoryById(String id)
    {
        return mallProductCategoryMapper.selectMallProductCategoryById(id);
    }

    /**
     * 查询商品类目列表
     * 
     * @param mallProductCategory 商品类目
     * @return 商品类目
     */
    @Override
    public List<MallProductCategory> selectMallProductCategoryList(MallProductCategory mallProductCategory)
    {
        return mallProductCategoryMapper.selectMallProductCategoryList(mallProductCategory);
    }

    /**
     * 新增商品类目
     * 
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    @Override
    public int insertMallProductCategory(MallProductCategory mallProductCategory)
    {
        mallProductCategory.setCreateTime(DateUtils.getNowDate());
        return mallProductCategoryMapper.insertMallProductCategory(mallProductCategory);
    }

    /**
     * 修改商品类目
     * 
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    @Override
    public int updateMallProductCategory(MallProductCategory mallProductCategory)
    {
        mallProductCategory.setUpdateTime(DateUtils.getNowDate());
        return mallProductCategoryMapper.updateMallProductCategory(mallProductCategory);
    }

    /**
     * 批量删除商品类目
     * 
     * @param ids 需要删除的商品类目主键
     * @return 结果
     */
    @Override
    public int deleteMallProductCategoryByIds(String[] ids)
    {
        return mallProductCategoryMapper.deleteMallProductCategoryByIds(ids);
    }

    /**
     * 删除商品类目信息
     * 
     * @param id 商品类目主键
     * @return 结果
     */
    @Override
    public int deleteMallProductCategoryById(String id)
    {
        return mallProductCategoryMapper.deleteMallProductCategoryById(id);
    }
}
