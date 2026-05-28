package com.mall.user.controller;

import com.mall.user.service.ISignInService;
import com.mall.user.vo.SignInVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SignInController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class SignInControllerTest {

    @Mock
    private ISignInService signInService;

    @InjectMocks
    private SignInController controller;

    private MockMvc mockMvc;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void signIn_shouldReturnSignInResult() throws Exception {
        SignInVO mockVO = new SignInVO();
        mockVO.setTodayPoints(10);
        mockVO.setConsecutiveDays(7);
        mockVO.setSignInCalendar(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        when(signInService.signIn(1L)).thenReturn(mockVO);

        mockMvc.perform(post("/api/user/sign-in")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayPoints").value(10))
                .andExpect(jsonPath("$.data.consecutiveDays").value(7))
                .andExpect(jsonPath("$.data.signInCalendar[0]").value(1));
    }
}
