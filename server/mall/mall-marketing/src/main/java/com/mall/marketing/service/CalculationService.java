package com.mall.marketing.service;

import com.mall.api.feign.RemoteMarketingService.CalculationReq;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;

/**
 * 优惠试算服务
 *
 * <p>下单前计算最优优惠<b>组合</b>，<b>不锁定</b>任何资源。设计依据：
 * {@code docs/design/14_mall-marketing详细设计.md} §3.4。</p>
 *
 * <p><b>性能约束</b>：不调远程服务、不做 DB 写操作。</p>
 *
 * <p><b>业务口径</b>（2026-10-02 确认）：一笔订单<b>最多使用 1 张优惠券</b>（取优惠最大者），
 * 优惠券与促销活动<b>可叠加</b>，最终应付为
 * {@code max(0, 原价 − 券优惠 − 促销优惠)}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface CalculationService {

    /**
     * 优惠试算
     *
     * @param req 试算请求（用户 + 商品明细 + 可选指定券）
     * @return 试算结果（原价 / 券优惠 / 促销优惠 / 应付 / 命中明细）
     */
    CalculationResp calculate(CalculationReq req);
}
