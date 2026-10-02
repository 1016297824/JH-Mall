package com.mall.marketing.infrastructure.schedule;

import com.mall.marketing.service.CouponClaimService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠券过期定时任务单元测试
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@ExtendWith(MockitoExtension.class)
class CouponExpireTaskTest {

    @Mock private CouponClaimService couponClaimService;
    @InjectMocks private CouponExpireTask couponExpireTask;

    @Test
    @DisplayName("执行一次扫描：委托 Service 批量置过期")
    void shouldInvokeExpireOnce() {
        when(couponClaimService.expireCoupons()).thenReturn(7);

        couponExpireTask.expireCoupons();

        verify(couponClaimService).expireCoupons();
    }

    @Test
    @DisplayName("Scanner 无过期券时也正常结束")
    void shouldHandleZeroAffected() {
        when(couponClaimService.expireCoupons()).thenReturn(0);

        assertThatCode(() -> couponExpireTask.expireCoupons()).doesNotThrowAnyException();

        verify(couponClaimService).expireCoupons();
    }

    @Test
    @DisplayName("Service 异常：吞掉并记录，不向外抛（避免调度线程中断后周期任务不再执行）")
    void shouldSwallowException() {
        when(couponClaimService.expireCoupons()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> couponExpireTask.expireCoupons()).doesNotThrowAnyException();
    }
}
