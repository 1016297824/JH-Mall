package com.mall.marketing.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.enums.marketing.CouponTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.convert.request.CouponRequestConvert;
import com.mall.marketing.convert.response.CouponConvert;
import com.mall.marketing.dto.request.CreateCouponReq;
import com.mall.marketing.dto.request.UpdateCouponReq;
import com.mall.marketing.dto.response.CouponDefResp;
import com.mall.marketing.mapper.MallCouponMapper;
import com.mall.marketing.service.CouponDefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 优惠券定义服务实现
 *
 * <p>券模板的查询与维护，核心是「状态决定可改范围」：草稿态全字段可改、
 * 已发布态仅可改名、已结束/已废弃态不可改。详见设计文档
 * {@code docs/design/14_mall-marketing详细设计.md} §3.1。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDefServiceImpl implements CouponDefService {

    /** 折扣率下界（含），即最低 0.1 折 */
    private static final int DISCOUNT_RATE_MIN = 1;

    /** 折扣率上界（含），100 表示不打折，故为非法值 */
    private static final int DISCOUNT_RATE_MAX = 99;

    private final MallCouponMapper couponMapper;

    @Override
    public List<CouponDefResp> listAvailableCoupons(int limit) {
        List<MallCouponDO> couponList = couponMapper.selectAvailable(LocalDateTime.now(), limit);
        return CouponConvert.toCouponDefRespList(couponList);
    }

    @Override
    public Long createCouponDef(CreateCouponReq req) {
        // 必填缺失 → A0401
        if (isBlank(req.getCouponName())) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }
        if (req.getCouponType() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }
        if (req.getUseStartTime() == null || req.getUseEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }
        // 取值非法 → A0402
        validateFields(req.getCouponType(), req.getFaceValue(), req.getDiscountRate(),
                req.getTotalCount(), req.getPerUserLimit(),
                req.getUseStartTime(), req.getUseEndTime());

        // 新建一律落为草稿态，剩余量等于发行总量（默认值由转换器统一构建）
        MallCouponDO coupon = CouponRequestConvert.toCouponDO(req);
        couponMapper.insert(coupon);
        log.info("创建券定义成功: couponId={}, couponName={}", coupon.getId(), coupon.getCouponName());
        return coupon.getId();
    }

    @Override
    public void updateCouponDef(UpdateCouponReq req) {
        MallCouponDO existing = couponMapper.selectByIdNotDeleted(req.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Integer status = existing.getCouponStatus();
        if (Objects.equals(status, CouponStatusEnum.DRAFT.getCode())) {
            // 草稿态：全部字段可改。草稿不会被领取，整行更新无并发风险
            if (isBlank(req.getCouponName())) {
                throw new BusinessException(ErrorCode.PARAM_MISSING);
            }
            validateFields(req.getCouponType(), req.getFaceValue(), req.getDiscountRate(),
                    req.getTotalCount(), req.getPerUserLimit(),
                    req.getUseStartTime(), req.getUseEndTime());
            CouponRequestConvert.applyUpdate(req, existing);
            couponMapper.updateById(existing);
        } else if (Objects.equals(status, CouponStatusEnum.PUBLISHED.getCode())) {
            // 已发布态：仅允许改名称。必须走字段级定向更新——整行写回会用陈旧快照
            // 覆盖掉并发的 remain_count（改名与领券交叉执行会导致超发）
            if (isBlank(req.getCouponName())) {
                throw new BusinessException(ErrorCode.PARAM_MISSING);
            }
            couponMapper.updateNameById(existing.getId(), req.getCouponName());
        } else {
            // 已结束 / 已废弃：不允许修改
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_ERROR);
        }

        log.info("修改券定义成功: couponId={}, couponStatus={}", existing.getId(), existing.getCouponStatus());
    }

    @Override
    public void deleteCouponDef(Long id) {
        MallCouponDO existing = couponMapper.selectByIdNotDeleted(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (Objects.equals(existing.getCouponStatus(), CouponStatusEnum.DRAFT.getCode())) {
            // 草稿态尚无已领出记录，物理删除
            couponMapper.deleteById(id);
            log.info("物理删除草稿券定义: couponId={}", id);
            return;
        }

        // 其余状态置为已废弃，保证已领出的券仍可追溯到券定义。
        // 同样走字段级定向更新，避免整行写回覆盖并发的 remain_count
        couponMapper.updateStatusById(id, CouponStatusEnum.DISCARDED.getCode());
        log.info("废弃券定义: couponId={}", id);
    }

    /**
     * 校验券定义业务字段取值是否合法
     *
     * <p>调用前须已完成必填校验（缺失抛 A0401），本方法只负责取值规则，
     * 命中即抛 {@code A0402}。</p>
     *
     * @param couponType   券类型码，取值见 {@code CouponTypeEnum}
     * @param faceValue    优惠面值（单位：分）
     * @param discountRate 折扣率（百分比）
     * @param totalCount   发行总量
     * @param perUserLimit 每人限领数量
     * @param useStartTime 有效期开始时间
     * @param useEndTime   有效期截止时间
     * @throws BusinessException 取值非法时抛出 {@code PARAM_INVALID}
     */
    private void validateFields(Integer couponType, Long faceValue, Integer discountRate,
                                Integer totalCount, Integer perUserLimit,
                                LocalDateTime useStartTime, LocalDateTime useEndTime) {
        if (totalCount == null || totalCount <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }
        if (perUserLimit == null || perUserLimit <= 0 || perUserLimit > totalCount) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        // 折扣券用折扣率，满减券/无门槛券用面值
        if (Objects.equals(couponType, CouponTypeEnum.DISCOUNT.getCode())) {
            if (discountRate == null || discountRate < DISCOUNT_RATE_MIN || discountRate > DISCOUNT_RATE_MAX) {
                throw new BusinessException(ErrorCode.PARAM_INVALID);
            }
        } else if (Objects.equals(couponType, CouponTypeEnum.FULL_REDUCE.getCode())
                || Objects.equals(couponType, CouponTypeEnum.NO_THRESHOLD.getCode())) {
            if (faceValue == null || faceValue <= 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID);
            }
        }

        if (useStartTime != null && useEndTime != null && !useEndTime.isAfter(useStartTime)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }
    }

    /**
     * 判断字符串是否为空白
     *
     * @param value 待判断字符串，可为 null
     * @return true 表示 null、空串或全空白
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
