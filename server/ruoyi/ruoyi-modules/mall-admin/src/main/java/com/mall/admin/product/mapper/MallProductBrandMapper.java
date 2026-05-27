package com.mall.admin.product.mapper;

import java.util.List;
import com.mall.admin.product.domain.MallProductBrand;

/**
 * 品牌管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallProductBrandMapper
{
    /**
     * 查询品牌管理
     *
     * @param id 品牌管理主键
     * @return 品牌管理
     */
    public MallProductBrand selectMallProductBrandById(String id);

    /**
     * 查询品牌管理列表
     *
     * @param mallProductBrand 品牌管理
     * @return 品牌管理集合
     */
    public List<MallProductBrand> selectMallProductBrandList(MallProductBrand mallProductBrand);

    /**
     * 新增品牌管理
     *
     * @param mallProductBrand 品牌管理
     * @return 结果
     */
    public int insertMallProductBrand(MallProductBrand mallProductBrand);

    /**
     * 修改品牌管理
     *
     * @param mallProductBrand 品牌管理
     * @return 结果
     */
    public int updateMallProductBrand(MallProductBrand mallProductBrand);

    /**
     * 删除品牌管理
     *
     * @param id 品牌管理主键
     * @return 结果
     */
    public int deleteMallProductBrandById(String id);

    /**
     * 批量删除品牌管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallProductBrandByIds(String[] ids);
}
