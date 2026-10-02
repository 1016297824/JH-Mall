package com.mall.order.controller;

import static com.mall.common.constant.HeaderConstants.IDEMPOTENT_KEY;
import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import com.mall.common.DTO.MallResult;
import com.mall.order.VO.OrderVO;
import com.mall.order.dto.request.CreateOrderRequest;
import com.mall.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端订单控制器
 *
 * <p>对应设计文档 §2.2 端点 6~11。</p>
 *
 * <p><b>与设计文档的两处差异</b>：</p>
 * <ol>
 *   <li>路径变量用 {@code {orderNo}} 而非 {@code {id}}——订单对外以订单号标识，
 *       且 {@code uk_order_no} 有唯一索引，Service 层也按订单号查询。</li>
 *   <li><b>未实现</b> {@code DELETE /orders/{id}}（设计文档端点 11）。
 *       订单是财务凭证，不应由 C 端用户删除；如需「隐藏已取消订单」
 *       应另设用户可见性字段，而非物理/逻辑删除。如确有需要请告知。</li>
 * </ol>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@RestController
@RequestMapping("/api/order/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param idempotentKey 幂等键，请求头 {@code Idempotent-Key}，由前端生成 UUID
     * @param req           下单请求
     * @param request       HTTP 请求
     * @return 订单号
     */
    @PostMapping
    public MallResult<String> createOrder(
            @RequestHeader(value = IDEMPOTENT_KEY, required = false) String idempotentKey,
            @RequestBody CreateOrderRequest req,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(orderService.createOrder(userId, idempotentKey, req));
    }

    /**
     * 查询我的订单列表
     *
     * @param status 订单状态过滤，可空
     * @param page   页码，从 1 开始
     * @param size   每页条数，服务端上限 50
     * @param request HTTP 请求
     * @return 订单列表
     */
    @GetMapping
    public MallResult<List<OrderVO>> listOrders(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(orderService.listOrders(userId, status, page, size));
    }

    /**
     * 查询订单详情
     *
     * @param orderNo 订单号
     * @param request HTTP 请求
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    public MallResult<OrderVO> getDetail(@PathVariable("orderNo") String orderNo,
                                          HttpServletRequest request) {
        Long userId = currentUserId(request);
        return MallResult.success(orderService.getDetail(userId, orderNo));
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @param request HTTP 请求
     * @return 空
     */
    @PostMapping("/{orderNo}/cancellation")
    public MallResult<Void> cancelOrder(@PathVariable("orderNo") String orderNo,
                                        HttpServletRequest request) {
        Long userId = currentUserId(request);
        orderService.cancelOrder(userId, orderNo);
        return MallResult.success(null);
    }

    /**
     * 确认收货
     *
     * @param orderNo 订单号
     * @param request HTTP 请求
     * @return 空
     */
    @PutMapping("/{orderNo}/receipt")
    public MallResult<Void> confirmReceipt(@PathVariable("orderNo") String orderNo,
                                           HttpServletRequest request) {
        Long userId = currentUserId(request);
        orderService.confirmReceipt(userId, orderNo);
        return MallResult.success(null);
    }

    private Long currentUserId(HttpServletRequest request) {
        return Long.parseLong(request.getHeader(X_USER_ID));
    }
}