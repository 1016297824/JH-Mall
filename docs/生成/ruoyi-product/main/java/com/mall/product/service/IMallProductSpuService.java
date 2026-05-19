package com.mall.product.service;

import java.util.List;
import com.mall.product.domain.MallProductSpu;

/**
 * SPU 管理Service接口
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallProductSpuService 
{
    /**
     * 查询SPU 管理
     * 
     * @param id SPU 管理主键
     * @return SPU 管理
     */
    MallProductSpu selectMallProductSpuById(String id);

    /**
     * 查询SPU 管理列表
     * 
     * @param mallProductSpu SPU 管理
     * @return SPU 管理集合
     */
    List<MallProductSpu> selectMallProductSpuList(MallProductSpu mallProductSpu);

    /**
     * 新增SPU 管理
     * 
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    int insertMallProductSpu(MallProductSpu mallProductSpu);

    /**
     * 修改SPU 管理
     * 
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    int updateMallProductSpu(MallProductSpu mallProductSpu);

    /**
     * 批量删除SPU 管理
     * 
     * @param ids 需要删除的SPU 管理主键集合
     * @return 结果
     */
    int deleteMallProductSpuByIds(String[] ids);

    /**
     * 删除SPU 管理信息
     * 
     * @param id SPU 管理主键
     * @return 结果
     */
    int deleteMallProductSpuById(String id);
}
