package com.mall.api.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * C 端订单服务 Feign 接口
 *
 * <p>提供给mall-payment（发起支付校验）、mall-marketing（校验订单状态）调用</p>
 *
 * <p>对应设计文档 {@code docs/design/07_mall-api契约层设计.md} §3.4</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@FeignClient(contextId = "mall-order", value = "mall-order")
public interface RemoteOrderService {

    /**
     * 查询订单当前状态与金额快照
     *
     * @param orderNo 订单号
     * @return 订单快照；订单不存在返回 null
     */
    @GetMapping("/inner/order/query")
    OrderDTO queryOrder(@RequestParam("orderNo") String orderNo);

    /**
     * 订单快照
     *
     * <p>{@code status} 为订单状态码，取值见 {@code mall-common} 的 {@code OrderStatusEnum}。
     * 金额单位为分。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class OrderDTO {

        /** 订单号 */
        private String orderNo;

        /** 用户 ID */
        private Long userId;

        /** 订单状态码，取值见 OrderStatusEnum */
        private Integer status;

        /** 实付金额（单位：分） */
        private Long payAmount;

        /** 支付过期时间，ISO-8601 字符串 */
        private String payExpireTime;

        /** 取消类型，未取消为 null */
        private String cancelType;
    }
}