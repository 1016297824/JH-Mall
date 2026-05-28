package com.mall.user.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.common.DTO.MallResult;
import com.mall.user.service.IPointsService;
import com.mall.user.vo.PointsRecordVO;
import com.mall.user.vo.PointsVO;

/**
 * C 端积分控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class PointsController {

    private final IPointsService pointsService;

    /**
     * 查询积分余额
     *
     * @param request HTTP 请求
     * @return 积分余额
     */
    @GetMapping("/points")
    public MallResult<PointsVO> getPoints(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(pointsService.getPoints(Long.parseLong(userId)));
    }

    /**
     * 分页查询积分流水
     *
     * @param page    页码
     * @param size    每页数量
     * @param bizType 业务类型
     * @param request HTTP 请求
     * @return 分页结果
     */
    @GetMapping("/points/records")
    public MallResult<IPage<PointsRecordVO>> getPointsRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType,
            HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(pointsService.getPointsRecords(Long.parseLong(userId), bizType, page, size));
    }
}
