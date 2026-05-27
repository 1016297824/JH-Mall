package com.mall.admin.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.admin.product.domain.MallProductSku;
import com.mall.admin.product.mapper.MallProductSpuMapper;
import com.mall.admin.product.domain.MallProductSpu;
import com.mall.admin.product.service.IMallProductSpuService;

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
    @Transactional
    @Override
    public int insertMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setCreateTime(DateUtils.getNowDate());
        int rows = mallProductSpuMapper.insertMallProductSpu(mallProductSpu);
        insertMallProductSku(mallProductSpu);
        return rows;
    }

    /**
     * 修改SPU 管理
     *
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setUpdateTime(DateUtils.getNowDate());
        mallProductSpuMapper.deleteMallProductSkuBySpuId(mallProductSpu.getId());
        insertMallProductSku(mallProductSpu);
        return mallProductSpuMapper.updateMallProductSpu(mallProductSpu);
    }

    /**
     * 批量删除SPU 管理
     *
     * @param ids 需要删除的SPU 管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallProductSpuByIds(String[] ids)
    {
        mallProductSpuMapper.deleteMallProductSkuBySpuIds(ids);
        return mallProductSpuMapper.deleteMallProductSpuByIds(ids);
    }

    /**
     * 删除SPU 管理信息
     *
     * @param id SPU 管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallProductSpuById(String id)
    {
        mallProductSpuMapper.deleteMallProductSkuBySpuId(id);
        return mallProductSpuMapper.deleteMallProductSpuById(id);
    }

    /**
     * 新增SKU 管理信息
     *
     * @param mallProductSpu SPU 管理对象
     */
    public void insertMallProductSku(MallProductSpu mallProductSpu)
    {
        List<MallProductSku> mallProductSkuList = mallProductSpu.getMallProductSkuList();
        String id = mallProductSpu.getId();
        if (StringUtils.isNotNull(mallProductSkuList))
        {
            List<MallProductSku> list = new ArrayList<MallProductSku>();
            for (MallProductSku mallProductSku : mallProductSkuList)
            {
                mallProductSku.setSpuId(id);
                list.add(mallProductSku);
            }
            if (list.size() > 0)
            {
                mallProductSpuMapper.batchMallProductSku(list);
            }
        }
    }
}
