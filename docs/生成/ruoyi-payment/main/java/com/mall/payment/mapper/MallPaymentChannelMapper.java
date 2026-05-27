package com.mall.payment.mapper;

import java.util.List;
import com.mall.payment.DO.MallPaymentChannel;

/**
 * 支付渠道Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface MallPaymentChannelMapper
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
     * 删除支付渠道
     *
     * @param id 支付渠道主键
     * @return 结果
     */
    public int deleteMallPaymentChannelById(String id);

    /**
     * 批量删除支付渠道
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallPaymentChannelByIds(String[] ids);
}
