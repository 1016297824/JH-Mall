package com.mall.product.mapper;

import java.util.List;
import com.mall.product.domain.MallProductCategory;

/**
 * 商品类目Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallProductCategoryMapper 
{
    /**
     * 查询商品类目
     * 
     * @param id 商品类目主键
     * @return 商品类目
     */
    MallProductCategory selectMallProductCategoryById(String id);

    /**
     * 查询商品类目列表
     * 
     * @param mallProductCategory 商品类目
     * @return 商品类目集合
     */
    List<MallProductCategory> selectMallProductCategoryList(MallProductCategory mallProductCategory);

    /**
     * 新增商品类目
     * 
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    int insertMallProductCategory(MallProductCategory mallProductCategory);

    /**
     * 修改商品类目
     * 
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    int updateMallProductCategory(MallProductCategory mallProductCategory);

    /**
     * 删除商品类目
     * 
     * @param id 商品类目主键
     * @return 结果
     */
    int deleteMallProductCategoryById(String id);

    /**
     * 批量删除商品类目
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallProductCategoryByIds(String[] ids);
}
