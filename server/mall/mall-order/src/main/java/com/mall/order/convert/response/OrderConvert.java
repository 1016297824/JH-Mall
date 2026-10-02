package com.mall.order.convert.response;

import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.DO.MallOrderItemDO;
import com.mall.order.VO.OrderItemVO;
import com.mall.order.VO.OrderVO;
import com.mall.order.statemachine.OrderEventEnum;
import com.mall.order.statemachine.OrderStateMachine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单转换器
 *
 * <p>静态方法，无状态。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class OrderConvert {

    /** 状态码 → 枚举。不使用 values()[code]，那依赖枚举声明顺序，极脆弱 */
    private static final Map<Integer, OrderStatusEnum> STATUS_MAP = new HashMap<>();

    static {
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            STATUS_MAP.put(status.getCode(), status);
        }
    }

    private OrderConvert() {
    }

    /**
     * 按状态码取枚举
     *
     * @param code 状态码
     * @return 枚举；非法码返回 null
     */
    public static OrderStatusEnum statusOf(Integer code) {
        return code == null ? null : STATUS_MAP.get(code);
    }

    /**
     * 订单项转换
     */
    public static OrderItemVO toItemVO(MallOrderItemDO item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setSpuId(item.getSpuId());
        vo.setSkuId(item.getSkuId());
        vo.setSkuCode(item.getSkuCode());
        vo.setSkuName(item.getSkuName());
        vo.setSpuName(item.getSpuName());
        vo.setMainImage(item.getMainImage());
        vo.setAttrsJson(item.getAttrsJson());
        vo.setQuantity(item.getQuantity());
        vo.setPrice(item.getPrice());
        vo.setTotalPrice(item.getTotalPrice());
        return vo;
    }

    /**
     * 订单转换（不含订单项）
     *
     * @param order 订单
     * @return 视图对象
     */
    public static OrderVO toVO(MallOrderDO order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayExpireTime(order.getPayExpireTime());
        vo.setCancelType(order.getCancelType());
        vo.setCancelReason(order.getCancelReason());
        vo.setLogisticsCompany(order.getLogisticsCompany());
        vo.setLogisticsNo(order.getLogisticsNo());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        OrderStatusEnum status = statusOf(order.getOrderStatus());
        vo.setOrderStatusDesc(status == null ? null : status.getDescription());
        return vo;
    }

    /**
     * 订单转换（含订单项与可执行操作）
     *
     * @param order        订单
     * @param items        订单项
     * @param stateMachine 状态机，用于推导当前可执行操作
     * @return 视图对象
     */
    public static OrderVO toVO(MallOrderDO order, List<MallOrderItemDO> items,
                               OrderStateMachine stateMachine) {
        OrderVO vo = toVO(order);
        vo.setItems(items.stream().map(OrderConvert::toItemVO).toList());
        vo.setActions(resolveActions(order, stateMachine));
        return vo;
    }

    /**
     * 推导当前状态下用户可触发的事件名
     *
     * <p>供前端决定按钮可见性，避免展示非法操作入口。
     * 仅为 UI 提示，后端仍会二次校验。</p>
     */
    private static List<String> resolveActions(MallOrderDO order, OrderStateMachine stateMachine) {
        OrderStatusEnum current = statusOf(order.getOrderStatus());
        if (current == null) {
            return List.of();
        }
        return Arrays.stream(OrderEventEnum.values())
                .filter(event -> stateMachine.canTransit(current, event))
                .filter(OrderConvert::isUserTriggered)
                .map(Enum::name)
                .toList();
    }

    /**
     * 是否为用户可触发的事件
     *
     * <p>支付回调、物流回调、超时任务、售后均由系统触发，不暴露给用户操作。</p>
     */
    private static boolean isUserTriggered(OrderEventEnum event) {
        return switch (event) {
            case USER_CANCEL, CONFIRM_RECEIPT -> true;
            default -> false;
        };
    }
}