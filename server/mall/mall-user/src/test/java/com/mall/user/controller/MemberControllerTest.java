package com.mall.user.controller;

import com.mall.user.service.IMemberService;
import com.mall.user.VO.MemberLevelVO;
import com.mall.user.VO.MembershipVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MemberController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private IMemberService memberService;

    @InjectMocks
    private MembershipController controller;

    private MockMvc mockMvc;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getMembership_shouldReturnMembership() throws Exception {
        MemberLevelVO currentLevel = new MemberLevelVO();
        currentLevel.setLevelName("黄金会员");
        currentLevel.setIcon("https://example.com/gold.png");
        currentLevel.setLevelValue(2);

        MembershipVO mockVO = new MembershipVO();
        mockVO.setCurrentLevel(currentLevel);
        mockVO.setGrowth(1500);
        mockVO.setTotalGrowth(5000);
        mockVO.setBenefits(Arrays.asList("包邮", "专属客服", "双倍积分"));
        when(memberService.getMembership(1L)).thenReturn(mockVO);

        mockMvc.perform(get("/api/user/membership")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentLevel.levelName").value("黄金会员"))
                .andExpect(jsonPath("$.data.currentLevel.icon").value("https://example.com/gold.png"))
                .andExpect(jsonPath("$.data.growth").value(1500))
                .andExpect(jsonPath("$.data.totalGrowth").value(5000))
                .andExpect(jsonPath("$.data.benefits[0]").value("包邮"));
    }
}
