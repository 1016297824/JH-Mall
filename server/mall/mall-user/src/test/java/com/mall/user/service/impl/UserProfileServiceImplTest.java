package com.mall.user.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.dto.request.UpdateProfileRequest;
import com.mall.user.infrastructure.feign.RemoteAuthAdapter;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberLevelMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import com.mall.user.vo.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserProfileServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private MallUserMapper mallUserMapper;

    @Mock
    private MallUserMemberMapper mallUserMemberMapper;

    @Mock
    private MallUserMemberLevelMapper mallUserMemberLevelMapper;

    @Mock
    private MallPointsAccountMapper mallPointsAccountMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RemoteAuthAdapter remoteAuthAdapter;

    @Mock
    private MallUserConfigProperties mallUserConfigProperties;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private MallUserDO buildUser() {
        MallUserDO user = new MallUserDO();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setNickname("测试用户");
        user.setAvatar("https://example.com/avatar.png");
        user.setGender(1);
        user.setEmail("test@example.com");
        return user;
    }

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

    private MallUserMemberLevelDO buildLevel() {
        MallUserMemberLevelDO level = new MallUserMemberLevelDO();
        level.setId(1L);
        level.setLevelName("普通会员");
        level.setLevelValue(1);
        level.setMinGrowth(0);
        level.setMaxGrowth(199);
        level.setIcon("icon.png");
        return level;
    }

    private MallPointsAccountDO buildAccount() {
        MallPointsAccountDO account = new MallPointsAccountDO();
        account.setId(1L);
        account.setUserId(1L);
        account.setTotalPoints(500);
        account.setAvailablePoints(300);
        account.setUsedPoints(150);
        account.setExpiredPoints(50);
        account.setIsDeleted(0);
        return account;
    }

    @Test
    void getProfileShouldReturnFromCache() {
        UserProfileVO cached = new UserProfileVO();
        cached.setUserId("1");
        cached.setNickname("缓存的用户");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cached);

        UserProfileVO result = userProfileService.getProfile(1L);

        assertNotNull(result);
        assertEquals("缓存的用户", result.getNickname());
        verify(mallUserMapper, never()).selectById(any());
    }

    @Test
    void getProfileShouldQueryDbWhenCacheMiss() {
        MallUserDO user = buildUser();
        MallUserMemberDO member = buildMember();
        MallUserMemberLevelDO level = buildLevel();
        MallPointsAccountDO account = buildAccount();

        MallUserConfigProperties.Profile profileConfig = new MallUserConfigProperties.Profile();
        profileConfig.setCacheTtl(600);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        when(mallUserMapper.selectById(1L)).thenReturn(user);
        when(mallUserMemberMapper.selectOne(any())).thenReturn(member);
        when(mallUserMemberLevelMapper.selectList(isNull())).thenReturn(Collections.singletonList(level));
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(account);
        when(remoteAuthAdapter.maskPhone("13800138000")).thenReturn("138****8000");
        when(mallUserConfigProperties.getProfile()).thenReturn(profileConfig);

        UserProfileVO result = userProfileService.getProfile(1L);

        assertNotNull(result);
        assertEquals("1", result.getUserId());
        assertEquals("测试用户", result.getNickname());
        assertEquals("138****8000", result.getPhone());
        assertEquals("普通会员", result.getMembershipLevel());
        assertEquals(500, result.getPoints());
        assertEquals(300, result.getAvailablePoints());
    }

    @Test
    void getProfileShouldThrowWhenUserNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(mallUserMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userProfileService.getProfile(999L));
    }

    @Test
    void updateProfileShouldUpdateAndReturnFreshProfile() {
        MallUserDO user = buildUser();
        MallUserMemberDO member = buildMember();
        MallUserMemberLevelDO level = buildLevel();
        MallPointsAccountDO account = buildAccount();
        MallUserConfigProperties.Profile profileConfig = new MallUserConfigProperties.Profile();
        profileConfig.setCacheTtl(600);

        when(mallUserMapper.selectById(1L)).thenReturn(user);
        when(mallUserMapper.updateById(any(MallUserDO.class))).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(mallUserMemberMapper.selectOne(any())).thenReturn(member);
        when(mallUserMemberLevelMapper.selectList(isNull())).thenReturn(Collections.singletonList(level));
        when(mallPointsAccountMapper.selectOne(any())).thenReturn(account);
        when(remoteAuthAdapter.maskPhone(anyString())).thenReturn("138****8000");
        when(mallUserConfigProperties.getProfile()).thenReturn(profileConfig);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setGender(2);

        UserProfileVO result = userProfileService.updateProfile(1L, request);

        assertNotNull(result);
        assertEquals("新昵称", result.getNickname());
        verify(redisTemplate).delete(anyString());
    }
}
