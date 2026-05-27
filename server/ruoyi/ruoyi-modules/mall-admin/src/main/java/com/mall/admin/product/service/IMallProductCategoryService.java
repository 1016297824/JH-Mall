package com.mall.admin.product.service;

import java.util.List;
import com.mall.admin.product.domain.MallProductCategory;

/**
 * 商品类目Service接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallProductCategoryService
{
    /**
     * 查询商品类目
     *
     * @param id 商品类目主键
     * @return 商品类目
     */
    public MallProductCategory selectMallProductCategoryById(String id);

    /**
     * 查询商品类目列表
     *
     * @param mallProductCategory 商品类目
     * @return 商品类目集合
     */
    public List<MallProductCategory> selectMallProductCategoryList(MallProductCategory mallProductCategory);

    /**
     * 新增商品类目
     *
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    public int insertMallProductCategory(MallProductCategory mallProductCategory);

    /**
     * 修改商品类目
     *
     * @param mallProductCategory 商品类目
     * @return 结果
     */
    public int updateMallProductCategory(MallProductCategory mallProductCategory);

    /**
     * 批量删除商品类目
     *
     * @param ids 需要删除的商品类目主键集合
     * @return 结果
     */
    public int deleteMallProductCategoryByIds(String[] ids);

    /**
     * 删除商品类目信息
     *
     * @param id 商品类目主键
     * @return 结果
     */
    public int deleteMallProductCategoryById(String id);
}
