package com.mall.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.service.IGrowthService;
import com.mall.user.vo.GrowthRecordVO;
import com.mall.user.vo.GrowthVO;
import com.mall.user.vo.MemberLevelVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GrowthController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class GrowthControllerTest {

    @Mock
    private IGrowthService memberService;

    @InjectMocks
    private GrowthController controller;

    private MockMvc mockMvc;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getGrowth_shouldReturnGrowth() throws Exception {
        MemberLevelVO currentLevel = new MemberLevelVO();
        currentLevel.setLevelName("黄金会员");
        currentLevel.setIcon("https://example.com/gold.png");
        currentLevel.setLevelValue(2);

        MemberLevelVO nextLevel = new MemberLevelVO();
        nextLevel.setLevelName("钻石会员");
        nextLevel.setIcon("https://example.com/diamond.png");
        nextLevel.setLevelValue(3);

        GrowthVO mockVO = new GrowthVO();
        mockVO.setGrowth(1500);
        mockVO.setTotalGrowth(5000);
        mockVO.setCurrentLevel(currentLevel);
        mockVO.setNextLevel(nextLevel);
        mockVO.setNeedGrowth(500);
        mockVO.setProgressPercent(75);
        when(memberService.getGrowth(1L)).thenReturn(mockVO);

        mockMvc.perform(get("/api/user/growth")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.growth").value(1500))
                .andExpect(jsonPath("$.data.totalGrowth").value(5000))
                .andExpect(jsonPath("$.data.currentLevel.levelName").value("黄金会员"))
                .andExpect(jsonPath("$.data.nextLevel.levelName").value("钻石会员"))
                .andExpect(jsonPath("$.data.needGrowth").value(500))
                .andExpect(jsonPath("$.data.progressPercent").value(75));
    }

    @Test
    void getGrowthRecords_shouldReturnPagedRecords() throws Exception {
        GrowthRecordVO record1 = new GrowthRecordVO();
        record1.setId(1L);
        record1.setBizType("order");
        record1.setBizTypeName("下单");
        record1.setChangeType(1);
        record1.setGrowth(100);
        record1.setBeforeGrowth(1400);
        record1.setAfterGrowth(1500);
        record1.setRemark("完成订单");
        record1.setCreateTime(new Date());

        IPage<GrowthRecordVO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(record1));
        mockPage.setTotal(1);
        when(memberService.getGrowthRecords(eq(1L), eq("order"), anyInt(), anyInt())).thenReturn(mockPage);

        mockMvc.perform(get("/api/user/growth/records")
                        .header(X_USER_ID, "1")
                        .param("page", "1")
                        .param("size", "20")
                        .param("bizType", "order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].bizType").value("order"))
                .andExpect(jsonPath("$.data.records[0].bizTypeName").value("下单"))
                .andExpect(jsonPath("$.data.records[0].growth").value(100))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getGrowthRecords_shouldUseDefaultPagination() throws Exception {
        IPage<GrowthRecordVO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);
        when(memberService.getGrowthRecords(eq(1L), isNull(), anyInt(), anyInt())).thenReturn(mockPage);

        mockMvc.perform(get("/api/user/growth/records")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
