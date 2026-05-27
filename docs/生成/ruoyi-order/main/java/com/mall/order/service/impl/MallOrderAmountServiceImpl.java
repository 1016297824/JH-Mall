package com.mall.order.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.order.mapper.MallOrderAmountMapper;
import com.mall.order.DO.MallOrderAmount;
import com.mall.order.service.IMallOrderAmountService;

/**
 * 金额快照Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@Service
public class MallOrderAmountServiceImpl implements IMallOrderAmountService
{
    @Autowired
    private MallOrderAmountMapper mallOrderAmountMapper;

    /**
     * 查询金额快照
     *
     * @param id 金额快照主键
     * @return 金额快照
     */
    @Override
    public MallOrderAmount selectMallOrderAmountById(String id)
    {
        return mallOrderAmountMapper.selectMallOrderAmountById(id);
    }

    /**
     * 查询金额快照列表
     *
     * @param mallOrderAmount 金额快照
     * @return 金额快照
     */
    @Override
    public List<MallOrderAmount> selectMallOrderAmountList(MallOrderAmount mallOrderAmount)
    {
        return mallOrderAmountMapper.selectMallOrderAmountList(mallOrderAmount);
    }

    /**
     * 新增金额快照
     *
     * @param mallOrderAmount 金额快照
     * @return 结果
     */
    @Override
    public int insertMallOrderAmount(MallOrderAmount mallOrderAmount)
    {
        mallOrderAmount.setCreateTime(DateUtils.getNowDate());
        return mallOrderAmountMapper.insertMallOrderAmount(mallOrderAmount);
    }

    /**
     * 修改金额快照
     *
     * @param mallOrderAmount 金额快照
     * @return 结果
     */
    @Override
    public int updateMallOrderAmount(MallOrderAmount mallOrderAmount)
    {
        mallOrderAmount.setUpdateTime(DateUtils.getNowDate());
        return mallOrderAmountMapper.updateMallOrderAmount(mallOrderAmount);
    }

    /**
     * 批量删除金额快照
     *
     * @param ids 需要删除的金额快照主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderAmountByIds(String[] ids)
    {
        return mallOrderAmountMapper.deleteMallOrderAmountByIds(ids);
    }

    /**
     * 删除金额快照信息
     *
     * @param id 金额快照主键
     * @return 结果
     */
    @Override
    public int deleteMallOrderAmountById(String id)
    {
        return mallOrderAmountMapper.deleteMallOrderAmountById(id);
    }
}
