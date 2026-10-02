package com.mall.marketing.service.impl;

import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.convert.response.PromotionConvert;
import com.mall.marketing.dto.response.PromotionResp;
import com.mall.marketing.mapper.MallPromotionMapper;
import com.mall.marketing.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动服务实现
 *
 * <p>纯查询，无写操作。活动顺序由 SQL 的 {@code sort_order} 决定，本层不重排。
 * 「进行中」的时间窗判定同样落在 SQL（{@code start_time <= now < end_time}），
 * 避免应用层与数据库时钟不一致。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final MallPromotionMapper promotionMapper;

    @Override
    public List<PromotionResp> listActivePromotions(int limit) {
        List<MallPromotionDO> promotions = promotionMapper.selectActive(LocalDateTime.now(), limit);
        return PromotionConvert.toPromotionRespList(promotions);
    }
}
