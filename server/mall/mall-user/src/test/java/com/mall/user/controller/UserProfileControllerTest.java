package com.mall.user.controller;

import com.mall.user.dto.request.UpdateProfileRequest;
import com.mall.user.service.IUserProfileService;
import com.mall.user.vo.UserProfileVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserProfileController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private IUserProfileService userProfileService;

    @InjectMocks
    private UserProfileController controller;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        UserProfileVO mockVO = new UserProfileVO();
        mockVO.setUserId("1");
        mockVO.setNickname("测试用户");
        mockVO.setAvatar("https://example.com/avatar.png");
        mockVO.setGender(1);
        mockVO.setGenderName("男");
        mockVO.setMembershipLevel("黄金会员");
        when(userProfileService.getProfile(1L)).thenReturn(mockVO);

        mockMvc.perform(get("/api/user/profile")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                .andExpect(jsonPath("$.data.avatar").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.gender").value(1))
                .andExpect(jsonPath("$.data.genderName").value("男"))
                .andExpect(jsonPath("$.data.membershipLevel").value("黄金会员"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setAvatar("https://example.com/new-avatar.png");

        UserProfileVO mockVO = new UserProfileVO();
        mockVO.setUserId("1");
        mockVO.setNickname("新昵称");
        mockVO.setAvatar("https://example.com/new-avatar.png");
        when(userProfileService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(mockVO);

        mockMvc.perform(put("/api/user/profile")
                        .header(X_USER_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.avatar").value("https://example.com/new-avatar.png"));
    }
}
