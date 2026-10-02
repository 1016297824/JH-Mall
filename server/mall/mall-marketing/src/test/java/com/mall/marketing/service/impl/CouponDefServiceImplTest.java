package com.mall.marketing.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.dto.request.CreateCouponReq;
import com.mall.marketing.dto.request.UpdateCouponReq;
import com.mall.marketing.dto.response.CouponDefResp;
import com.mall.marketing.mapper.MallCouponMapper;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠券定义服务单元测试
 *
 * <p>覆盖设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.1 的四类操作，
 * 重点是「状态决定可改范围」这组业务规则。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CouponDefServiceImplTest {

    private static final Long COUPON_ID = 10L;

    @Mock private MallCouponMapper couponMapper;

    @InjectMocks private CouponDefServiceImpl couponDefService;

    /**
     * 断言抛出指定错误码的业务异常
     *
     * @param expectedCode 期望的错误码字面量
     * @param callable     被测调用
     */
    private static void assertErrorCode(String expectedCode, ThrowingCallable callable) {
        BusinessException ex = catchThrowableOfType(callable, BusinessException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
    }

    /** 合法的满减券创建请求 */
    private CreateCouponReq validReduceReq() {
        CreateCouponReq req = new CreateCouponReq();
        req.setCouponName("满 100 减 10");
        req.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        req.setFaceValue(1000L);
        req.setMinOrderAmount(10000L);
        req.setTotalCount(1000);
        req.setPerUserLimit(1);
        req.setUseStartTime(LocalDateTime.now().minusDays(1));
        req.setUseEndTime(LocalDateTime.now().plusDays(30));
        return req;
    }

    /** 合法的折扣券创建请求 */
    private CreateCouponReq validDiscountReq() {
        CreateCouponReq req = new CreateCouponReq();
        req.setCouponName("满 200 享 8.5 折");
        req.setCouponType(CouponTypeEnum.DISCOUNT.getCode());
        req.setDiscountRate(85);
        req.setDiscountLimit(5000L);
        req.setMinOrderAmount(20000L);
        req.setTotalCount(500);
        req.setPerUserLimit(2);
        req.setUseStartTime(LocalDateTime.now().minusDays(1));
        req.setUseEndTime(LocalDateTime.now().plusDays(30));
        return req;
    }

    /** 构造一条指定状态的券定义 */
    private MallCouponDO coupon(CouponStatusEnum status) {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setId(COUPON_ID);
        coupon.setCouponName("满 100 减 10");
        coupon.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
        coupon.setFaceValue(1000L);
        coupon.setMinOrderAmount(10000L);
        coupon.setTotalCount(1000);
        coupon.setRemainCount(800);
        coupon.setPerUserLimit(1);
        coupon.setUseStartTime(LocalDateTime.now().minusDays(1));
        coupon.setUseEndTime(LocalDateTime.now().plusDays(30));
        coupon.setCouponStatus(status.getCode());
        coupon.setIsDeleted(0);
        coupon.setVersion(3);
        return coupon;
    }

    // ═══════════════════════════════════════════════════════════
    // 可领券列表
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("可领券列表 listAvailableCoupons")
    class ListAvailableCoupons {

        @Test
        @DisplayName("委托 Mapper 查询并补齐类型/状态描述")
        void listShouldConvertAndEnrichDesc() {
            when(couponMapper.selectAvailable(any(LocalDateTime.class), eq(20)))
                    .thenReturn(List.of(coupon(CouponStatusEnum.PUBLISHED)));

            List<CouponDefResp> result = couponDefService.listAvailableCoupons(20);

            assertThat(result).hasSize(1);
            CouponDefResp resp = result.get(0);
            assertThat(resp.getCouponName()).isEqualTo("满 100 减 10");
            assertThat(resp.getCouponTypeDesc()).isEqualTo(CouponTypeEnum.FULL_REDUCE.getDescription());
            assertThat(resp.getCouponStatusDesc()).isEqualTo(CouponStatusEnum.PUBLISHED.getDescription());
        }

        @Test
        @DisplayName("无可用券时返回空列表")
        void listEmpty() {
            when(couponMapper.selectAvailable(any(LocalDateTime.class), eq(20))).thenReturn(List.of());

            assertThat(couponDefService.listAvailableCoupons(20)).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 创建
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("创建券定义 createCouponDef")
    class CreateCouponDef {

        @Test
        @DisplayName("满减券：落为草稿态，剩余量等于发行总量，返回新 ID")
        void createReduceCoupon() {
            when(couponMapper.insert(any(MallCouponDO.class))).thenAnswer(inv -> {
                MallCouponDO saved = inv.getArgument(0);
                saved.setId(COUPON_ID);
                return 1;
            });

            Long id = couponDefService.createCouponDef(validReduceReq());

            assertThat(id).isEqualTo(COUPON_ID);
            ArgumentCaptor<MallCouponDO> captor = ArgumentCaptor.forClass(MallCouponDO.class);
            verify(couponMapper).insert(captor.capture());
            MallCouponDO saved = captor.getValue();
            assertThat(saved.getCouponStatus()).isEqualTo(CouponStatusEnum.DRAFT.getCode());
            assertThat(saved.getRemainCount()).isEqualTo(saved.getTotalCount());
            assertThat(saved.getIsDeleted()).isZero();
        }

        @Test
        @DisplayName("无门槛券：面值大于 0 即通过校验")
        void createNoThresholdCoupon() {
            CreateCouponReq req = validReduceReq();
            req.setCouponType(CouponTypeEnum.NO_THRESHOLD.getCode());
            req.setFaceValue(500L);
            req.setMinOrderAmount(0L);
            when(couponMapper.insert(any(MallCouponDO.class))).thenAnswer(inv -> {
                MallCouponDO saved = inv.getArgument(0);
                saved.setId(COUPON_ID);
                return 1;
            });

            assertThat(couponDefService.createCouponDef(req)).isEqualTo(COUPON_ID);
        }

        @Test
        @DisplayName("折扣券：折扣率 1~99 内通过校验")
        void createDiscountCoupon() {
            when(couponMapper.insert(any(MallCouponDO.class))).thenAnswer(inv -> {
                MallCouponDO saved = inv.getArgument(0);
                saved.setId(COUPON_ID);
                return 1;
            });

            assertThat(couponDefService.createCouponDef(validDiscountReq())).isEqualTo(COUPON_ID);
        }

        @Test
        @DisplayName("名称为空 → A0401")
        void createWithBlankName() {
            CreateCouponReq req = validReduceReq();
            req.setCouponName("   ");

            assertErrorCode(ErrorCode.PARAM_MISSING.getCode(), () -> couponDefService.createCouponDef(req));
            verify(couponMapper, never()).insert(any(MallCouponDO.class));
        }

        @Test
        @DisplayName("券类型为空 → A0401")
        void createWithoutType() {
            CreateCouponReq req = validReduceReq();
            req.setCouponType(null);

            assertErrorCode(ErrorCode.PARAM_MISSING.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("满减券面值为 0 → A0402")
        void createReduceWithZeroFaceValue() {
            CreateCouponReq req = validReduceReq();
            req.setFaceValue(0L);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("无门槛券面值为负 → A0402")
        void createNoThresholdWithNegativeFaceValue() {
            CreateCouponReq req = validReduceReq();
            req.setCouponType(CouponTypeEnum.NO_THRESHOLD.getCode());
            req.setFaceValue(-100L);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("折扣券缺折扣率 → A0402")
        void createDiscountWithoutRate() {
            CreateCouponReq req = validDiscountReq();
            req.setDiscountRate(null);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("折扣券折扣率 100（等于不打折）→ A0402")
        void createDiscountWithRate100() {
            CreateCouponReq req = validDiscountReq();
            req.setDiscountRate(100);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("发行总量为 0 → A0402")
        void createWithZeroTotalCount() {
            CreateCouponReq req = validReduceReq();
            req.setTotalCount(0);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("每人限领数超过发行总量 → A0402")
        void createWithPerUserLimitExceedingTotal() {
            CreateCouponReq req = validReduceReq();
            req.setTotalCount(10);
            req.setPerUserLimit(11);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("截止时间早于开始时间 → A0402")
        void createWithInvalidTimeRange() {
            CreateCouponReq req = validReduceReq();
            req.setUseStartTime(LocalDateTime.now().plusDays(10));
            req.setUseEndTime(LocalDateTime.now().plusDays(1));

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(), () -> couponDefService.createCouponDef(req));
        }

        @Test
        @DisplayName("有效期为空 → A0401")
        void createWithoutTimeRange() {
            CreateCouponReq req = validReduceReq();
            req.setUseStartTime(null);

            assertErrorCode(ErrorCode.PARAM_MISSING.getCode(), () -> couponDefService.createCouponDef(req));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 修改
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("修改券定义 updateCouponDef")
    class UpdateCouponDef {

        /** 构造一个「改名 + 改面值」的修改请求 */
        private UpdateCouponReq renameAndChangeFaceValue() {
            UpdateCouponReq req = new UpdateCouponReq();
            req.setId(COUPON_ID);
            req.setCouponName("满 100 减 20");
            req.setCouponType(CouponTypeEnum.FULL_REDUCE.getCode());
            req.setFaceValue(2000L);
            req.setMinOrderAmount(10000L);
            req.setTotalCount(1000);
            req.setPerUserLimit(1);
            req.setUseStartTime(LocalDateTime.now().minusDays(1));
            req.setUseEndTime(LocalDateTime.now().plusDays(30));
            return req;
        }

        @Test
        @DisplayName("券不存在 → A0501")
        void updateNotFound() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> couponDefService.updateCouponDef(renameAndChangeFaceValue()));
        }

        @Test
        @DisplayName("已废弃的券不允许修改 → A0503")
        void updateDiscardedCoupon() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.DISCARDED));

            assertErrorCode(ErrorCode.RESOURCE_STATUS_ERROR.getCode(),
                    () -> couponDefService.updateCouponDef(renameAndChangeFaceValue()));
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
        }

        @Test
        @DisplayName("已结束的券不允许修改 → A0503")
        void updateEndedCoupon() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.ENDED));

            assertErrorCode(ErrorCode.RESOURCE_STATUS_ERROR.getCode(),
                    () -> couponDefService.updateCouponDef(renameAndChangeFaceValue()));
        }

        @Test
        @DisplayName("草稿态：全部字段均可修改")
        void updateDraftAllowsAllFields() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.DRAFT));
            when(couponMapper.updateById(any(MallCouponDO.class))).thenReturn(1);

            couponDefService.updateCouponDef(renameAndChangeFaceValue());

            ArgumentCaptor<MallCouponDO> captor = ArgumentCaptor.forClass(MallCouponDO.class);
            verify(couponMapper).updateById(captor.capture());
            MallCouponDO updated = captor.getValue();
            assertThat(updated.getCouponName()).isEqualTo("满 100 减 20");
            assertThat(updated.getFaceValue()).isEqualTo(2000L);
            // 剩余量不受修改影响
            assertThat(updated.getRemainCount()).isEqualTo(800);
        }

        @Test
        @DisplayName("已发布态：只走字段级改名，禁止整行 updateById（防覆盖并发扣减的 remain_count）")
        void updatePublishedOnlyAllowsName() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.PUBLISHED));
            when(couponMapper.updateNameById(COUPON_ID, "满 100 减 20")).thenReturn(1);

            couponDefService.updateCouponDef(renameAndChangeFaceValue());

            verify(couponMapper).updateNameById(COUPON_ID, "满 100 减 20");
            // 关键断言：不能整行写回——否则会用陈旧快照覆盖并发的 remain_count，导致超发
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
        }

        @Test
        @DisplayName("已发布态改名称为空 → A0401，不落库")
        void updatePublishedWithBlankName() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.PUBLISHED));
            UpdateCouponReq req = renameAndChangeFaceValue();
            req.setCouponName("  ");

            assertErrorCode(ErrorCode.PARAM_MISSING.getCode(),
                    () -> couponDefService.updateCouponDef(req));
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
            verify(couponMapper, never()).updateNameById(any(), any());
        }

        @Test
        @DisplayName("草稿态改为非法面值 → A0402，不落库")
        void updateDraftWithInvalidFaceValue() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.DRAFT));
            UpdateCouponReq req = renameAndChangeFaceValue();
            req.setFaceValue(0L);

            assertErrorCode(ErrorCode.PARAM_INVALID.getCode(),
                    () -> couponDefService.updateCouponDef(req));
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 删除
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("删除券定义 deleteCouponDef")
    class DeleteCouponDef {

        @Test
        @DisplayName("券不存在 → A0501")
        void deleteNotFound() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID)).thenReturn(null);

            assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    () -> couponDefService.deleteCouponDef(COUPON_ID));
        }

        @Test
        @DisplayName("草稿态：物理删除")
        void deleteDraftPhysically() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.DRAFT));
            when(couponMapper.deleteById(COUPON_ID)).thenReturn(1);

            couponDefService.deleteCouponDef(COUPON_ID);

            verify(couponMapper).deleteById(COUPON_ID);
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
        }

        @Test
        @DisplayName("已发布：走字段级状态更新置为废弃，不物理删除、不整行写回")
        void deletePublishedAsDiscarded() {
            when(couponMapper.selectByIdNotDeleted(COUPON_ID))
                    .thenReturn(coupon(CouponStatusEnum.PUBLISHED));
            when(couponMapper.updateStatusById(COUPON_ID, CouponStatusEnum.DISCARDED.getCode()))
                    .thenReturn(1);

            couponDefService.deleteCouponDef(COUPON_ID);

            verify(couponMapper).updateStatusById(COUPON_ID, CouponStatusEnum.DISCARDED.getCode());
            verify(couponMapper, never()).deleteById(COUPON_ID);
            // 同样不允许整行写回，避免覆盖并发的 remain_count
            verify(couponMapper, never()).updateById(any(MallCouponDO.class));
        }
    }
}
