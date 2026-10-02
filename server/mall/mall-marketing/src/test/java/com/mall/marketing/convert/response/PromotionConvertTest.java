package com.mall.marketing.convert.response;

import com.mall.common.enums.marketing.PromotionStatusEnum;
import com.mall.common.enums.marketing.PromotionTypeEnum;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.dto.response.PromotionResp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 促销活动转换器单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class PromotionConvertTest {

    private MallPromotionDO promotion() {
        MallPromotionDO promotion = new MallPromotionDO();
        promotion.setId(7L);
        promotion.setPromotionName("双十一满减");
        promotion.setPromotionType(PromotionTypeEnum.SECKILL.getCode());
        promotion.setStartTime(LocalDateTime.of(2026, 11, 11, 0, 0));
        promotion.setEndTime(LocalDateTime.of(2026, 11, 12, 0, 0));
        promotion.setPromotionStatus(PromotionStatusEnum.ACTIVE.getCode());
        promotion.setDescription("秒杀专场");
        promotion.setBannerImage("https://cdn.example.com/banner/1111.png");
        promotion.setSortOrder(3);
        return promotion;
    }

    @Test
    @DisplayName("全字段映射，并补齐类型/状态描述")
    void shouldMapAllFields() {
        PromotionResp resp = PromotionConvert.toPromotionResp(promotion());

        assertThat(resp.getId()).isEqualTo(7L);
        assertThat(resp.getPromotionName()).isEqualTo("双十一满减");
        assertThat(resp.getPromotionType()).isEqualTo(PromotionTypeEnum.SECKILL.getCode());
        assertThat(resp.getPromotionTypeDesc()).isEqualTo(PromotionTypeEnum.SECKILL.getDescription());
        assertThat(resp.getStartTime()).isEqualTo(LocalDateTime.of(2026, 11, 11, 0, 0));
        assertThat(resp.getEndTime()).isEqualTo(LocalDateTime.of(2026, 11, 12, 0, 0));
        assertThat(resp.getPromotionStatusDesc()).isEqualTo(PromotionStatusEnum.ACTIVE.getDescription());
        assertThat(resp.getDescription()).isEqualTo("秒杀专场");
        assertThat(resp.getBannerImage()).isEqualTo("https://cdn.example.com/banner/1111.png");
        assertThat(resp.getSortOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("入参为 null 时返回 null")
    void shouldReturnNullForNullInput() {
        assertThat(PromotionConvert.toPromotionResp(null)).isNull();
    }

    @Test
    @DisplayName("列表转换保持顺序与元素个数")
    void shouldConvertList() {
        List<PromotionResp> list = PromotionConvert.toPromotionRespList(List.of(promotion(), promotion()));

        assertThat(list).hasSize(2);
        assertThat(list).allSatisfy(resp -> assertThat(resp.getPromotionName()).isEqualTo("双十一满减"));
    }

    @Test
    @DisplayName("无法识别的类型码/状态码返回空串描述，不抛异常")
    void shouldReturnBlankDescForUnknownCode() {
        MallPromotionDO promotion = promotion();
        promotion.setPromotionType(99);
        promotion.setPromotionStatus(99);

        PromotionResp resp = PromotionConvert.toPromotionResp(promotion);

        assertThat(resp.getPromotionTypeDesc()).isEmpty();
        assertThat(resp.getPromotionStatusDesc()).isEmpty();
    }

    @Test
    @DisplayName("类型/状态为空时描述返回空串")
    void shouldReturnBlankDescForNullCode() {
        MallPromotionDO promotion = promotion();
        promotion.setPromotionType(null);
        promotion.setPromotionStatus(null);

        PromotionResp resp = PromotionConvert.toPromotionResp(promotion);

        assertThat(resp.getPromotionTypeDesc()).isEmpty();
        assertThat(resp.getPromotionStatusDesc()).isEmpty();
    }
}
