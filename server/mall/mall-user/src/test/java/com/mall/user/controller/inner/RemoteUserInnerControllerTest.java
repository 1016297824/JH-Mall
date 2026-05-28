package com.mall.user.controller.inner;

import com.mall.common.enums.user.UserStatusEnum;
import com.mall.api.feign.RemoteUserService;
import com.mall.user.DO.MallUserDO;
import com.mall.user.service.IMallUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RemoteUserInnerController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@ExtendWith(MockitoExtension.class)
class RemoteUserInnerControllerTest {

    @Mock
    private IMallUserService mallUserService;

    @InjectMocks
    private RemoteUserInnerController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    private MallUserDO buildMockUser() {
        MallUserDO user = new MallUserDO();
        user.setId(12345L);
        user.setPhone("13800138000");
        user.setPhoneHash("abc123hash");
        user.setPassword("$2a$10$encodedhash");
        user.setNickname("测试用户");
        user.setAvatar("https://example.com/avatar.png");
        user.setEmail("test@example.com");
        user.setEmailHash("email123hash");
        user.setGender(1);
        user.setUserStatus(UserStatusEnum.NORMAL.getCode());
        user.setRegisterType("phone");
        user.setRegisterIp("127.0.0.1");
        user.setIsPrivacyAgreed(1);
        return user;
    }

    @Test
    void testFindByPhone_ShouldReturnDTO() throws Exception {
        MallUserDO mockUser = buildMockUser();
        when(mallUserService.selectByPhone("13800138000")).thenReturn(mockUser);

        mockMvc.perform(get("/inner/user/phone/13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12345))
                .andExpect(jsonPath("$.phone").value("13800138000"))
                .andExpect(jsonPath("$.phoneHash").value("abc123hash"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.nickname").value("测试用户"))
                .andExpect(jsonPath("$.avatar").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.emailHash").value("email123hash"))
                .andExpect(jsonPath("$.gender").value(1))
                .andExpect(jsonPath("$.userStatus").value(UserStatusEnum.NORMAL.getCode()))
                .andExpect(jsonPath("$.registerType").value("phone"))
                .andExpect(jsonPath("$.registerIp").value("127.0.0.1"))
                .andExpect(jsonPath("$.privacyAgreed").value("1"));

        verify(mallUserService).selectByPhone("13800138000");
    }

    @Test
    void testFindByPhone_ShouldReturnNullWhenNotFound() throws Exception {
        when(mallUserService.selectByPhone("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/inner/user/phone/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(mallUserService).selectByPhone("nonexistent");
    }

    @Test
    void testRegister_ShouldReturnUserId() throws Exception {
        RemoteUserService.RegisterRequest request = new RemoteUserService.RegisterRequest();
        request.setPhone("13800138000");
        request.setPhoneHash("hash456");
        request.setPassword("$2a$10$encoded");
        request.setRegisterType("phone");

        when(mallUserService.registerByPhone(
                "13800138000", "hash456", "$2a$10$encoded"
        )).thenReturn("12345");

        mockMvc.perform(post("/inner/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("12345"));

        verify(mallUserService).registerByPhone("13800138000", "hash456", "$2a$10$encoded");
    }

    @Test
    void testUpdatePassword_ShouldSucceed() throws Exception {
        RemoteUserService.PasswordUpdateRequest request = new RemoteUserService.PasswordUpdateRequest();
        request.setNewPassword("$2a$10$newhash");

        doNothing().when(mallUserService).updatePasswordById("12345", "$2a$10$newhash");

        mockMvc.perform(put("/inner/user/12345/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mallUserService).updatePasswordById("12345", "$2a$10$newhash");
    }

    @Test
    void testUpdatePhone_ShouldSucceed() throws Exception {
        RemoteUserService.PhoneUpdateRequest request = new RemoteUserService.PhoneUpdateRequest();
        request.setNewPhone("13900139000");
        request.setNewPhoneHash("newPhoneHash456");

        doNothing().when(mallUserService).updatePhoneById("12345", "13900139000", "newPhoneHash456");

        mockMvc.perform(put("/inner/user/12345/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mallUserService).updatePhoneById("12345", "13900139000", "newPhoneHash456");
    }

    @Test
    void testDeactivateAccount_ShouldCallUpdateUserStatusById() throws Exception {
        doNothing().when(mallUserService).updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));

        mockMvc.perform(delete("/inner/user/12345/account"))
                .andExpect(status().isOk());

        verify(mallUserService).updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));
    }
}
