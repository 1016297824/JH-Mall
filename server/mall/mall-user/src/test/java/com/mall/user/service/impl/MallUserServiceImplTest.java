package com.mall.user.service.impl;

import com.mall.common.enums.user.UserStatusEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.mapper.MallPointsAccountMapper;
import com.mall.user.mapper.MallUserMapper;
import com.mall.user.mapper.MallUserMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallUserServiceImplTest {

    @Mock
    private MallUserMapper mallUserMapper;

    @Mock
    private MallUserMemberMapper mallUserMemberMapper;

    @Mock
    private MallPointsAccountMapper mallPointsAccountMapper;

    @InjectMocks
    private MallUserServiceImpl mallUserService;

    private MallUserDO mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new MallUserDO();
        mockUser.setId(12345L);
        mockUser.setPhone("13800138000");
        mockUser.setPhoneHash("abc123hash");
        mockUser.setPassword("$2a$10$hash");
        mockUser.setNickname("测试用户");
        mockUser.setUserStatus(UserStatusEnum.NORMAL.getCode());
        mockUser.setRegisterType("phone");
        mockUser.setIsPrivacyAgreed(1);
    }

    @Test
    void testSelectByPhoneHash_ShouldReturnUser() {
        when(mallUserMapper.selectByPhoneHash("abc123hash")).thenReturn(mockUser);
        MallUserDO result = mallUserService.selectByPhoneHash("abc123hash");
        assertNotNull(result);
        assertEquals(12345L, result.getId());
        verify(mallUserMapper).selectByPhoneHash("abc123hash");
    }

    @Test
    void testSelectByPhoneHash_ShouldReturnNullWhenNotFound() {
        when(mallUserMapper.selectByPhoneHash("nonexistent")).thenReturn(null);
        MallUserDO result = mallUserService.selectByPhoneHash("nonexistent");
        assertNull(result);
    }

    @Test
    void testSelectByPhone_ShouldHashPhoneAndQuery() {
        when(mallUserMapper.selectByPhoneHash(anyString())).thenReturn(mockUser);
        MallUserDO result = mallUserService.selectByPhone("13800138000");
        assertNotNull(result);
        assertEquals(12345L, result.getId());
        verify(mallUserMapper).selectByPhoneHash(anyString());
    }

    @Test
    void testRegisterByPhone_ShouldCreateUserAndReturnId() {
        when(mallUserMapper.insert(any(MallUserDO.class))).thenReturn(1);
        when(mallUserMemberMapper.insert(any())).thenReturn(1);
        when(mallPointsAccountMapper.insert(any())).thenReturn(1);

        String userId = mallUserService.registerByPhone("13800138000", "hash456", "$2a$10$encoded");

        assertNotNull(userId);
        ArgumentCaptor<MallUserDO> captor = ArgumentCaptor.forClass(MallUserDO.class);
        verify(mallUserMapper).insert(captor.capture());
        MallUserDO saved = captor.getValue();
        assertEquals("13800138000", saved.getPhone());
        assertEquals("hash456", saved.getPhoneHash());
        assertEquals("$2a$10$encoded", saved.getPassword());
        assertEquals("用户8000", saved.getNickname());
        assertEquals(UserStatusEnum.NORMAL.getCode(), saved.getUserStatus());
        assertEquals(1, saved.getIsPrivacyAgreed());
    }

    @Test
    void testUpdatePasswordById_ShouldDelegate() {
        when(mallUserMapper.selectById(12345L)).thenReturn(mockUser);
        doNothing().when(mallUserMapper).updateById(any(MallUserDO.class));

        mallUserService.updatePasswordById("12345", "newHash");

        verify(mallUserMapper).updateById(any(MallUserDO.class));
    }

    @Test
    void testUpdatePhoneById_ShouldDelegate() {
        when(mallUserMapper.selectById(12345L)).thenReturn(mockUser);
        doNothing().when(mallUserMapper).updateById(any(MallUserDO.class));

        mallUserService.updatePhoneById("12345", "13900139000", "newPhoneHash");

        verify(mallUserMapper).updateById(any(MallUserDO.class));
    }

    @Test
    void testUpdateUserStatusById_ShouldDelegate() {
        when(mallUserMapper.selectById(12345L)).thenReturn(mockUser);
        doNothing().when(mallUserMapper).updateById(any(MallUserDO.class));

        mallUserService.updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));

        verify(mallUserMapper).updateById(any(MallUserDO.class));
    }
}
