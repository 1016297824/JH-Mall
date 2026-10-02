package com.mall.marketing.service.impl;

import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.enums.marketing.CouponStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponDO;
import com.mall.marketing.DO.MallCouponRecordDO;
import com.mall.marketing.config.MallMarketingConfigProperties;
import com.mall.marketing.dto.response.CouponRecordResp;
import com.mall.marketing.infrastructure.outbox.OutboxPublisher;
import com.mall.marketing.mapper.MallCouponMapper;
import com.mall.marketing.mapper.MallCouponRecordMapper;
import com.mall.marketing.service.CouponClaimService;
import com.mall.marketing.statemachine.CouponEventEnum;
import com.mall.marketing.statemachine.CouponStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户优惠券服务实现
 *
 * <p>覆盖券记录从领取到核销/释放的全生命周期。所有 {@code record_status} 变更均经
 * {@link CouponStateMachine#transition} 判定，本服务只负责前置校验、补偿与 CAS 落库。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponClaimServiceImpl implements CouponClaimService {

    /** 券编码前缀 */
    private static final String COUPON_CODE_PREFIX = "CPN";

    /** 券编码随机数上界（6 位） */
    private static final int COUPON_CODE_RANDOM_BOUND = 1_000_000;

    private final MallCouponMapper couponMapper;

    private final MallCouponRecordMapper couponRecordMapper;

    private final CouponStateMachine couponStateMachine;

    private final MallMarketingConfigProperties config;

    private final OutboxPublisher outboxPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long claimCoupon(Long userId, Long couponDefId) {
        // 1. 校验券定义可领：存在 + 已发布 + 未过有效期
        MallCouponDO coupon = couponMapper.selectByIdNotDeleted(couponDefId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (!Objects.equals(coupon.getCouponStatus(), CouponStatusEnum.PUBLISHED.getCode())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        if (coupon.getUseEndTime() == null || !coupon.getUseEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }

        // 2. 校验每人限领数量
        long claimed = couponRecordMapper.countClaimed(userId, couponDefId);
        if (coupon.getPerUserLimit() != null && claimed >= coupon.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.RESOURCE_EXISTS);
        }

        // 3. 乐观锁扣减库存：影响 0 行代表已领完或版本冲突
        int affected = couponMapper.decreaseRemainCount(couponDefId, coupon.getVersion());
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COUPON_DEPLETED);
        }

        // 4. 落券记录：面值与过期时间取券定义快照
        MallCouponRecordDO record = new MallCouponRecordDO();
        record.setCouponId(couponDefId);
        record.setUserId(userId);
        record.setCouponCode(generateCouponCode());
        record.setRecordStatus(CouponRecordStatusEnum.AVAILABLE.getCode());
        record.setFaceValue(coupon.getFaceValue());
        record.setExpireTime(coupon.getUseEndTime());
        record.setIsDeleted(0);
        record.setCreateTime(LocalDateTime.now());
        couponRecordMapper.insert(record);

        log.info("领券成功: userId={}, couponDefId={}, couponRecordId={}", userId, couponDefId, record.getId());
        return record.getId();
    }

    @Override
    public boolean lockCoupon(Long couponClaimId, String orderNo) {
        MallCouponRecordDO record = couponRecordMapper.selectByIdNotDeleted(couponClaimId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 状态机不感知 orderNo，需在转移前写入记录（转移动作自身不携带订单号）
        record.setOrderNo(orderNo);
        try {
            couponStateMachine.transition(record, CouponEventEnum.LOCK);
        } catch (BusinessException e) {
            // 已占用 / 已过期等不满足锁定条件的情形：按 boolean 契约返回 false，由调用方转 A0612
            log.warn("锁券失败: couponRecordId={}, orderNo={}, reason={}",
                    couponClaimId, orderNo, e.getMessage());
            return false;
        }

        // CAS 落库：影响 0 行说明并发竞争失败
        return couponRecordMapper.lockById(couponClaimId, orderNo) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(String orderNo) {
        List<MallCouponRecordDO> records =
                couponRecordMapper.selectByOrderNoAndStatus(orderNo, CouponRecordStatusEnum.LOCKED.getCode());
        if (records.isEmpty()) {
            log.info("券核销跳过（无锁定券，幂等）: orderNo={}", orderNo);
            return;
        }

        for (MallCouponRecordDO record : records) {
            couponStateMachine.transition(record, CouponEventEnum.PAY_SUCCESS);
            int affected = couponRecordMapper.markUsedById(record.getId(), orderNo);
            if (affected == 0) {
                log.warn("券核销落库失败（竞态）: couponRecordId={}, orderNo={}", record.getId(), orderNo);
                // CAS 未生效则券并未真正核销，不能留下核销事实
                continue;
            }
            publishCouponUsed(record, orderNo);
        }
        log.info("券核销完成: orderNo={}, 记录数={}", orderNo, records.size());
    }

    /**
     * 写券核销事实到 Outbox（与状态变更处于同一本地事务）
     *
     * <p>Payload 严格对应设计文档 §7.1：{@code couponRecordId, couponId, userId, orderNo,
     * faceValue（分）, useTime}。禁止直接序列化 DO，故用精简 Map。</p>
     *
     * @param record  已核销的券记录
     * @param orderNo 订单号
     */
    private void publishCouponUsed(MallCouponRecordDO record, String orderNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("couponRecordId", record.getId());
        payload.put("couponId", record.getCouponId());
        payload.put("userId", record.getUserId());
        payload.put("orderNo", orderNo);
        payload.put("faceValue", record.getFaceValue());
        payload.put("useTime", LocalDateTime.now());
        outboxPublisher.publish(MqTopicConstants.Coupon.USED, "CouponUsed",
                String.valueOf(record.getId()), payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(String orderNo) {
        List<MallCouponRecordDO> records =
                couponRecordMapper.selectByOrderNoAndStatus(orderNo, CouponRecordStatusEnum.LOCKED.getCode());
        if (records.isEmpty()) {
            log.info("券释放跳过（无锁定券，幂等）: orderNo={}", orderNo);
            return;
        }

        for (MallCouponRecordDO record : records) {
            Long couponId = record.getCouponId();
            // ORDER_CANCEL 的后置动作会清空 orderNo，必须先取出作为 CAS 落库的归属条件
            String recordOrderNo = record.getOrderNo();

            couponStateMachine.transition(record, CouponEventEnum.ORDER_CANCEL);

            int affected = couponRecordMapper.releaseById(record.getId(), recordOrderNo);
            if (affected > 0) {
                // 仅 CAS 真正成功才回补库存，避免重复投递导致重复回补
                couponMapper.increaseRemainCount(couponId);
            }
        }
        log.info("券释放完成: orderNo={}, 记录数={}", orderNo, records.size());
    }

    @Override
    public List<CouponRecordResp> listMyCoupons(Long userId, Integer recordStatus, int limit) {
        List<MallCouponRecordDO> records = couponRecordMapper.selectUserRecords(userId, recordStatus, limit);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量补券定义信息，避免逐条查库（N+1）
        Set<Long> couponIds = records.stream()
                .map(MallCouponRecordDO::getCouponId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MallCouponDO> couponMap = couponMapper.selectByIdsNotDeleted(couponIds).stream()
                .collect(Collectors.toMap(MallCouponDO::getId, Function.identity(), (first, second) -> first));

        List<CouponRecordResp> result = new ArrayList<>(records.size());
        for (MallCouponRecordDO record : records) {
            result.add(toCouponRecordResp(record, couponMap.get(record.getCouponId())));
        }
        return result;
    }

    @Override
    public int expireCoupons() {
        int batchSize = config.getCoupon().getExpireBatchSize();
        int affected = couponRecordMapper.expireBatch(batchSize);
        log.info("批量置过期完成: batchSize={}, affected={}", batchSize, affected);
        return affected;
    }

    @Override
    public boolean validateCoupon(Long couponClaimId, Long userId) {
        MallCouponRecordDO record = couponRecordMapper.selectByIdNotDeleted(couponClaimId);
        // 券不存在与非本人返回同一错误码，避免泄露券归属信息
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (record.getRecordStatus() == null
                || record.getRecordStatus() != CouponRecordStatusEnum.AVAILABLE.getCode()) {
            return false;
        }
        return record.getExpireTime() != null && record.getExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 生成全局唯一券编码
     *
     * <p>格式：{@code CPN + 当前毫秒时间戳 + 6 位随机数}。</p>
     *
     * @return 券编码
     */
    private String generateCouponCode() {
        return COUPON_CODE_PREFIX + System.currentTimeMillis()
                + String.format("%06d", ThreadLocalRandom.current().nextInt(COUPON_CODE_RANDOM_BOUND));
    }

    /**
     * 券记录 DO 转响应 DTO
     *
     * @param record 券记录
     * @param coupon 券定义，可能为 null（定义已被删除）
     * @return 券记录响应
     */
    private CouponRecordResp toCouponRecordResp(MallCouponRecordDO record, MallCouponDO coupon) {
        CouponRecordResp resp = new CouponRecordResp();
        resp.setId(record.getId());
        resp.setCouponId(record.getCouponId());
        resp.setCouponCode(record.getCouponCode());
        resp.setFaceValue(record.getFaceValue());
        resp.setRecordStatus(record.getRecordStatus());
        resp.setRecordStatusDesc(statusDesc(record.getRecordStatus()));
        resp.setOrderNo(record.getOrderNo());
        resp.setLockTime(record.getLockTime());
        resp.setUseTime(record.getUseTime());
        resp.setExpireTime(record.getExpireTime());
        if (coupon != null) {
            resp.setCouponName(coupon.getCouponName());
            resp.setCouponType(coupon.getCouponType());
            resp.setMinOrderAmount(coupon.getMinOrderAmount());
        }
        return resp;
    }

    /**
     * 券记录状态码转描述文案
     *
     * @param recordStatus 记录状态码，可能为 null
     * @return 状态描述，无法识别时返回空串
     */
    private String statusDesc(Integer recordStatus) {
        if (recordStatus == null) {
            return "";
        }
        for (CouponRecordStatusEnum status : CouponRecordStatusEnum.values()) {
            if (status.getCode() == recordStatus) {
                return status.getDescription();
            }
        }
        return "";
    }
}
