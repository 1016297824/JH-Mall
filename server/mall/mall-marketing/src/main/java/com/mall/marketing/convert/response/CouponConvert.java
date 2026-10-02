package com.mall.marketing.convert.response;

import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.dto.response.CouponDefResp;

import java.util.List;

/**
 * 优惠券定义转换器（DO → DTO）
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class CouponConvert {

    private CouponConvert() {
    }

    /**
     * 券定义 DO 转响应 DTO
     *
     * @param couponDO 券定义 DO，可为 null
     * @return 券定义响应，入参为 null 时返回 null
     */
    public static CouponDefResp toCouponDefResp(MallCouponDO couponDO) {
        if (couponDO == null) {
            return null;
        }
        CouponDefResp resp = new CouponDefResp();
        resp.setId(couponDO.getId());
        resp.setCouponName(couponDO.getCouponName());
        resp.setCouponType(couponDO.getCouponType());
        resp.setCouponTypeDesc(couponTypeDesc(couponDO.getCouponType()));
        resp.setFaceValue(couponDO.getFaceValue());
        resp.setDiscountRate(couponDO.getDiscountRate());
        resp.setDiscountLimit(couponDO.getDiscountLimit());
        resp.setMinOrderAmount(couponDO.getMinOrderAmount());
        resp.setTotalCount(couponDO.getTotalCount());
        resp.setRemainCount(couponDO.getRemainCount());
        resp.setPerUserLimit(couponDO.getPerUserLimit());
        resp.setUseStartTime(couponDO.getUseStartTime());
        resp.setUseEndTime(couponDO.getUseEndTime());
        resp.setCouponStatus(couponDO.getCouponStatus());
        resp.setCouponStatusDesc(couponStatusDesc(couponDO.getCouponStatus()));
        return resp;
    }

    /**
     * 券定义 DO 列表转响应列表
     *
     * @param couponDOList 券定义 DO 列表
     * @return 券定义响应列表
     */
    public static List<CouponDefResp> toCouponDefRespList(List<MallCouponDO> couponDOList) {
        return couponDOList.stream().map(CouponConvert::toCouponDefResp).toList();
    }

    /**
     * 券类型码转描述
     *
     * @param couponType 券类型码，可为 null
     * @return 类型描述，无法识别时返回空串
     */
    private static String couponTypeDesc(Integer couponType) {
        if (couponType == null) {
            return "";
        }
        for (CouponTypeEnum type : CouponTypeEnum.values()) {
            if (type.getCode() == couponType) {
                return type.getDescription();
            }
        }
        return "";
    }

    /**
     * 券状态码转描述
     *
     * @param couponStatus 券状态码，可为 null
     * @return 状态描述，无法识别时返回空串
     */
    private static String couponStatusDesc(Integer couponStatus) {
        if (couponStatus == null) {
            return "";
        }
        for (CouponStatusEnum status : CouponStatusEnum.values()) {
            if (status.getCode() == couponStatus) {
                return status.getDescription();
            }
        }
        return "";
    }
}
