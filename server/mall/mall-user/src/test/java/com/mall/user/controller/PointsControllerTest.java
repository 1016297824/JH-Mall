package com.mall.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.service.IPointsService;
import com.mall.user.vo.PointsRecordVO;
import com.mall.user.vo.PointsVO;
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
 * PointsController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class PointsControllerTest {

    @Mock
    private IPointsService pointsService;

    @InjectMocks
    private PointsController controller;

    private MockMvc mockMvc;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getPoints_shouldReturnPoints() throws Exception {
        PointsVO mockVO = new PointsVO();
        mockVO.setTotalPoints(10000);
        mockVO.setAvailablePoints(8500);
        mockVO.setUsedPoints(1500);
        mockVO.setExpiredPoints(0);
        when(pointsService.getPoints(1L)).thenReturn(mockVO);

        mockMvc.perform(get("/api/user/points")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPoints").value(10000))
                .andExpect(jsonPath("$.data.availablePoints").value(8500))
                .andExpect(jsonPath("$.data.usedPoints").value(1500));
    }

    @Test
    void getPointsRecords_shouldReturnPagedRecords() throws Exception {
        PointsRecordVO record1 = new PointsRecordVO();
        record1.setId(1L);
        record1.setBizType("signin");
        record1.setBizTypeName("签到");
        record1.setChangeType(1);
        record1.setPoints(10);
        record1.setBeforePoints(100);
        record1.setAfterPoints(110);
        record1.setRemark("每日签到");
        record1.setCreateTime(new Date());

        IPage<PointsRecordVO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(record1));
        mockPage.setTotal(1);
        when(pointsService.getPointsRecords(eq(1L), eq("signin"), anyInt(), anyInt())).thenReturn(mockPage);

        mockMvc.perform(get("/api/user/points/records")
                        .header(X_USER_ID, "1")
                        .param("page", "1")
                        .param("size", "20")
                        .param("bizType", "signin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].bizType").value("signin"))
                .andExpect(jsonPath("$.data.records[0].bizTypeName").value("签到"))
                .andExpect(jsonPath("$.data.records[0].points").value(10))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getPointsRecords_shouldUseDefaultPagination() throws Exception {
        IPage<PointsRecordVO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);
        when(pointsService.getPointsRecords(eq(1L), isNull(), anyInt(), anyInt())).thenReturn(mockPage);

        mockMvc.perform(get("/api/user/points/records")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
