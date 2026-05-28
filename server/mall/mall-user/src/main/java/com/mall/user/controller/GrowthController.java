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
import com.mall.user.service.IGrowthLogService;
import com.mall.user.service.IMemberService;
import com.mall.user.VO.GrowthRecordVO;
import com.mall.user.VO.GrowthVO;

/**
 * C 端成长值控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class GrowthController {

    private final IMemberService memberService;

    private final IGrowthLogService growthService;

    @GetMapping("/growth")
    public MallResult<GrowthVO> getGrowth(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getGrowth(Long.parseLong(userId)));
    }

    @GetMapping("/growth/records")
    public MallResult<IPage<GrowthRecordVO>> getGrowthRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizType,
            HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(growthService.getGrowthRecords(Long.parseLong(userId), bizType, page, size));
    }
}
