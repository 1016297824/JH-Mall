package com.mall.user.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.common.DTO.MallResult;
import com.mall.user.service.IMemberService;
import com.mall.user.VO.MembershipVO;

/**
 * C 端会员控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MembershipController {

    /** 会员服务 */
    private final IMemberService memberService;

    /**
     * 查询会员信息
     *
     * @param request HTTP 请求
     * @return 会员信息
     */
    @GetMapping("/membership")
    public MallResult<MembershipVO> getMembership(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(memberService.getMembership(Long.parseLong(userId)));
    }
}
