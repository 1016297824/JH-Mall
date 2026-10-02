package com.mall.marketing.convert.request;

import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.dto.request.CreateCouponReq;
import com.mall.marketing.dto.request.UpdateCouponReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 优惠券定义请求转换器单元测试
 *
 * <p>重点是「创建时建哪些默认值」与「修改时<b>不该动</b>哪些字段」两组不变式——
 * 后者是库存一致性的一部分。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
class CouponRequestConvertTest {

    private CreateCouponReq createReq() {
        CreateCouponReq req = new CreateCouponReq();
        req.setCouponName("满 100 减 10");
        req.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        req.setFaceValue(1000L);
        req.setMinOrderAmount(10000L);
        req.setTotalCount(1000);
        req.setPerUserLimit(1);
        req.setUseStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        req.setUseEndTime(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        return req;
    }

    @Test
    @DisplayName("创建：落为草稿态，剩余量等于发行总量，逻辑删除位与版本号初始化")
    void createShouldApplyDefaults() {
        MallCouponDO coupon = CouponRequestConvert.toCouponDO(createReq());

        assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatusEnum.DRAFT.getCode());
        assertThat(coupon.getRemainCount()).isEqualTo(1000);
        assertThat(coupon.getIsDeleted()).isZero();
        assertThat(coupon.getVersion()).isZero();
        assertThat(coupon.getCreateTime()).isNotNull();
        assertThat(coupon.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("创建：门槛金额未填时落 0（无门槛），不落 null")
    void createShouldDefaultMinOrderAmountToZero() {
        CreateCouponReq req = createReq();
        req.setMinOrderAmount(null);

        assertThat(CouponRequestConvert.toCouponDO(req).getMinOrderAmount()).isZero();
    }

    @Test
    @DisplayName("修改：可变字段被覆盖")
    void applyUpdateShouldOverwriteMutableFields() {
        MallCouponDO target = CouponRequestConvert.toCouponDO(createReq());
        UpdateCouponReq req = new UpdateCouponReq();
        req.setId(target.getId());
        req.setCouponName("满 200 减 30");
        req.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        req.setFaceValue(3000L);
        req.setMinOrderAmount(20000L);
        req.setTotalCount(500);
        req.setPerUserLimit(2);
        req.setUseStartTime(LocalDateTime.of(2026, 2, 1, 0, 0));
        req.setUseEndTime(LocalDateTime.of(2026, 6, 30, 23, 59, 59));

        CouponRequestConvert.applyUpdate(req, target);

        assertThat(target.getCouponName()).isEqualTo("满 200 减 30");
        assertThat(target.getFaceValue()).isEqualTo(3000L);
        assertThat(target.getMinOrderAmount()).isEqualTo(20000L);
        assertThat(target.getTotalCount()).isEqualTo(500);
    }

    @Test
    @DisplayName("修改：不得触碰剩余量、状态、版本号与创建时间")
    void applyUpdateShouldNotTouchIssuanceFields() {
        MallCouponDO target = CouponRequestConvert.toCouponDO(createReq());
        // 模拟已有领取发生：剩余量已被扣减、状态已发布、版本号已递增
        target.setRemainCount(700);
        target.setCouponStatus(CouponStatusEnum.PUBLISHED.getCode());
        target.setVersion(5);
        LocalDateTime originalCreateTime = target.getCreateTime();

        UpdateCouponReq req = new UpdateCouponReq();
        req.setId(target.getId());
        req.setCouponName("满 100 减 20");
        req.setTotalCount(2000);
        req.setPerUserLimit(3);

        CouponRequestConvert.applyUpdate(req, target);

        assertThat(target.getRemainCount()).isEqualTo(700);
        assertThat(target.getCouponStatus()).isEqualTo(CouponStatusEnum.PUBLISHED.getCode());
        assertThat(target.getVersion()).isEqualTo(5);
        assertThat(target.getCreateTime()).isEqualTo(originalCreateTime);
        assertThat(target.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("修改：门槛金额未填时落 0，不落 null")
    void applyUpdateShouldDefaultMinOrderAmountToZero() {
        MallCouponDO target = CouponRequestConvert.toCouponDO(createReq());
        UpdateCouponReq req = new UpdateCouponReq();
        req.setId(target.getId());
        req.setCouponName("改名");
        req.setMinOrderAmount(null);

        CouponRequestConvert.applyUpdate(req, target);

        assertThat(target.getMinOrderAmount()).isZero();
    }
}
