package com.mall.marketing.service.impl;

import com.mall.common.enums.marketing.PromotionStatusEnum;
import com.mall.common.enums.marketing.PromotionTypeEnum;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.dto.response.PromotionResp;
import com.mall.marketing.mapper.MallPromotionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 促销活动服务单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock private MallPromotionMapper promotionMapper;

    @InjectMocks private PromotionServiceImpl promotionService;

    /** 构造一个进行中的活动 */
    private MallPromotionDO activePromotion(Long id, String name, Integer sortOrder) {
        MallPromotionDO promotion = new MallPromotionDO();
        promotion.setId(id);
        promotion.setPromotionName(name);
        promotion.setPromotionType(PromotionTypeEnum.FULL_REDUCE.getCode());
        promotion.setStartTime(LocalDateTime.now().minusDays(1));
        promotion.setEndTime(LocalDateTime.now().plusDays(7));
        promotion.setPromotionStatus(PromotionStatusEnum.ACTIVE.getCode());
        promotion.setDescription("满 200 减 30");
        promotion.setBannerImage("https://cdn.example.com/banner/1.png");
        promotion.setSortOrder(sortOrder);
        promotion.setIsDeleted(0);
        return promotion;
    }

    @Test
    @DisplayName("委托 Mapper 查询并补齐类型/状态描述")
    void listShouldDelegateAndEnrichDesc() {
        when(promotionMapper.selectActive(any(LocalDateTime.class), eq(20)))
                .thenReturn(List.of(activePromotion(1L, "满 200 减 30", 1)));

        List<PromotionResp> result = promotionService.listActivePromotions(20);

        assertThat(result).hasSize(1);
        PromotionResp resp = result.get(0);
        assertThat(resp.getPromotionName()).isEqualTo("满 200 减 30");
        assertThat(resp.getPromotionTypeDesc()).isEqualTo(PromotionTypeEnum.FULL_REDUCE.getDescription());
        assertThat(resp.getPromotionStatusDesc()).isEqualTo(PromotionStatusEnum.ACTIVE.getDescription());
        assertThat(resp.getBannerImage()).isEqualTo("https://cdn.example.com/banner/1.png");
    }

    @Test
    @DisplayName("保持 Mapper 返回的排序（活动顺序由 SQL 的 sort_order 决定，服务层不重排）")
    void listShouldKeepMapperOrder() {
        when(promotionMapper.selectActive(any(LocalDateTime.class), eq(20)))
                .thenReturn(List.of(
                        activePromotion(2L, "第二个", 2),
                        activePromotion(1L, "第一个", 1)));

        List<PromotionResp> result = promotionService.listActivePromotions(20);

        assertThat(result).extracting(PromotionResp::getPromotionName)
                .containsExactly("第二个", "第一个");
    }

    @Test
    @DisplayName("limit 透传给 Mapper，不被服务层写死")
    void listShouldPassThroughLimit() {
        when(promotionMapper.selectActive(any(LocalDateTime.class), eq(5))).thenReturn(List.of());

        assertThat(promotionService.listActivePromotions(5)).isEmpty();

        verify(promotionMapper).selectActive(any(LocalDateTime.class), eq(5));
    }
}
