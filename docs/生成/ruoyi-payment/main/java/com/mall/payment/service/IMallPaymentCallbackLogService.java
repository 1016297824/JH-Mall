package com.mall.payment.service;

import java.util.List;
import com.mall.payment.DO.MallPaymentCallbackLog;

/**
 * 回调日志Service接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface IMallPaymentCallbackLogService
{
    /**
     * 查询回调日志
     *
     * @param id 回调日志主键
     * @return 回调日志
     */
    public MallPaymentCallbackLog selectMallPaymentCallbackLogById(String id);

    /**
     * 查询回调日志列表
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 回调日志集合
     */
    public List<MallPaymentCallbackLog> selectMallPaymentCallbackLogList(MallPaymentCallbackLog mallPaymentCallbackLog);

    /**
     * 新增回调日志
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 结果
     */
    public int insertMallPaymentCallbackLog(MallPaymentCallbackLog mallPaymentCallbackLog);

    /**
     * 修改回调日志
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 结果
     */
    public int updateMallPaymentCallbackLog(MallPaymentCallbackLog mallPaymentCallbackLog);

    /**
     * 批量删除回调日志
     *
     * @param ids 需要删除的回调日志主键集合
     * @return 结果
     */
    public int deleteMallPaymentCallbackLogByIds(String[] ids);

    /**
     * 删除回调日志信息
     *
     * @param id 回调日志主键
     * @return 结果
     */
    public int deleteMallPaymentCallbackLogById(String id);
}
