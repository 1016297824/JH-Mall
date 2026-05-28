package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.vo.GrowthRecordVO;
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
 * GrowthServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock
    private MallUserGrowthLogMapper mallUserGrowthLogMapper;

    @InjectMocks
    private GrowthLogServiceImpl growthService;

    @Test
    void getGrowthRecordsShouldReturnPagedResults() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();
        logDO.setId(1L);
        logDO.setUserId(1L);
        logDO.setBizType("order");
        logDO.setBizNo("ORD001");
        logDO.setChangeType(1);
        logDO.setGrowth(50);
        logDO.setBeforeGrowth(100);
        logDO.setAfterGrowth(150);
        logDO.setRemark("下单赠送");
        logDO.setCreateTime(LocalDateTime.now());

        Page<MallUserGrowthLogDO> logPage = new Page<>(1, 10);
        logPage.setRecords(Collections.singletonList(logDO));
        logPage.setTotal(1);

        when(mallUserGrowthLogMapper.selectByUserIdPage(any(), eq(1L), eq("order"))).thenReturn(logPage);

        IPage<GrowthRecordVO> result = growthService.getGrowthRecords(1L, "order", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        GrowthRecordVO vo = result.getRecords().get(0);
        assertEquals("order", vo.getBizType());
        assertEquals(50, vo.getGrowth());
    }
}
