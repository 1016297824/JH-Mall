package com.mall.payment.vo;

import com.mall.common.enums.payment.PaymentStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * C 端支付单视图对象
 *
 * <p>金额字段保持 {@code Long} 单位<strong>分</strong>，由前端负责展示转换
 * （项目约定：后端不做金额单位换算）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
public class PaymentVO {

    /** 支付单号 */
    private String paymentNo;

    /** 关联订单号 */
    private String orderNo;

    /** 支付金额（单位：分） */
    private Long payAmount;

    /** 支付单状态码，取值见 PaymentStatusEnum */
    private Integer paymentStatus;

    /** 支付单状态文本，便于前端直接展示 */
    private String paymentStatusText;

    /** 支付成功时间 */
    private LocalDateTime paySuccessTime;

    /** 支付过期时间 */
    private LocalDateTime expireTime;

    /**
     * 按状态码填充展示文本
     *
     * @param status 支付单状态码
     */
    public void applyStatusText(Integer status) {
        if (status == null) {
            return;
        }
        for (PaymentStatusEnum statusEnum : PaymentStatusEnum.values()) {
            if (statusEnum.getCode() == status) {
                this.paymentStatusText = statusEnum.getDescription();
                return;
            }
        }
    }
}
