package com.mall.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.domain.MallProductSku;
import com.mall.product.service.IMallProductSkuService;

/**
 * SKU 管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallProductSkuServiceImpl implements IMallProductSkuService 
{
    @Autowired
    private MallProductSkuMapper mallProductSkuMapper;

    /**
     * 查询SKU 管理
     * 
     * @param id SKU 管理主键
     * @return SKU 管理
     */
    @Override
    public MallProductSku selectMallProductSkuById(String id)
    {
        return mallProductSkuMapper.selectMallProductSkuById(id);
    }

    /**
     * 查询SKU 管理列表
     * 
     * @param mallProductSku SKU 管理
     * @return SKU 管理
     */
    @Override
    public List<MallProductSku> selectMallProductSkuList(MallProductSku mallProductSku)
    {
        return mallProductSkuMapper.selectMallProductSkuList(mallProductSku);
    }

    /**
     * 新增SKU 管理
     * 
     * @param mallProductSku SKU 管理
     * @return 结果
     */
    @Override
    public int insertMallProductSku(MallProductSku mallProductSku)
    {
        mallProductSku.setCreateTime(DateUtils.getNowDate());
        return mallProductSkuMapper.insertMallProductSku(mallProductSku);
    }

    /**
     * 修改SKU 管理
     * 
     * @param mallProductSku SKU 管理
     * @return 结果
     */
    @Override
    public int updateMallProductSku(MallProductSku mallProductSku)
    {
        mallProductSku.setUpdateTime(DateUtils.getNowDate());
        return mallProductSkuMapper.updateMallProductSku(mallProductSku);
    }

    /**
     * 批量删除SKU 管理
     * 
     * @param ids 需要删除的SKU 管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSkuByIds(String[] ids)
    {
        return mallProductSkuMapper.deleteMallProductSkuByIds(ids);
    }

    /**
     * 删除SKU 管理信息
     * 
     * @param id SKU 管理主键
     * @return 结果
     */
    @Override
    public int deleteMallProductSkuById(String id)
    {
        return mallProductSkuMapper.deleteMallProductSkuById(id);
    }
}
