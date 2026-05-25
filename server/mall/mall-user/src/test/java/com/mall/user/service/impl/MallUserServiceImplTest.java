package com.mall.user.service.impl;

import com.mall.api.enums.UserStatusEnum;
import com.mall.user.domain.MallUser;
import com.mall.user.mapper.MallUserMapper;
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

    @InjectMocks
    private MallUserServiceImpl mallUserService;

    private MallUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new MallUser();
        mockUser.setId("12345");
        mockUser.setPhone("13800138000");
        mockUser.setPhoneHash("abc123hash");
        mockUser.setPassword("$2a$10$hash");
        mockUser.setNickname("测试用户");
        mockUser.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));
        mockUser.setRegisterType("phone");
    }

    @Test
    void testSelectByPhoneHash_ShouldReturnUser() {
        when(mallUserMapper.selectByPhoneHash("abc123hash")).thenReturn(mockUser);
        MallUser result = mallUserService.selectByPhoneHash("abc123hash");
        assertNotNull(result);
        assertEquals("12345", result.getId());
        verify(mallUserMapper).selectByPhoneHash("abc123hash");
    }

    @Test
    void testSelectByPhoneHash_ShouldReturnNullWhenNotFound() {
        when(mallUserMapper.selectByPhoneHash("nonexistent")).thenReturn(null);
        MallUser result = mallUserService.selectByPhoneHash("nonexistent");
        assertNull(result);
    }

    @Test
    void testSelectByPhone_ShouldHashPhoneAndQuery() {
        when(mallUserMapper.selectByPhoneHash(anyString())).thenReturn(mockUser);
        MallUser result = mallUserService.selectByPhone("13800138000");
        assertNotNull(result);
        assertEquals("12345", result.getId());
        verify(mallUserMapper).selectByPhoneHash(anyString());
    }

    @Test
    void testSelectByWechatOpenId_ShouldReturnNull() {
        MallUser result = mallUserService.selectByWechatOpenId("open123");
        assertNull(result);
    }

    @Test
    void testRegisterByPhone_ShouldCreateUserAndReturnId() {
        when(mallUserMapper.insertMallUser(any(MallUser.class))).thenReturn(1);

        String userId = mallUserService.registerByPhone("13800138000", "hash456", "$2a$10$encoded");

        assertNotNull(userId);
        ArgumentCaptor<MallUser> captor = ArgumentCaptor.forClass(MallUser.class);
        verify(mallUserMapper).insertMallUser(captor.capture());
        MallUser saved = captor.getValue();
        assertEquals("13800138000", saved.getPhone());
        assertEquals("hash456", saved.getPhoneHash());
        assertEquals("$2a$10$encoded", saved.getPassword());
        assertEquals("用户8000", saved.getNickname());
        assertEquals("0", saved.getUserStatus());
        assertEquals("phone", saved.getRegisterType());
        assertEquals("1", saved.getIsPrivacyAgreed());
    }

    @Test
    void testUpdatePasswordById_ShouldDelegate() {
        when(mallUserMapper.updatePassword("12345", "newHash")).thenReturn(1);
        int result = mallUserService.updatePasswordById("12345", "newHash");
        assertEquals(1, result);
        verify(mallUserMapper).updatePassword("12345", "newHash");
    }

    @Test
    void testUpdatePhoneById_ShouldDelegate() {
        when(mallUserMapper.updatePhone("12345", "13900139000", "newPhoneHash")).thenReturn(1);
        int result = mallUserService.updatePhoneById("12345", "13900139000", "newPhoneHash");
        assertEquals(1, result);
        verify(mallUserMapper).updatePhone("12345", "13900139000", "newPhoneHash");
    }

    @Test
    void testUpdateUserStatusById_ShouldDelegate() {
        when(mallUserMapper.updateUserStatus("12345", String.valueOf(UserStatusEnum.DELETED.getCode()))).thenReturn(1);
        int result = mallUserService.updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));
        assertEquals(1, result);
        verify(mallUserMapper).updateUserStatus("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));
    }
}
