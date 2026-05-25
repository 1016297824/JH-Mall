package com.mall.user.controller.api;

import com.mall.api.enums.UserStatusEnum;
import com.mall.api.dto.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import com.mall.user.domain.MallUser;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private MallUser buildMockUser() {
        MallUser user = new MallUser();
        user.setId("12345");
        user.setPhone("13800138000");
        user.setPhoneHash("abc123hash");
        user.setPassword("$2a$10$encodedhash");
        user.setNickname("测试用户");
        user.setAvatar("https://example.com/avatar.png");
        user.setEmail("test@example.com");
        user.setEmailHash("email123hash");
        user.setGender("1");
        user.setUserStatus(String.valueOf(UserStatusEnum.NORMAL.getCode()));
        user.setRegisterType("phone");
        user.setRegisterIp("127.0.0.1");
        user.setIsPrivacyAgreed("1");
        return user;
    }

    @Test
    void testFindByPhone_ShouldReturnDTO() throws Exception {
        MallUser mockUser = buildMockUser();
        when(mallUserService.selectByPhone("13800138000")).thenReturn(mockUser);

        String responseBody = mockMvc.perform(get("/inner/user/phone/13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("12345"))
                .andExpect(jsonPath("$.phone").value("13800138000"))
                .andExpect(jsonPath("$.phoneHash").value("abc123hash"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.nickname").value("测试用户"))
                .andExpect(jsonPath("$.avatar").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.emailHash").value("email123hash"))
                .andExpect(jsonPath("$.gender").value("1"))
                .andExpect(jsonPath("$.userStatus").value(String.valueOf(UserStatusEnum.NORMAL.getCode())))
                .andExpect(jsonPath("$.registerType").value("phone"))
                .andExpect(jsonPath("$.registerIp").value("127.0.0.1"))
                .andExpect(jsonPath("$.privacyAgreed").value("1"))
                .andReturn().getResponse().getContentAsString();

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

        when(mallUserService.updatePasswordById("12345", "$2a$10$newhash")).thenReturn(1);

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

        when(mallUserService.updatePhoneById("12345", "13900139000", "newPhoneHash456")).thenReturn(1);

        mockMvc.perform(put("/inner/user/12345/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mallUserService).updatePhoneById("12345", "13900139000", "newPhoneHash456");
    }

    @Test
    void testDeactivateAccount_ShouldCallUpdateUserStatusById() throws Exception {
        when(mallUserService.updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()))).thenReturn(1);

        mockMvc.perform(delete("/inner/user/12345/account"))
                .andExpect(status().isOk());

        verify(mallUserService).updateUserStatusById("12345", String.valueOf(UserStatusEnum.DELETED.getCode()));
    }
}
