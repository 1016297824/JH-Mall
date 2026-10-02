package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.DO.MallPaymentChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 支付渠道配置 Mapper
 *
 * <p>渠道配置为低频读、极低频写，读取侧由 Service 加 Caffeine 本地缓存
 * （TTL 取 {@code mall.payment.channel.cache-ttl}），故此处只提供必要的查询方法。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Mapper
public interface MallPaymentChannelMapper extends BaseMapper<MallPaymentChannelDO> {

    /** 是否启用：启用 */
    int ENABLED = 1;

    /**
     * 查询全部已启用渠道
     *
     * <p>用于前端展示可用支付方式，按 {@code sort_order} 升序。</p>
     *
     * @return 已启用渠道列表
     */
    default List<MallPaymentChannelDO> selectEnabled() {
        return selectList(new LambdaQueryWrapper<MallPaymentChannelDO>()
                .eq(MallPaymentChannelDO::getIsEnabled, ENABLED)
                .eq(MallPaymentChannelDO::getIsDeleted, 0)
                .orderByAsc(MallPaymentChannelDO::getSortOrder)
                .orderByAsc(MallPaymentChannelDO::getId));
    }

    /**
     * 按渠道编码查询未删除的渠道配置
     *
     * @param channelCode 渠道编码，如 wechat / alipay
     * @return 渠道配置，不存在返回 null
     */
    default MallPaymentChannelDO selectByChannelCode(String channelCode) {
        return selectOne(new LambdaQueryWrapper<MallPaymentChannelDO>()
                .eq(MallPaymentChannelDO::getChannelCode, channelCode)
                .eq(MallPaymentChannelDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }
}
