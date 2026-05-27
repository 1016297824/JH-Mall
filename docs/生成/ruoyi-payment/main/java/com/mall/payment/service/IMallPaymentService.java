package com.mall.payment.service;

import java.util.List;
import com.mall.payment.DO.MallPayment;

/**
 * 支付单Service接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface IMallPaymentService
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
     * 批量删除支付单
     *
     * @param ids 需要删除的支付单主键集合
     * @return 结果
     */
    public int deleteMallPaymentByIds(String[] ids);

    /**
     * 删除支付单信息
     *
     * @param id 支付单主键
     * @return 结果
     */
    public int deleteMallPaymentById(String id);
}
