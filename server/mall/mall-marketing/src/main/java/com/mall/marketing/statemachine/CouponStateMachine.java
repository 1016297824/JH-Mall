package com.mall.marketing.statemachine;

import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.marketing.CouponRecordStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.DO.MallCouponRecordDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券记录状态机
 *
 * <p>券记录状态变更的<b>唯一入口</b>。约束与 {@code OrderStateMachine} 保持一致：</p>
 * <ul>
 *   <li>不操作 DB、不持有 Mapper、不管理事务 —— 只做转移合法性判断与前置条件校验</li>
 *   <li>只修改传入对象的内存态字段，落库由 Service 层 CAS UPDATE 完成</li>
 *   <li>无状态单例，Map 只读，{@link #transition} 不修改共享状态，线程安全</li>
 * </ul>
 *
 * <p>转移矩阵严格对应设计文档 {@code docs/design/14_mall-marketing详细设计.md} §3.3：</p>
 *
 * <table border="1">
 *   <caption>券记录状态转移矩阵</caption>
 *   <tr><th>当前状态</th><th>事件</th><th>目标状态</th><th>后置动作</th></tr>
 *   <tr><td>AVAILABLE</td><td>LOCK</td><td>LOCKED</td><td>记录 orderNo + lockTime</td></tr>
 *   <tr><td>LOCKED</td><td>PAY_SUCCESS</td><td>USED</td><td>记录 useTime</td></tr>
 *   <tr><td>LOCKED</td><td>ORDER_CANCEL</td><td>RELEASED</td><td>清除 orderNo + 记录 releaseTime</td></tr>
 *   <tr><td>AVAILABLE</td><td>EXPIRE</td><td>EXPIRED</td><td>—</td></tr>
 *   <tr><td>RELEASED</td><td>EXPIRE</td><td>EXPIRED</td><td>—</td></tr>
 * </table>
 *
 * <p><b>调用方注意</b>：{@code ORDER_CANCEL} 的后置动作会把 {@code orderNo} 置空，
 * 而落库的 CAS 语句需要 {@code orderNo} 作为归属条件。因此 Service 层必须
 * <b>先取出 orderNo 再调用 {@link #transition}</b>（与 mall-order 先缓存
 * {@code originStatus} 再转移的做法一致）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Component
public class CouponStateMachine {

    /** 状态码 → 枚举 */
    private static final Map<Integer, CouponRecordStatusEnum> STATUS_CODE_MAP = new HashMap<>();

    static {
        for (CouponRecordStatusEnum status : CouponRecordStatusEnum.values()) {
            STATUS_CODE_MAP.put(status.getCode(), status);
        }
    }

    /** 转移矩阵：当前状态 → 触发事件 → 转移定义 */
    private final Map<CouponRecordStatusEnum, Map<CouponEventEnum, CouponTransition>> transitions =
            new EnumMap<>(CouponRecordStatusEnum.class);

    public CouponStateMachine() {
        initTransitions();
    }

    /**
     * 执行状态转移
     *
     * @param record 当前券记录，状态与相关时间字段会被原地修改
     * @param event  触发事件
     * @return 目标状态
     * @throws BusinessException A0702 转移矩阵无匹配；A0612 前置条件不满足
     */
    public CouponRecordStatusEnum transition(MallCouponRecordDO record, CouponEventEnum event) {
        CouponRecordStatusEnum current = STATUS_CODE_MAP.get(record.getRecordStatus());
        if (current == null) {
            log.error("券记录状态码非法: recordStatus={}, couponRecordId={}",
                    record.getRecordStatus(), record.getId());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        CouponTransition transition = transitions
                .getOrDefault(current, Map.of())
                .get(event);

        if (transition == null) {
            log.warn("非法状态转移: {} --{}--> ?, couponRecordId={}", current, event, record.getId());
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        if (!transition.precondition().test(record)) {
            log.warn("前置条件不满足: {} --{}--> ?, couponRecordId={}, orderNo={}, expireTime={}",
                    current, event, record.getId(), record.getOrderNo(), record.getExpireTime());
            throw new BusinessException(ErrorCode.COUPON_CONDITION_NOT_MET);
        }

        CouponRecordStatusEnum target = transition.targetResolver().apply(record);
        transition.postAction().accept(record);
        record.setRecordStatus(target.getCode());
        record.setUpdateTime(LocalDateTime.now());

        log.info("券状态转移: {} --{}--> {}, couponRecordId={}, orderNo={}",
                current, event, target, record.getId(), record.getOrderNo());
        return target;
    }

    /**
     * 该状态下是否允许此事件
     *
     * <p>供领券列表 / 下单前置校验使用，避免展示不可用入口。</p>
     *
     * @param from  当前状态
     * @param event 事件
     * @return 允许返回 true
     */
    public boolean canTransit(CouponRecordStatusEnum from, CouponEventEnum event) {
        return transitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    /**
     * 券是否在有效期内
     *
     * @param record 券记录
     * @return 未过期返回 true
     */
    private boolean notExpired(MallCouponRecordDO record) {
        return record.getExpireTime() != null
                && record.getExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 券是否已到期
     *
     * @param record 券记录
     * @return 已过期返回 true
     */
    private boolean expired(MallCouponRecordDO record) {
        return record.getExpireTime() != null
                && record.getExpireTime().isBefore(LocalDateTime.now());
    }

    /**
     * 是否已关联订单（锁定 / 核销 / 释放的归属校验）
     *
     * @param record 券记录
     * @return 已关联订单返回 true
     */
    private boolean boundToOrder(MallCouponRecordDO record) {
        return record.getOrderNo() != null && !record.getOrderNo().isBlank();
    }

    /**
     * 初始化转移矩阵 —— 严格对应设计文档 §3.3
     */
    private void initTransitions() {

        // ── AVAILABLE ──
        // 前置：券未过期（过期的券不允许锁定，防止过期券被下单占用）
        put(CouponRecordStatusEnum.AVAILABLE, CouponEventEnum.LOCK,
                CouponTransition.to(CouponRecordStatusEnum.LOCKED,
                        this::notExpired,
                        record -> {
                            record.setLockTime(LocalDateTime.now());
                        }));
        // 前置：已到期才允许置过期
        put(CouponRecordStatusEnum.AVAILABLE, CouponEventEnum.EXPIRE,
                CouponTransition.to(CouponRecordStatusEnum.EXPIRED,
                        this::expired,
                        record -> { }));

        // ── LOCKED ──
        // 前置：必须已绑定订单号，否则无法确认核销/释放的对象
        put(CouponRecordStatusEnum.LOCKED, CouponEventEnum.PAY_SUCCESS,
                CouponTransition.to(CouponRecordStatusEnum.USED,
                        this::boundToOrder,
                        record -> record.setUseTime(LocalDateTime.now())));
        put(CouponRecordStatusEnum.LOCKED, CouponEventEnum.ORDER_CANCEL,
                CouponTransition.to(CouponRecordStatusEnum.RELEASED,
                        this::boundToOrder,
                        record -> {
                            record.setReleaseTime(LocalDateTime.now());
                            record.setOrderNo(null);
                        }));

        // ── RELEASED ──
        // 释放后的券在原有效期到期后自动过期（设计文档 §3.3）
        put(CouponRecordStatusEnum.RELEASED, CouponEventEnum.EXPIRE,
                CouponTransition.to(CouponRecordStatusEnum.EXPIRED,
                        this::expired,
                        record -> { }));
    }

    private void put(CouponRecordStatusEnum from, CouponEventEnum event, CouponTransition transition) {
        transitions
                .computeIfAbsent(from, k -> new EnumMap<>(CouponEventEnum.class))
                .put(event, transition);
    }
}
