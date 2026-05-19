package com.mall.product.mapper;

import java.util.List;
import com.mall.product.domain.MallProductSpu;

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
     * 删除SPU 管理
     * 
     * @param id SPU 管理主键
     * @return 结果
     */
    int deleteMallProductSpuById(String id);

    /**
     * 批量删除SPU 管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteMallProductSpuByIds(String[] ids);
}
