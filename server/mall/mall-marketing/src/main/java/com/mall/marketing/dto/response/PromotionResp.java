package com.mall.marketing.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 促销活动响应
 *
 * <p>C 端活动列表使用。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
public class PromotionResp {

    /** 活动 ID */
    private Long id;

    /** 活动名称 */
    private String promotionName;

    /** 活动类型，取值见 PromotionTypeEnum */
    private Integer promotionType;

    /** 活动类型描述，便于前端直接展示 */
    private String promotionTypeDesc;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态，取值见 PromotionStatusEnum */
    private Integer promotionStatus;

    /** 活动状态描述，便于前端直接展示 */
    private String promotionStatusDesc;

    /** 活动描述 */
    private String description;

    /** 活动 Banner 图 URL */
    private String bannerImage;

    /** 排序值，越小越靠前 */
    private Integer sortOrder;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
