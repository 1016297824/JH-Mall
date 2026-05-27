package com.mall.payment.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.payment.mapper.MallPaymentCallbackLogMapper;
import com.mall.payment.DO.MallPaymentCallbackLog;
import com.mall.payment.service.IMallPaymentCallbackLogService;

/**
 * 回调日志Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@Service
public class MallPaymentCallbackLogServiceImpl implements IMallPaymentCallbackLogService
{
    @Autowired
    private MallPaymentCallbackLogMapper mallPaymentCallbackLogMapper;

    /**
     * 查询回调日志
     *
     * @param id 回调日志主键
     * @return 回调日志
     */
    @Override
    public MallPaymentCallbackLog selectMallPaymentCallbackLogById(String id)
    {
        return mallPaymentCallbackLogMapper.selectMallPaymentCallbackLogById(id);
    }

    /**
     * 查询回调日志列表
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 回调日志
     */
    @Override
    public List<MallPaymentCallbackLog> selectMallPaymentCallbackLogList(MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        return mallPaymentCallbackLogMapper.selectMallPaymentCallbackLogList(mallPaymentCallbackLog);
    }

    /**
     * 新增回调日志
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 结果
     */
    @Override
    public int insertMallPaymentCallbackLog(MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        mallPaymentCallbackLog.setCreateTime(DateUtils.getNowDate());
        return mallPaymentCallbackLogMapper.insertMallPaymentCallbackLog(mallPaymentCallbackLog);
    }

    /**
     * 修改回调日志
     *
     * @param mallPaymentCallbackLog 回调日志
     * @return 结果
     */
    @Override
    public int updateMallPaymentCallbackLog(MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        mallPaymentCallbackLog.setUpdateTime(DateUtils.getNowDate());
        return mallPaymentCallbackLogMapper.updateMallPaymentCallbackLog(mallPaymentCallbackLog);
    }

    /**
     * 批量删除回调日志
     *
     * @param ids 需要删除的回调日志主键
     * @return 结果
     */
    @Override
    public int deleteMallPaymentCallbackLogByIds(String[] ids)
    {
        return mallPaymentCallbackLogMapper.deleteMallPaymentCallbackLogByIds(ids);
    }

    /**
     * 删除回调日志信息
     *
     * @param id 回调日志主键
     * @return 结果
     */
    @Override
    public int deleteMallPaymentCallbackLogById(String id)
    {
        return mallPaymentCallbackLogMapper.deleteMallPaymentCallbackLogById(id);
    }
}
