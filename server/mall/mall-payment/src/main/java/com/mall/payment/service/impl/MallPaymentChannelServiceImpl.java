package com.mall.payment.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.domain.MallPaymentChannel;
import com.mall.payment.service.IMallPaymentChannelService;

/**
 * 支付渠道Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-21
 */
@Service
public class MallPaymentChannelServiceImpl implements IMallPaymentChannelService 
{
    @Autowired
    private MallPaymentChannelMapper mallPaymentChannelMapper;

    /**
     * 查询支付渠道
     * 
     * @param id 支付渠道主键
     * @return 支付渠道
     */
    @Override
    public MallPaymentChannel selectMallPaymentChannelById(String id)
    {
        return mallPaymentChannelMapper.selectMallPaymentChannelById(id);
    }

    /**
     * 查询支付渠道列表
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 支付渠道
     */
    @Override
    public List<MallPaymentChannel> selectMallPaymentChannelList(MallPaymentChannel mallPaymentChannel)
    {
        return mallPaymentChannelMapper.selectMallPaymentChannelList(mallPaymentChannel);
    }

    /**
     * 新增支付渠道
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 结果
     */
    @Override
    public int insertMallPaymentChannel(MallPaymentChannel mallPaymentChannel)
    {
        mallPaymentChannel.setCreateTime(DateUtils.getNowDate());
        return mallPaymentChannelMapper.insertMallPaymentChannel(mallPaymentChannel);
    }

    /**
     * 修改支付渠道
     * 
     * @param mallPaymentChannel 支付渠道
     * @return 结果
     */
    @Override
    public int updateMallPaymentChannel(MallPaymentChannel mallPaymentChannel)
    {
        mallPaymentChannel.setUpdateTime(DateUtils.getNowDate());
        return mallPaymentChannelMapper.updateMallPaymentChannel(mallPaymentChannel);
    }

    /**
     * 批量删除支付渠道
     * 
     * @param ids 需要删除的支付渠道主键
     * @return 结果
     */
    @Override
    public int deleteMallPaymentChannelByIds(String[] ids)
    {
        return mallPaymentChannelMapper.deleteMallPaymentChannelByIds(ids);
    }

    /**
     * 删除支付渠道信息
     * 
     * @param id 支付渠道主键
     * @return 结果
     */
    @Override
    public int deleteMallPaymentChannelById(String id)
    {
        return mallPaymentChannelMapper.deleteMallPaymentChannelById(id);
    }
}
