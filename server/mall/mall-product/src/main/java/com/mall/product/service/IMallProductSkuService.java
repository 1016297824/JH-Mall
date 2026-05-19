package com.mall.product.service;

import java.util.List;
import com.mall.product.domain.MallProductSku;

/**
 * SKU 管理Service接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallProductSkuService 
{
    /**
     * 查询SKU 管理
     * 
     * @param id SKU 管理主键
     * @return SKU 管理
     */
    MallProductSku selectMallProductSkuById(String id);

    /**
     * 查询SKU 管理列表
     * 
     * @param mallProductSku SKU 管理
     * @return SKU 管理集合
     */
    List<MallProductSku> selectMallProductSkuList(MallProductSku mallProductSku);

    /**
     * 新增SKU 管理
     * 
     * @param mallProductSku SKU 管理
     * @return 结果
     */
    int insertMallProductSku(MallProductSku mallProductSku);

    /**
     * 修改SKU 管理
     * 
     * @param mallProductSku SKU 管理
     * @return 结果
     */
    int updateMallProductSku(MallProductSku mallProductSku);

    /**
     * 批量删除SKU 管理
     * 
     * @param ids 需要删除的SKU 管理主键集合
     * @return 结果
     */
    int deleteMallProductSkuByIds(String[] ids);

    /**
     * 删除SKU 管理信息
     * 
     * @param id SKU 管理主键
     * @return 结果
     */
    int deleteMallProductSkuById(String id);
}
