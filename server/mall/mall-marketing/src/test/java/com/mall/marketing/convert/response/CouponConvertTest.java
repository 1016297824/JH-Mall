package com.mall.marketing.convert.response;

import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.dto.response.CouponDefResp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 优惠券定义转换器单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class CouponConvertTest {

    private MallCouponDO coupon() {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setId(10L);
        coupon.setCouponName("满 100 减 10");
        coupon.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        coupon.setFaceValue(1000L);
        coupon.setDiscountRate(null);
        coupon.setDiscountLimit(null);
        coupon.setMinOrderAmount(10000L);
        coupon.setTotalCount(1000);
        coupon.setRemainCount(800);
        coupon.setPerUserLimit(1);
        coupon.setUseStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        coupon.setUseEndTime(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        coupon.setCouponStatus(CouponStatusEnum.PUBLISHED.getCode());
        return coupon;
    }

    @Test
    @DisplayName("全字段映射，并补齐类型/状态描述")
    void shouldMapAllFields() {
        CouponDefResp resp = CouponConvert.toCouponDefResp(coupon());

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getCouponName()).isEqualTo("满 100 减 10");
        assertThat(resp.getCouponTypeDesc()).isEqualTo(CouponTypeEnum.FULL_REDUCE.getDescription());
        assertThat(resp.getFaceValue()).isEqualTo(1000L);
        assertThat(resp.getMinOrderAmount()).isEqualTo(10000L);
        assertThat(resp.getRemainCount()).isEqualTo(800);
        assertThat(resp.getPerUserLimit()).isEqualTo(1);
        assertThat(resp.getUseEndTime()).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        assertThat(resp.getCouponStatusDesc()).isEqualTo(CouponStatusEnum.PUBLISHED.getDescription());
    }

    @Test
    @DisplayName("入参为 null 时返回 null")
    void shouldReturnNullForNullInput() {
        assertThat(CouponConvert.toCouponDefResp(null)).isNull();
    }

    @Test
    @DisplayName("列表转换保持元素个数")
    void shouldConvertList() {
        List<CouponDefResp> list = CouponConvert.toCouponDefRespList(List.of(coupon(), coupon(), coupon()));

        assertThat(list).hasSize(3);
    }

    @Test
    @DisplayName("无法识别的类型码/状态码返回空串描述，不抛异常")
    void shouldReturnBlankDescForUnknownCode() {
        MallCouponDO coupon = coupon();
        coupon.setCouponType(99);
        coupon.setCouponStatus(99);

        CouponDefResp resp = CouponConvert.toCouponDefResp(coupon);

        assertThat(resp.getCouponTypeDesc()).isEmpty();
        assertThat(resp.getCouponStatusDesc()).isEmpty();
    }

    @Test
    @DisplayName("类型/状态为空时描述返回空串")
    void shouldReturnBlankDescForNullCode() {
        MallCouponDO coupon = coupon();
        coupon.setCouponType(null);
        coupon.setCouponStatus(null);

        CouponDefResp resp = CouponConvert.toCouponDefResp(coupon);

        assertThat(resp.getCouponTypeDesc()).isEmpty();
        assertThat(resp.getCouponStatusDesc()).isEmpty();
    }
}
