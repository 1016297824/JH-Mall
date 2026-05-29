package com.mall.user.convert.response;

import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.VO.PointsRecordVO;
import com.mall.user.VO.PointsVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class PointsConvertTest {

    @Test
    void toPointsVOShouldMapAllFields() {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setTotalPoints(1000);
        account.setAvailablePoints(800);
        account.setUsedPoints(150);
        account.setExpiredPoints(50);

        PointsVO vo = PointsConvert.toPointsVO(account);

        assertEquals(1000, vo.getTotalPoints());
        assertEquals(800, vo.getAvailablePoints());
        assertEquals(150, vo.getUsedPoints());
        assertEquals(50, vo.getExpiredPoints());
    }

    @Test
    void toPointsRecordVOShouldMapAllFields() {
        MallUserPointsLogDO logDO = new MallUserPointsLogDO();
        logDO.setId(1L);
        logDO.setBizType("signin");
        logDO.setChangeType(0);
        logDO.setPoints(10);
        logDO.setBeforePoints(100);
        logDO.setAfterPoints(110);
        logDO.setRemark("签到");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 20, 0, 0);
        logDO.setCreateTime(now);

        PointsRecordVO vo = PointsConvert.toPointsRecordVO(logDO);

        assertEquals(1L, vo.getId());
        assertEquals("signin", vo.getBizType());
        assertNotNull(vo.getBizTypeName());
        assertEquals("签到", vo.getBizTypeName());
        assertEquals(0, vo.getChangeType());
        assertEquals(10, vo.getPoints());
        assertEquals(100, vo.getBeforePoints());
        assertEquals(110, vo.getAfterPoints());
        assertEquals("签到", vo.getRemark());
        Date expectedDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(expectedDate, vo.getCreateTime());
    }

    @Test
    void toPointsRecordVOShouldHandleUnknownBizType() {
        MallUserPointsLogDO logDO = new MallUserPointsLogDO();
        logDO.setBizType("unknown");

        PointsRecordVO vo = PointsConvert.toPointsRecordVO(logDO);

        assertEquals("unknown", vo.getBizType());
        assertEquals("unknown", vo.getBizTypeName());
    }

    @Test
    void toPointsRecordVOShouldHandleNullCreateTime() {
        MallUserPointsLogDO logDO = new MallUserPointsLogDO();

        PointsRecordVO vo = PointsConvert.toPointsRecordVO(logDO);

        assertNull(vo.getCreateTime());
    }

    @Test
    void toPointsRecordVOShouldHandleNullBizType() {
        MallUserPointsLogDO logDO = new MallUserPointsLogDO();

        PointsRecordVO vo = PointsConvert.toPointsRecordVO(logDO);

        assertNull(vo.getBizType());
    }
}
