package com.mall.user.convert.response;

import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.UserProfileVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserConvert 用户资料聚合转换器单元测试
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
class UserConvertTest {

    private MallUserDO buildUser() {
        MallUserDO user = new MallUserDO();
        user.setId(1L);
        user.setNickname("测试用户");
        user.setAvatar("https://img.example.com/avatar.jpg");
        user.setGender(1);
        user.setBirthday(LocalDateTime.of(1995, 6, 15, 0, 0, 0));
        user.setEmail("test@example.com");
        return user;
    }

    @Test
    void toUserProfileVOShouldMapAllFields() {
        MallUserDO user = buildUser();
        String maskedPhone = "138****8000";

        MallUserMemberDO member = new MallUserMemberDO();
        member.setGrowth(300);
        member.setTotalGrowth(1000);

        MallUserMemberLevelDO level = new MallUserMemberLevelDO();
        level.setLevelName("金卡会员");
        level.setIcon("/icon/gold.png");

        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setTotalPoints(500);
        account.setAvailablePoints(300);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, maskedPhone, member, level, account);

        assertEquals("1", vo.getUserId());
        assertEquals("测试用户", vo.getNickname());
        assertEquals("https://img.example.com/avatar.jpg", vo.getAvatar());
        assertEquals(1, vo.getGender());
        assertEquals("男", vo.getGenderName());
        assertEquals("1995-06-15", vo.getBirthday());
        assertEquals("138****8000", vo.getPhone());
        assertEquals("test@example.com", vo.getEmail());
        assertEquals("金卡会员", vo.getMembershipLevel());
        assertEquals("/icon/gold.png", vo.getMembershipIcon());
        assertEquals(300, vo.getGrowth());
        assertEquals(1000, vo.getTotalGrowth());
        assertEquals(500, vo.getPoints());
        assertEquals(300, vo.getAvailablePoints());
    }

    @Test
    void toUserProfileVOShouldHandleUnknownGender() {
        MallUserDO user = buildUser();
        user.setGender(99);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, null, null, null, null);

        assertEquals("未知", vo.getGenderName());
    }

    @Test
    void toUserProfileVOShouldHandleNullGender() {
        MallUserDO user = buildUser();
        user.setGender(null);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, null, null, null, null);

        assertEquals("未知", vo.getGenderName());
    }

    @Test
    void toUserProfileVOShouldHandleNullBirthday() {
        MallUserDO user = buildUser();
        user.setBirthday(null);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, null, null, null, null);

        assertNull(vo.getBirthday());
    }

    @Test
    void toUserProfileVOShouldHandleNullMember() {
        MallUserDO user = buildUser();

        UserProfileVO vo = UserConvert.toUserProfileVO(user, "138****8000", null, null, null);

        assertEquals("1", vo.getUserId());
        assertEquals("测试用户", vo.getNickname());
        assertNull(vo.getMembershipLevel());
        assertNull(vo.getMembershipIcon());
        assertNull(vo.getGrowth());
        assertNull(vo.getTotalGrowth());
        assertNull(vo.getPoints());
        assertNull(vo.getAvailablePoints());
    }

    @Test
    void toUserProfileVOShouldHandleNullLevel() {
        MallUserDO user = buildUser();
        MallUserMemberDO member = new MallUserMemberDO();
        member.setGrowth(300);
        member.setTotalGrowth(1000);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, "138****8000", member, null, null);

        assertEquals(300, vo.getGrowth());
        assertNull(vo.getMembershipLevel());
    }

    @Test
    void toUserProfileVOShouldHandleNullAccount() {
        MallUserDO user = buildUser();
        MallUserMemberDO member = new MallUserMemberDO();
        member.setGrowth(300);
        member.setTotalGrowth(1000);
        MallUserMemberLevelDO level = new MallUserMemberLevelDO();
        level.setLevelName("金卡会员");
        level.setIcon("/icon/gold.png");

        UserProfileVO vo = UserConvert.toUserProfileVO(user, "138****8000", member, level, null);

        assertEquals("金卡会员", vo.getMembershipLevel());
        assertNull(vo.getPoints());
        assertNull(vo.getAvailablePoints());
    }

    @Test
    void toUserProfileVOShouldHandleFemaleGender() {
        MallUserDO user = buildUser();
        user.setGender(2);

        UserProfileVO vo = UserConvert.toUserProfileVO(user, null, null, null, null);

        assertEquals("女", vo.getGenderName());
    }
}
