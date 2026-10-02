package com.mall.marketing.dto.request;

import com.mall.api.feign.RemoteMarketingService.CalculationReq.CalculationItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * C 端优惠试算请求
 *
 * <p><b>刻意不含 {@code userId}</b>：用户身份必须由网关下发的 {@code X-User-Id} 请求头决定，
 * 绝不能由请求体提供。否则「券归属校验」形同虚设——攻击者只要把 userId 填成受害者，
 * 再带上受害者的 {@code couponClaimId}，就能通过归属校验去抵扣别人的券（越权 / IDOR）。
 * 故 Controller 会从请求头取 userId 后再拼装内部契约对象 {@code CalculationReq}。</p>
 *
 * <p>本类字段是契约 DTO {@code CalculationReq} 的「去 userId」子集，
 * 商品明细直接复用契约的 {@code CalculationItem}，避免再维护一份平行类型。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class CalculationRequest {

    /** 商品明细（单价单位：分） */
    private List<CalculationItem> items;

    /** 指定使用的优惠券记录 ID，可选；不传则由服务端在用户可用券中择优 */
    private Long couponClaimId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
