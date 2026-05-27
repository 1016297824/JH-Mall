package com.mall.payment.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.payment.DO.MallPaymentRefund;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.DO.MallPayment;
import com.mall.payment.service.IMallPaymentService;

/**
 * 支付单Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@Service
public class MallPaymentServiceImpl implements IMallPaymentService
{
    @Autowired
    private MallPaymentMapper mallPaymentMapper;

    /**
     * 查询支付单
     *
     * @param id 支付单主键
     * @return 支付单
     */
    @Override
    public MallPayment selectMallPaymentById(String id)
    {
        return mallPaymentMapper.selectMallPaymentById(id);
    }

    /**
     * 查询支付单列表
     *
     * @param mallPayment 支付单
     * @return 支付单
     */
    @Override
    public List<MallPayment> selectMallPaymentList(MallPayment mallPayment)
    {
        return mallPaymentMapper.selectMallPaymentList(mallPayment);
    }

    /**
     * 新增支付单
     *
     * @param mallPayment 支付单
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMallPayment(MallPayment mallPayment)
    {
        mallPayment.setCreateTime(DateUtils.getNowDate());
        int rows = mallPaymentMapper.insertMallPayment(mallPayment);
        insertMallPaymentRefund(mallPayment);
        return rows;
    }

    /**
     * 修改支付单
     *
     * @param mallPayment 支付单
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallPayment(MallPayment mallPayment)
    {
        mallPayment.setUpdateTime(DateUtils.getNowDate());
        mallPaymentMapper.deleteMallPaymentRefundByPaymentId(mallPayment.getId());
        insertMallPaymentRefund(mallPayment);
        return mallPaymentMapper.updateMallPayment(mallPayment);
    }

    /**
     * 批量删除支付单
     *
     * @param ids 需要删除的支付单主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallPaymentByIds(String[] ids)
    {
        mallPaymentMapper.deleteMallPaymentRefundByPaymentIds(ids);
        return mallPaymentMapper.deleteMallPaymentByIds(ids);
    }

    /**
     * 删除支付单信息
     *
     * @param id 支付单主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallPaymentById(String id)
    {
        mallPaymentMapper.deleteMallPaymentRefundByPaymentId(id);
        return mallPaymentMapper.deleteMallPaymentById(id);
    }

    /**
     * 新增退款单信息
     *
     * @param mallPayment 支付单对象
     */
    public void insertMallPaymentRefund(MallPayment mallPayment)
    {
        List<MallPaymentRefund> mallPaymentRefundList = mallPayment.getMallPaymentRefundList();
        String id = mallPayment.getId();
        if (StringUtils.isNotNull(mallPaymentRefundList))
        {
            List<MallPaymentRefund> list = new ArrayList<MallPaymentRefund>();
            for (MallPaymentRefund mallPaymentRefund : mallPaymentRefundList)
            {
                mallPaymentRefund.setPaymentId(id);
                list.add(mallPaymentRefund);
            }
            if (list.size() > 0)
            {
                mallPaymentMapper.batchMallPaymentRefund(list);
            }
        }
    }
}
