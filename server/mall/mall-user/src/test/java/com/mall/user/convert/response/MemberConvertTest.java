package com.mall.user.convert.response;

import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.GrowthVO;
import com.mall.user.VO.MemberLevelVO;
import com.mall.user.VO.MembershipVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberConvert 会员转换器单元测试
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
class MemberConvertTest {

    private MallUserMemberLevelDO buildLevel(Long id, String name, Integer levelValue,
                                              Integer minGrowth, Integer maxGrowth, String icon, String benefitsJson) {
        MallUserMemberLevelDO level = new MallUserMemberLevelDO();
        level.setId(id);
        level.setLevelName(name);
        level.setLevelValue(levelValue);
        level.setMinGrowth(minGrowth);
        level.setMaxGrowth(maxGrowth);
        level.setIcon(icon);
        level.setBenefitsJson(benefitsJson);
        return level;
    }

    private MallUserMemberDO buildMember(Long userId, Long levelId, Integer growth, Integer totalGrowth) {
        MallUserMemberDO member = new MallUserMemberDO();
        member.setUserId(userId);
        member.setLevelId(levelId);
        member.setGrowth(growth);
        member.setTotalGrowth(totalGrowth);
        return member;
    }

    @Test
    void toMemberLevelVOShouldMapAllFields() {
        MallUserMemberLevelDO level = buildLevel(1L, "金卡会员", 3, 500, 2000, "/icon/gold.png", "[\"包邮\",\"9.5折\"]");

        MemberLevelVO vo = MemberConvert.toMemberLevelVO(level);

        assertEquals("金卡会员", vo.getLevelName());
        assertEquals("/icon/gold.png", vo.getIcon());
        assertEquals(3, vo.getLevelValue());
    }

    @Test
    void toMemberLevelVOShouldHandleNull() {
        MemberLevelVO vo = MemberConvert.toMemberLevelVO(null);

        assertNotNull(vo);
        assertNull(vo.getLevelName());
        assertNull(vo.getIcon());
        assertNull(vo.getLevelValue());
    }

    @Test
    void toMembershipVOShouldMapAllFields() {
        MallUserMemberDO member = buildMember(1L, 2L, 300, 1000);
        MallUserMemberLevelDO currentLevel = buildLevel(2L, "银卡会员", 2, 200, 500, "/icon/silver.png", "[\"满99包邮\",\"9.8折\"]");
        MallUserMemberLevelDO nextLevel = buildLevel(3L, "金卡会员", 3, 500, 2000, "/icon/gold.png", "[\"包邮\",\"9.5折\"]");
        List<String> benefits = Arrays.asList("满99包邮", "9.8折");

        MembershipVO vo = MemberConvert.toMembershipVO(member, currentLevel, nextLevel, benefits);

        assertEquals(300, vo.getGrowth());
        assertEquals(1000, vo.getTotalGrowth());
        assertNotNull(vo.getCurrentLevel());
        assertEquals("银卡会员", vo.getCurrentLevel().getLevelName());
        assertNotNull(vo.getNextLevel());
        assertEquals("金卡会员", vo.getNextLevel().getLevelName());
        assertEquals(benefits, vo.getBenefits());
    }

    @Test
    void toMembershipVOShouldHandleNullNextLevel() {
        MallUserMemberDO member = buildMember(1L, 4L, 3000, 5000);
        MallUserMemberLevelDO currentLevel = buildLevel(4L, "钻石会员", 4, 2000, 99999, "/icon/diamond.png", "[\"包邮\",\"9.0折\"]");

        MembershipVO vo = MemberConvert.toMembershipVO(member, currentLevel, null, Collections.emptyList());

        assertEquals(3000, vo.getGrowth());
        assertNotNull(vo.getCurrentLevel());
        assertNull(vo.getNextLevel());
    }

    @Test
    void toGrowthVOShouldMapAllFields() {
        MallUserMemberDO member = buildMember(1L, 2L, 300, 1000);
        MallUserMemberLevelDO currentLevel = buildLevel(2L, "银卡会员", 2, 200, 500, "/icon/silver.png", null);
        MallUserMemberLevelDO nextLevel = buildLevel(3L, "金卡会员", 3, 500, 2000, "/icon/gold.png", null);

        GrowthVO vo = MemberConvert.toGrowthVO(member, currentLevel, nextLevel, 200, 50);

        assertEquals(300, vo.getGrowth());
        assertEquals(1000, vo.getTotalGrowth());
        assertNotNull(vo.getCurrentLevel());
        assertEquals("银卡会员", vo.getCurrentLevel().getLevelName());
        assertNotNull(vo.getNextLevel());
        assertEquals("金卡会员", vo.getNextLevel().getLevelName());
        assertEquals(200, vo.getNeedGrowth());
        assertEquals(50, vo.getProgressPercent());
    }

    @Test
    void toGrowthVOShouldHandleNullNextLevel() {
        MallUserMemberDO member = buildMember(1L, 4L, 3000, 5000);
        MallUserMemberLevelDO currentLevel = buildLevel(4L, "钻石会员", 4, 2000, 99999, "/icon/diamond.png", null);

        GrowthVO vo = MemberConvert.toGrowthVO(member, currentLevel, null, 0, 100);

        assertNotNull(vo.getCurrentLevel());
        assertNull(vo.getNextLevel());
        assertEquals(0, vo.getNeedGrowth());
        assertEquals(100, vo.getProgressPercent());
    }
}
