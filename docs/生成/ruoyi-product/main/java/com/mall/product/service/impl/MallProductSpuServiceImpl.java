package com.mall.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.domain.MallProductSpu;
import com.mall.product.service.IMallProductSpuService;

/**
 * SPU 管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallProductSpuServiceImpl implements IMallProductSpuService 
{
    @Autowired
    private MallProductSpuMapper mallProductSpuMapper;

    /**
     * 查询SPU 管理
     * 
     * @param id SPU 管理主键
     * @return SPU 管理
     */
    @Override
    public MallProductSpu selectMallProductSpuById(String id)
    {
        return mallProductSpuMapper.selectMallProductSpuById(id);
    }

    /**
     * 查询SPU 管理列表
     * 
     * @param mallProductSpu SPU 管理
     * @return SPU 管理
     */
    @Override
    public List<MallProductSpu> selectMallProductSpuList(MallProductSpu mallProductSpu)
    {
        return mallProductSpuMapper.selectMallProductSpuList(mallProductSpu);
    }

    /**
     * 新增SPU 管理
     * 
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    @Override
    public int insertMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setCreateTime(DateUtils.getNowDate());
        return mallProductSpuMapper.insertMallProductSpu(mallProductSpu);
    }

    /**
     * 修改SPU 管理
     * 
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    @Override
    public int updateMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setUpdateTime(DateUtils.getNowDate());
        return mallProductSpuMapper.updateMallProductSpu(mallProductSpu);
    }

    /**
     * 批量删除SPU 管理
     * 
     * @param ids 需要删除的SPU 管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSpuByIds(String[] ids)
    {
        return mallProductSpuMapper.deleteMallProductSpuByIds(ids);
    }

    /**
     * 删除SPU 管理信息
     * 
     * @param id SPU 管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSpuById(String id)
    {
        return mallProductSpuMapper.deleteMallProductSpuById(id);
    }
}
