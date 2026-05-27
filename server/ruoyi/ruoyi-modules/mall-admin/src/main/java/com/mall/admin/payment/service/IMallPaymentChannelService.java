package com.mall.payment.service;

import java.util.List;
import com.mall.payment.domain.MallPaymentChannel;

/**
 * 支付渠道Service接口
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
public interface IMallPaymentChannelService 
{
    /**
     * 查询支付渠道
     * 
     * @param id 支付渠道主键
     * @return 支付渠道
     */
    public MallPaymentChannel selectMallPaymentChannelById(String id);

    /**
     * 查询支付渠道列表
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 支付渠道集合
     */
    public List<MallPaymentChannel> selectMallPaymentChannelList(MallPaymentChannel mallPaymentChannel);

    /**
     * 新增支付渠道
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 结果
     */
    public int insertMallPaymentChannel(MallPaymentChannel mallPaymentChannel);

    /**
     * 修改支付渠道
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 结果
     */
    public int updateMallPaymentChannel(MallPaymentChannel mallPaymentChannel);

    /**
     * 批量删除支付渠道
     * 
     * @param ids 需要删除的支付渠道主键集合
     * @return 结果
     */
    public int deleteMallPaymentChannelByIds(String[] ids);

    /**
     * 删除支付渠道信息
     * 
     * @param id 支付渠道主键
     * @return 结果
     */
    public int deleteMallPaymentChannelById(String id);
}
