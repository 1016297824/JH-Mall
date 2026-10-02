package com.mall.payment.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发起支付请求
 *
 * <p>⚠️ <b>刻意不包含 userId</b>：付款人身份只能来自网关透传的 {@code X-User-Id} 请求头，
 * 由 Controller 取出后单独传给 Service。若允许请求体携带 userId，
 * 攻击者可指定他人身份发起支付（与 mall-marketing 试算接口同一约束）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
public class PayRequestDTO {

    /** 订单号 */
    private String orderNo;

    /** 支付渠道编码，如 wechat / alipay */
    private String channelCode;

    /** 用户在该渠道的标识（微信 JSAPI / 小程序必需），H5 / Native 可为空 */
    private String openid;
}
