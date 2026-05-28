package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.enums.user.GrowthChangeTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.mapper.MallUserGrowthLogMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.vo.GrowthRecordVO;
import com.mall.user.vo.GrowthVO;
import com.mall.user.vo.MembershipVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemberServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MallUserMemberMapper mallUserMemberMapper;

    @Mock
    private MallUserMemberLevelMapper mallUserMemberLevelMapper;

    @Mock
    private MallUserGrowthLogMapper mallUserGrowthLogMapper;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private MemberServiceImpl memberService;

    private MallUserMemberDO buildMember() {
        MallUserMemberDO member = new MallUserMemberDO();
        member.setId(1L);
        member.setUserId(1L);
        member.setLevelId(1L);
        member.setGrowth(100);
        member.setTotalGrowth(500);
        member.setIsDeleted(0);
        return member;
    }

    private MallUserMemberLevelDO buildLevel(Long id, String name, int min, int max, int value) {
        MallUserMemberLevelDO level = new MallUserMemberLevelDO();
        level.setId(id);
        level.setLevelName(name);
        level.setLevelValue(value);
        level.setMinGrowth(min);
        level.setMaxGrowth(max);
        level.setIcon("icon.png");
        return level;
    }

    @Test
    void getMembershipShouldReturnCorrectInfo() {
        MallUserMemberDO member = buildMember();
        MallUserMemberLevelDO level1 = buildLevel(1L, "普通会员", 0, 199, 1);
        MallUserMemberLevelDO level2 = buildLevel(2L, "银卡会员", 200, 499, 2);

        when(mallUserMemberMapper.selectOne(any())).thenReturn(member);
        when(mallUserMemberLevelMapper.selectList(isNull())).thenReturn(Arrays.asList(level1, level2));

        MembershipVO result = memberService.getMembership(1L);

        assertNotNull(result);
        assertEquals(100, result.getGrowth());
        assertEquals(500, result.getTotalGrowth());
        assertNotNull(result.getCurrentLevel());
        assertEquals("普通会员", result.getCurrentLevel().getLevelName());
        assertNotNull(result.getNextLevel());
        assertEquals("银卡会员", result.getNextLevel().getLevelName());
    }

    @Test
    void getMembershipShouldThrowWhenMemberNotFound() {
        when(mallUserMemberMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> memberService.getMembership(999L));
    }

    @Test
    void getGrowthShouldReturnCorrectGrowthInfo() {
        MallUserMemberDO member = buildMember();
        MallUserMemberLevelDO level1 = buildLevel(1L, "普通会员", 0, 199, 1);
        MallUserMemberLevelDO level2 = buildLevel(2L, "银卡会员", 200, 499, 2);

        when(mallUserMemberMapper.selectOne(any())).thenReturn(member);
        when(mallUserMemberLevelMapper.selectList(isNull())).thenReturn(Arrays.asList(level1, level2));

        GrowthVO result = memberService.getGrowth(1L);

        assertNotNull(result);
        assertEquals(100, result.getGrowth());
        assertEquals(500, result.getTotalGrowth());
        assertNotNull(result.getCurrentLevel());
        assertNotNull(result.getNextLevel());
        assertTrue(result.getProgressPercent() >= 0);
    }

    @Test
    void addGrowthShouldCallMapperAndInsertLog() {
        MallUserMemberDO member = buildMember();
        MallUserMemberLevelDO level1 = buildLevel(1L, "普通会员", 0, 199, 1);

        when(mallUserMemberMapper.selectOne(any())).thenReturn(member);
        when(mallUserMemberMapper.addGrowth(eq(1L), eq(50))).thenReturn(1);
        when(mallUserGrowthLogMapper.insert(any(MallUserGrowthLogDO.class))).thenReturn(1);
        when(mallUserMemberLevelMapper.selectList(isNull())).thenReturn(Collections.singletonList(level1));

        memberService.addGrowth(1L, 50, BizTypeEnum.ORDER, "ORD001");

        verify(mallUserMemberMapper).addGrowth(eq(1L), eq(50));
        verify(mallUserGrowthLogMapper).insert(any(MallUserGrowthLogDO.class));
    }

    @Test
    void getGrowthRecordsShouldReturnPagedResults() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();
        logDO.setId(1L);
        logDO.setUserId(1L);
        logDO.setBizType("order");
        logDO.setBizNo("ORD001");
        logDO.setChangeType(GrowthChangeTypeEnum.INCREASE.getCode());
        logDO.setGrowth(50);
        logDO.setBeforeGrowth(100);
        logDO.setAfterGrowth(150);
        logDO.setRemark("下单赠送");
        logDO.setCreateTime(LocalDateTime.now());

        Page<MallUserGrowthLogDO> logPage = new Page<>(1, 10);
        logPage.setRecords(Collections.singletonList(logDO));
        logPage.setTotal(1);

        when(mallUserGrowthLogMapper.selectByUserIdPage(any(), eq(1L), eq("order"))).thenReturn(logPage);

        IPage<GrowthRecordVO> result = memberService.getGrowthRecords(1L, "order", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        GrowthRecordVO vo = result.getRecords().get(0);
        assertEquals("order", vo.getBizType());
        assertEquals(50, vo.getGrowth());
    }
}
