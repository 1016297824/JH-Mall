package com.mall.product.mapper;

import java.util.List;
import com.mall.product.DO.MallProductSpu;
import com.mall.product.DO.MallProductSku;

/**
 * SPU 管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface MallProductSpuMapper
{
    /**
     * 查询SPU 管理
     *
     * @param id SPU 管理主键
     * @return SPU 管理
     */
    public MallProductSpu selectMallProductSpuById(String id);

    /**
     * 查询SPU 管理列表
     *
     * @param mallProductSpu SPU 管理
     * @return SPU 管理集合
     */
    public List<MallProductSpu> selectMallProductSpuList(MallProductSpu mallProductSpu);

    /**
     * 新增SPU 管理
     *
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    public int insertMallProductSpu(MallProductSpu mallProductSpu);

    /**
     * 修改SPU 管理
     *
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    public int updateMallProductSpu(MallProductSpu mallProductSpu);

    /**
     * 删除SPU 管理
     *
     * @param id SPU 管理主键
     * @return 结果
     */
    public int deleteMallProductSpuById(String id);

    /**
     * 批量删除SPU 管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallProductSpuByIds(String[] ids);

    /**
     * 批量删除SKU 管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallProductSkuBySpuIds(String[] ids);

    /**
     * 批量新增SKU 管理
     *
     * @param mallProductSkuList SKU 管理列表
     * @return 结果
     */
    public int batchMallProductSku(List<MallProductSku> mallProductSkuList);


    /**
     * 通过SPU 管理主键删除SKU 管理信息
     *
     * @param id SPU 管理ID
     * @return 结果
     */
    public int deleteMallProductSkuBySpuId(String id);
}
