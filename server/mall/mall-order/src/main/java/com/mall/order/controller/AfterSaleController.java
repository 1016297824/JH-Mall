package com.mall.order.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.common.DTO.MallResult;
import com.mall.order.VO.AfterSaleVO;
import com.mall.order.dto.request.SubmitAfterSaleRequest;
import com.mall.order.service.AfterSaleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端售后控制器
 *
 * <p>对应设计文档 §2.2 端点 12~14。</p>
 *
 * <p>管理端审核端点（approve / reject）由 mall-admin 的代码生成器产物承担，
 * 不在本C 端控制器中暴露。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/order/after_sales")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    /**
     * 提交售后申请
     *
     * @param req     申请请求
     * @param request HTTP 请求
     * @return 售后单号
     */
    @PostMapping
    public MallResult<String> submit(@RequestBody SubmitAfterSaleRequest req,
                                     HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(afterSaleService.submit(userId, req));
    }

    /**
     * 查询售后列表
     *
     * @param orderNo 订单号，可空表示查全部
     * @param request HTTP 请求
     * @return 售后单列表
     */
    @GetMapping
    public MallResult<List<AfterSaleVO>> list(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(afterSaleService.list(userId, orderNo));
    }

    private Long currentUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader(X_USER_ID));
    }
}