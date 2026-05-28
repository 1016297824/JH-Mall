package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.enums.user.GrowthChangeTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;
import com.mall.user.VO.PointsRecordVO;
import com.mall.user.VO.PointsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointsServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private MallPointsAccountMapper mallPointsAccountMapper;

    @Mock
    private MallUserPointsLogMapper mallUserPointsLogMapper;

    @InjectMocks
    private PointsServiceImpl pointsService;

    private MallPointsAccountDO buildAccount() {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setId(1L);
        account.setUserId(1L);
        account.setTotalPoints(500);
        account.setAvailablePoints(300);
        account.setUsedPoints(150);
        account.setExpiredPoints(50);
        account.setVersion(0);
        account.setIsDeleted(0);
        return account;
    }

    @Test
    void getPointsShouldReturnCorrectBalance() {
        MallPointsAccountDO account = buildAccount();
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(account);

        PointsVO result = pointsService.getPoints(1L);

        assertNotNull(result);
        assertEquals(500, result.getTotalPoints());
        assertEquals(300, result.getAvailablePoints());
        assertEquals(150, result.getUsedPoints());
        assertEquals(50, result.getExpiredPoints());
        verify(mallPointsAccountMapper).selectOne(any());
    }

    @Test
    void getPointsShouldThrowAccountNotFoundWhenUserNotExists() {
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> pointsService.getPoints(999L));
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND.getCode(), ex.getErrorCode());
        verify(mallPointsAccountMapper).selectOne(any());
    }

    @Test
    void getPointsRecordsShouldReturnPagedResults() {
        MallUserPointsLogDO logDO = new MallUserPointsLogDO();
        logDO.setId(1L);
        logDO.setUserId(1L);
        logDO.setBizType("order");
        logDO.setBizNo("ORD001");
        logDO.setChangeType(GrowthChangeTypeEnum.INCREASE.getCode());
        logDO.setPoints(100);
        logDO.setBeforePoints(200);
        logDO.setAfterPoints(300);
        logDO.setRemark("下单赠送");
        logDO.setCreateTime(LocalDateTime.now());

        Page<MallUserPointsLogDO> logPage = new Page<>(1, 10);
        logPage.setRecords(Collections.singletonList(logDO));
        logPage.setTotal(1);

        lenient().when(mallUserPointsLogMapper.selectPage(any(), any())).thenReturn(logPage);

        IPage<PointsRecordVO> result = pointsService.getPointsRecords(1L, "order", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        PointsRecordVO vo = result.getRecords().get(0);
        assertEquals("order", vo.getBizType());
        assertEquals(100, vo.getPoints());
        verify(mallUserPointsLogMapper).selectPage(any(), any());
    }

    @Test
    void addPointsShouldCallMapperAddPoints() {
        MallPointsAccountDO account = buildAccount();
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(account);
        when(mallPointsAccountMapper.addPoints(eq(1L), eq(100), eq(0))).thenReturn(1);
        when(mallUserPointsLogMapper.insert(any(MallUserPointsLogDO.class))).thenReturn(1);

        pointsService.addPoints(1L, 100, BizTypeEnum.ORDER, "ORD001");

        verify(mallPointsAccountMapper).addPoints(eq(1L), eq(100), eq(0));
        verify(mallUserPointsLogMapper).insert(any(MallUserPointsLogDO.class));
    }

    @Test
    void addPointsShouldThrowWhenAccountNotFound() {
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> pointsService.addPoints(999L, 100, BizTypeEnum.ORDER, "ORD001"));
    }

    @Test
    void addPointsShouldRetryOnOptimisticLockFailure() {
        MallPointsAccountDO account = buildAccount();
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(account);
        lenient().when(mallPointsAccountMapper.addPoints(eq(1L), eq(100), eq(0))).thenReturn(0).thenReturn(1);
        lenient().when(mallUserPointsLogMapper.insert(any(MallUserPointsLogDO.class))).thenReturn(1);

        pointsService.addPoints(1L, 100, BizTypeEnum.ORDER, "ORD001");

        verify(mallPointsAccountMapper, times(2)).addPoints(eq(1L), eq(100), eq(0));
        verify(mallUserPointsLogMapper).insert(any(MallUserPointsLogDO.class));
    }
}
