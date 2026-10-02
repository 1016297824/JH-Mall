package com.mall.marketing.convert.response;

import com.mall.common.enums.marketing.PromotionStatusEnum;
import com.mall.common.enums.marketing.PromotionTypeEnum;
import com.mall.marketing.DO.MallPromotionDO;
import com.mall.marketing.dto.response.PromotionResp;

import java.util.List;

/**
 * 促销活动转换器（DO → DTO）
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class PromotionConvert {

    private PromotionConvert() {
    }

    /**
     * 活动 DO 转响应 DTO
     *
     * @param promotionDO 活动 DO，可为 null
     * @return 活动响应，入参为 null 时返回 null
     */
    public static PromotionResp toPromotionResp(MallPromotionDO promotionDO) {
        if (promotionDO == null) {
            return null;
        }
        PromotionResp resp = new PromotionResp();
        resp.setId(promotionDO.getId());
        resp.setPromotionName(promotionDO.getPromotionName());
        resp.setPromotionType(promotionDO.getPromotionType());
        resp.setPromotionTypeDesc(promotionTypeDesc(promotionDO.getPromotionType()));
        resp.setStartTime(promotionDO.getStartTime());
        resp.setEndTime(promotionDO.getEndTime());
        resp.setPromotionStatus(promotionDO.getPromotionStatus());
        resp.setPromotionStatusDesc(promotionStatusDesc(promotionDO.getPromotionStatus()));
        resp.setDescription(promotionDO.getDescription());
        resp.setBannerImage(promotionDO.getBannerImage());
        resp.setSortOrder(promotionDO.getSortOrder());
        return resp;
    }

    /**
     * 活动 DO 列表转响应列表
     *
     * @param promotionDOList 活动 DO 列表
     * @return 活动响应列表
     */
    public static List<PromotionResp> toPromotionRespList(List<MallPromotionDO> promotionDOList) {
        return promotionDOList.stream().map(PromotionConvert::toPromotionResp).toList();
    }

    /**
     * 活动类型码转描述
     *
     * @param promotionType 活动类型码，可为 null
     * @return 类型描述，无法识别时返回空串
     */
    private static String promotionTypeDesc(Integer promotionType) {
        if (promotionType == null) {
            return "";
        }
        for (PromotionTypeEnum type : PromotionTypeEnum.values()) {
            if (type.getCode() == promotionType) {
                return type.getDescription();
            }
        }
        return "";
    }

    /**
     * 活动状态码转描述
     *
     * @param promotionStatus 活动状态码，可为 null
     * @return 状态描述，无法识别时返回空串
     */
    private static String promotionStatusDesc(Integer promotionStatus) {
        if (promotionStatus == null) {
            return "";
        }
        for (PromotionStatusEnum status : PromotionStatusEnum.values()) {
            if (status.getCode() == promotionStatus) {
                return status.getDescription();
            }
        }
        return "";
    }
}
