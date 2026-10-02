package com.mall.marketing.convert.request;

import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.dto.request.CreateCouponReq;
import com.mall.marketing.dto.request.UpdateCouponReq;

import java.time.LocalDateTime;

/**
 * 优惠券定义请求转换器（Request → DO）
 *
 * <p>只做字段搬运与建默认值，<b>不做</b>业务校验——校验规则集中在 Service 层，
 * 避免同一规则在两处维护。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class CouponRequestConvert {

    private CouponRequestConvert() {
    }

    /**
     * 创建请求转券定义 DO
     *
     * <p>建默认值：状态为「草稿」、剩余量等于发行总量、逻辑删除位 0、乐观锁版本 0。
     * 发布是独立操作，不在创建时完成。</p>
     *
     * @param req 创建请求
     * @return 券定义 DO
     */
    public static MallCouponDO toCouponDO(CreateCouponReq req) {
        MallCouponDO coupon = new MallCouponDO();
        coupon.setCouponName(req.getCouponName());
        coupon.setCouponType(req.getCouponType());
        coupon.setFaceValue(req.getFaceValue());
        coupon.setDiscountRate(req.getDiscountRate());
        coupon.setDiscountLimit(req.getDiscountLimit());
        coupon.setMinOrderAmount(req.getMinOrderAmount() == null ? 0L : req.getMinOrderAmount());
        coupon.setTotalCount(req.getTotalCount());
        coupon.setRemainCount(req.getTotalCount());
        coupon.setPerUserLimit(req.getPerUserLimit());
        coupon.setUseStartTime(req.getUseStartTime());
        coupon.setUseEndTime(req.getUseEndTime());
        coupon.setCouponStatus(CouponStatusEnum.DRAFT.getCode());
        coupon.setIsDeleted(0);
        coupon.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        return coupon;
    }

    /**
     * 把修改请求的可变字段应用到已存在的券定义上
     *
     * <p>不改变状态、不改变剩余量（剩余量只在领券/取消回补时变动）。</p>
     *
     * @param req    修改请求
     * @param target 待更新的券定义 DO，原地修改
     */
    public static void applyUpdate(UpdateCouponReq req, MallCouponDO target) {
        target.setCouponName(req.getCouponName());
        target.setCouponType(req.getCouponType());
        target.setFaceValue(req.getFaceValue());
        target.setDiscountRate(req.getDiscountRate());
        target.setDiscountLimit(req.getDiscountLimit());
        target.setMinOrderAmount(req.getMinOrderAmount() == null ? 0L : req.getMinOrderAmount());
        target.setTotalCount(req.getTotalCount());
        target.setPerUserLimit(req.getPerUserLimit());
        target.setUseStartTime(req.getUseStartTime());
        target.setUseEndTime(req.getUseEndTime());
        target.setUpdateTime(LocalDateTime.now());
    }
}
