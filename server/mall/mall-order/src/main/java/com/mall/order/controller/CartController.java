package com.mall.order.controller;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.common.DTO.MallResult;
import com.mall.order.VO.CartVO;
import com.mall.order.dto.request.AddCartRequest;
import com.mall.order.dto.request.UpdateCartRequest;
import com.mall.order.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端购物车控制器
 *
 * <p>对应设计文档 §2.2 端点 1~5，另增 6（修改选中状态）。
 * 用户身份由网关 MallAuthFilter 校验后写入 {@code X-User-Id} 请求头。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/order/cart")
@RequiredArgsConstructor
public class CartController {

    /** 购物车服务 */
    private final CartService cartService;

    /**
     * 查询购物车
     *
     * @param request HTTP 请求
     * @return 购物车项列表
     */
    @GetMapping("/items")
    public MallResult<List<CartVO>> listItems(HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(cartService.listCart(userId));
    }

    /**
     * 加入购物车
     *
     * @param req     加购请求
     * @param request HTTP 请求
     * @return 加购后的购物车项
     */
    @PostMapping("/items")
    public MallResult<CartVO> addItem(@RequestBody AddCartRequest req, HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(cartService.addItem(userId, req));
    }

    /**
     * 修改购物车项数量
     *
     * @param id      购物车项 ID
     * @param req     更新请求（取 quantity）
     * @param request HTTP 请求
     * @return 空
     */
    @PutMapping("/items/{id}")
    public MallResult<Void> updateQuantity(@PathVariable("id") Long id,
                                           @RequestBody UpdateCartRequest req,
                                           HttpServletRequest request) {
        Long userId = currentUserId(request);
        cartService.updateQuantity(userId, id, req);
        return MallResult.success(null);
    }

    /**
     * 修改选中状态
     *
     * @param id      购物车项 ID
     * @param req     更新请求（取 isSelected）
     * @param request HTTP 请求
     * @return 空
     */
    @PutMapping("/items/{id}/selected")
    public MallResult<Void> updateSelected(@PathVariable("id") Long id,
                                           @RequestBody UpdateCartRequest req,
                                           HttpServletRequest request) {
        Long userId = currentUserId(request);
        cartService.updateSelected(userId, id, req.getIsSelected());
        return MallResult.success(null);
    }

    /**
     * 删除购物车项
     *
     * @param id      购物车项 ID
     * @param request HTTP 请求
     * @return 空
     */
    @DeleteMapping("/items/{id}")
    public MallResult<Void> removeItem(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        cartService.removeItem(userId, id);
        return MallResult.success(null);
    }

    /**
     * 清空购物车
     *
     * @param request HTTP 请求
     * @return 空
     */
    @DeleteMapping("/items")
    public MallResult<Void> clearCart(HttpServletRequest request) {
        Long userId = currentUserId(request);
        cartService.clearCart(userId);
        return MallResult.success(null);
    }

    /**
     * 从请求头取当前用户 ID
     */
    private Long currentUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader(X_USER_ID));
    }
}