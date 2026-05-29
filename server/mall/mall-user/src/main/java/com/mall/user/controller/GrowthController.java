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

    /** 会员服务 */
    private final IMemberService memberService;

    /** 成长值流水服务 */
    private final IGrowthLogService growthService;

    /**
     * 查询成长值
     *
     * @param request HTTP 请求
     * @return 成长值信息
     */
    @GetMapping("/growth")
    public MallResult<GrowthVO> getGrowth(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getGrowth(Long.parseLong(userId)));
    }

    /**
     * 分页查询成长值流水
     *
     * @param page    页码
     * @param size    每页数量
     * @param bizType 业务类型
     * @param request HTTP 请求
     * @return 分页结果
     */
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
