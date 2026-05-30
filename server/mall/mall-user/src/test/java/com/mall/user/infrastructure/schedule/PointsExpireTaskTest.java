package com.mall.user.infrastructure.schedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.infrastructure.schedule.PointsExpireTask;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserPointsLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PointsExpireTask 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class PointsExpireTaskTest {

    @Mock
    private MallPointsAccountMapper pointsAccountMapper;

    @Mock
    private MallUserPointsLogMapper pointsLogMapper;

    @InjectMocks
    private PointsExpireTask task;

    private Page<MallPointsAccountDO> buildPage(List<MallPointsAccountDO> records) {
        Page<MallPointsAccountDO> page = new Page<>(1, 500);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }

    @Test
    void execute_shouldExpirePointsInBatches() {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setUserId(1L);
        account.setAvailablePoints(100);

        when(pointsAccountMapper.selectPage(any(), any()))
                .thenReturn(buildPage(List.of(account)))
                .thenReturn(buildPage(List.of()));

        assertDoesNotThrow(() -> task.execute());

        verify(pointsAccountMapper).expirePoints(eq(1L));
        verify(pointsLogMapper).insert(any(MallUserPointsLogDO.class));
    }

    @Test
    void execute_shouldSkipZeroAvailablePoints() {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setUserId(1L);
        account.setAvailablePoints(0);

        when(pointsAccountMapper.selectPage(any(), any()))
                .thenReturn(buildPage(List.of(account)))
                .thenReturn(buildPage(List.of()));

        assertDoesNotThrow(() -> task.execute());

        verify(pointsAccountMapper, never()).expirePoints(eq(1L));
        verify(pointsLogMapper, never()).insert(any(MallUserPointsLogDO.class));
    }
}
