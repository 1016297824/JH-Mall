package com.mall.user.convert.response;

import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.VO.GrowthRecordVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class GrowthConvertTest {

    @Test
    void toGrowthRecordVOShouldMapAllFields() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();
        logDO.setId(1L);
        logDO.setBizType("signin");
        logDO.setChangeType(0);
        logDO.setGrowth(50);
        logDO.setBeforeGrowth(100);
        logDO.setAfterGrowth(150);
        logDO.setRemark("签到");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 20, 0, 0);
        logDO.setCreateTime(now);

        GrowthRecordVO vo = GrowthConvert.toGrowthRecordVO(logDO);

        assertEquals(1L, vo.getId());
        assertEquals("signin", vo.getBizType());
        assertEquals("签到", vo.getBizTypeName());
        assertEquals(0, vo.getChangeType());
        assertEquals(50, vo.getGrowth());
        assertEquals(100, vo.getBeforeGrowth());
        assertEquals(150, vo.getAfterGrowth());
        assertEquals("签到", vo.getRemark());
        Date expectedDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(expectedDate, vo.getCreateTime());
    }

    @Test
    void toGrowthRecordVOShouldHandleUnknownBizType() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();
        logDO.setBizType("unknown_type");

        GrowthRecordVO vo = GrowthConvert.toGrowthRecordVO(logDO);

        assertEquals("unknown_type", vo.getBizType());
        assertEquals("unknown_type", vo.getBizTypeName());
    }

    @Test
    void toGrowthRecordVOShouldHandleNullCreateTime() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();

        GrowthRecordVO vo = GrowthConvert.toGrowthRecordVO(logDO);

        assertNull(vo.getCreateTime());
    }

    @Test
    void toGrowthRecordVOShouldHandleNullBizType() {
        MallUserGrowthLogDO logDO = new MallUserGrowthLogDO();

        GrowthRecordVO vo = GrowthConvert.toGrowthRecordVO(logDO);

        assertNull(vo.getBizType());
    }
}
