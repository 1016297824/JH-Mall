package com.mall.payment.mapper;

import java.util.List;
import com.mall.payment.domain.MallPayment;
import com.mall.payment.domain.MallPaymentRefund;

/**
 * 支付单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public interface MallPaymentMapper 
{
    /**
     * 查询支付单
     * 
     * @param id 支付单主键
     * @return 支付单
     */
    public MallPayment selectMallPaymentById(String id);

    /**
     * 查询支付单列表
     * 
     * @param mallPayment 支付单
     * @return 支付单集合
     */
    public List<MallPayment> selectMallPaymentList(MallPayment mallPayment);

    /**
     * 新增支付单
     * 
     * @param mallPayment 支付单
     * @return 结果
     */
    public int insertMallPayment(MallPayment mallPayment);

    /**
     * 修改支付单
     * 
     * @param mallPayment 支付单
     * @return 结果
     */
    public int updateMallPayment(MallPayment mallPayment);

    /**
     * 删除支付单
     * 
     * @param id 支付单主键
     * @return 结果
     */
    public int deleteMallPaymentById(String id);

    /**
     * 批量删除支付单
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallPaymentByIds(String[] ids);

    /**
     * 批量删除退款单
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallPaymentRefundByPaymentIds(String[] ids);
    
    /**
     * 批量新增退款单
     * 
     * @param mallPaymentRefundList 退款单列表
     * @return 结果
     */
    public int batchMallPaymentRefund(List<MallPaymentRefund> mallPaymentRefundList);
    

    /**
     * 通过支付单主键删除退款单信息
     * 
     * @param id 支付单ID
     * @return 结果
     */
    public int deleteMallPaymentRefundByPaymentId(String id);
}
