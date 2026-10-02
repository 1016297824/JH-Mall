package com.mall.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 发起支付响应
 *
 * <p>返回给 C 端用于调起支付 SDK。{@code payParams} 的字段随渠道而异
 * （微信为 appId/timeStamp/nonceStr/package/signType/paySign，支付宝为表单或 tradeNo），
 * 故用 {@code Map} 承载以保持契约稳定。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayResultDTO {

    /** 支付单号 */
    private String paymentNo;

    /** 前端调起支付 SDK 所需的参数 */
    private Map<String, String> payParams;
}
