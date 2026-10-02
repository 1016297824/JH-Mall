package com.mall.order.VO;

import com.mall.common.enums.order.OrderStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象
 *
 * <p>金额单位均为分。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class OrderVO {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单状态码，取值见 OrderStatusEnum */
    private Integer orderStatus;

    /** 订单状态中文描述 */
    private String orderStatusDesc;

    /** 商品总金额 */
    private Long totalAmount;

    /** 优惠总金额 */
    private Long discountAmount;

    /** 运费 */
    private Long freightAmount;

    /** 实付金额 */
    private Long payAmount;

    /** 支付过期时间；已过该时间仍为待支付则订单会被自动关闭 */
    private LocalDateTime payExpireTime;

    /** 取消类型：USER_CANCEL / PAY_TIMEOUT / FORCE_CANCEL */
    private String cancelType;

    /** 取消原因 */
    private String cancelReason;

    /** 物流公司 */
    private String logisticsCompany;

    /** 物流单号 */
    private String logisticsNo;

    /** 买家备注 */
    private String remark;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单项 */
    private List<OrderItemVO> items;

    /**
     * 当前用户可执行的操作
     *
     * <p>由 {@code OrderStateMachine.canTransit} 推导，供前端决定按钮可见性，
     * 避免展示非法操作入口。前端仍需处理后端拒绝的情况。</p>
     */
    private List<String> actions;

    /** 是否处于待支付状态（前端据此展示倒计时） */
    public boolean isWaitPay() {
        return orderStatus != null && orderStatus == OrderStatusEnum.WAIT_PAY.getCode();
    }
}