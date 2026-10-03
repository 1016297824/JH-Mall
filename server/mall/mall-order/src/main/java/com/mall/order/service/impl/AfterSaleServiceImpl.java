package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.AfterSaleStatusEnum;
import com.mall.common.enums.order.AfterSaleTypeEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallAfterSaleDO;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.DO.MallOrderItemDO;
import com.mall.order.VO.AfterSaleVO;
import com.mall.order.config.MallOrderConfigProperties;
import com.mall.order.convert.response.AfterSaleConvert;
import com.mall.order.dto.request.SubmitAfterSaleRequest;
import com.mall.order.infrastructure.feign.RemotePaymentAdapter;
import com.mall.order.infrastructure.feign.RemoteProductAdapter;
import com.mall.order.mapper.MallAfterSaleMapper;
import com.mall.order.mapper.MallOrderItemMapper;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.service.AfterSaleService;
import com.mall.order.statemachine.OrderEventEnum;
import com.mall.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 售后服务实现
 *
 * <p>对应设计文档 §8。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private static final DateTimeFormatter NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MallAfterSaleMapper afterSaleMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final MallOrderMapper orderMapper;
    private final OrderStateMachine stateMachine;
    private final RemotePaymentAdapter paymentAdapter;
    private final RemoteProductAdapter productAdapter;
    private final MallOrderConfigProperties config;

    // ═══════════════════════════════════════════════════════════
    // 用户侧
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(Long userId, SubmitAfterSaleRequest req) {
        if (req == null || req.getOrderNo() == null || req.getAfterSaleType() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }

        MallOrderDO order = orderMapper.selectByOrderNo(req.getOrderNo());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 状态机判定该订单当前状态是否允许此类型售后
        stateMachine.transition(order, resolveRefundEvent(req.getAfterSaleType()));

        long amount = req.getAmount() == null ? nvl(order.getPayAmount()) : req.getAmount();
        if (amount <= 0 || amount > nvl(order.getPayAmount())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        MallAfterSaleDO afterSale = new MallAfterSaleDO();
        afterSale.setAfterSaleNo(generateAfterSaleNo());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderItemId(req.getOrderItemId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleType(req.getAfterSaleType());
        afterSale.setReason(req.getReason());
        afterSale.setAmount(amount);
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PENDING.getCode());
        afterSale.setApplyTime(LocalDateTime.now());
        afterSale.setIsDeleted(0);
        afterSale.setCreateTime(LocalDateTime.now());
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.insert(afterSale);

        // 自动审核：仅退款且金额未超阈值（§8.2）
        if (req.getAfterSaleType() == AfterSaleTypeEnum.REFUND_ONLY.getCode()
                && amount <= config.getAutoApproveThreshold()) {
            log.info("售后单自动审核通过: afterSaleNo={}, amount={}", afterSale.getAfterSaleNo(), amount);
            afterSaleMapper.updateStatus(afterSale.getId(), AfterSaleStatusEnum.APPROVED.getCode(),
                    LocalDateTime.now(), "系统自动审核通过");
            invokeRefund(order, afterSale);
        }

        log.info("售后申请已提交: userId={}, afterSaleNo={}, type={}", userId,
                afterSale.getAfterSaleNo(), req.getAfterSaleType());
        return afterSale.getAfterSaleNo();
    }

    @Override
    public List<AfterSaleVO> list(Long userId, String orderNo) {
        List<MallAfterSaleDO> list;
        String actualOrderNo = orderNo;

        if (orderNo != null) {
            MallOrderDO order = orderMapper.selectByOrderNo(orderNo);
            if (order == null || !userId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            actualOrderNo = order.getOrderNo();
            list = afterSaleMapper.selectByOrderId(order.getId());
        } else {
            list = afterSaleMapper.selectList(new LambdaQueryWrapper<MallAfterSaleDO>()
                    .eq(MallAfterSaleDO::getUserId, userId)
                    .eq(MallAfterSaleDO::getIsDeleted, 0)
                    .orderByDesc(MallAfterSaleDO::getApplyTime));
            actualOrderNo = null;
        }

        final String orderNoForView = actualOrderNo;
        return list.stream()
                .map(a -> AfterSaleConvert.toVO(a, orderNoForView))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════
    // 管理端
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long afterSaleId, String remark) {
        MallAfterSaleDO afterSale = requireAfterSale(afterSaleId);
        MallOrderDO order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 订单推进到退款中：状态机校验 + 乐观锁落库
        Integer originStatus = order.getOrderStatus();
        Integer version = order.getVersion();
        stateMachine.transition(order, resolveRefundEvent(afterSale.getAfterSaleType()));
        int affected = orderMapper.updateStatusCas(order.getOrderNo(), order.getOrderStatus(),
                originStatus, version, order.getPreRefundStatus());
        if (affected == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        afterSaleMapper.updateStatus(afterSaleId, AfterSaleStatusEnum.APPROVED.getCode(),
                LocalDateTime.now(), remark);
        invokeRefund(order, afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long afterSaleId, String remark) {
        requireAfterSale(afterSaleId);
        afterSaleMapper.updateStatus(afterSaleId, AfterSaleStatusEnum.REJECTED.getCode(),
                LocalDateTime.now(), remark);
        log.info("售后单已驳回: afterSaleId={}", afterSaleId);
    }

    // ═══════════════════════════════════════════════════════════
    // MQ 回调
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundCallback(String afterSaleNo, Long refundAmount) {
        MallAfterSaleDO afterSale = afterSaleMapper.selectByAfterSaleNo(afterSaleNo);
        if (afterSale == null) {
            log.warn("退款回调但售后单不存在: afterSaleNo={}", afterSaleNo);
            return;
        }
        // 幂等：已完成的售后单重复回调直接跳过
        if (afterSale.getAfterSaleStatus() == AfterSaleStatusEnum.COMPLETED.getCode()) {
            log.info("售后单已完成，忽略重复退款回调: afterSaleNo={}", afterSaleNo);
            return;
        }

        afterSaleMapper.updateStatus(afterSale.getId(), AfterSaleStatusEnum.COMPLETED.getCode(),
                LocalDateTime.now(), "退款完成");

        // 退货退款需回补库存；仅退款不回补（§8.1）
        if (afterSale.getAfterSaleType() == AfterSaleTypeEnum.RETURN_REFUND.getCode()
                && afterSale.getOrderItemId() != null) {
            MallOrderItemDO item = orderItemMapper.selectById(afterSale.getOrderItemId());
            if (item != null && item.getSkuId() != null && item.getQuantity() != null) {
                // 传 afterSaleNo 作幂等键：restock 成功后本地事务若回滚，MQ 重投不会重复回补
                productAdapter.restock(item.getSkuId(), item.getQuantity(), afterSaleNo);
                log.info("退货退款已回补库存: afterSaleNo={}, skuId={}, qty={}",
                        afterSaleNo, item.getSkuId(), item.getQuantity());
            } else {
                log.error("【需人工介入】退货退款但订单项缺失，无法回补库存: afterSaleNo={}", afterSaleNo);
            }
        }
        // 售后单完成 → 订单同步推进到 REFUNDED（此前只改售后单，订单永久停在 REFUNDING）
        advanceOrderToRefunded(afterSale);

        log.info("退款回调处理完成: afterSaleNo={}, refundAmount={}", afterSaleNo, refundAmount);
    }

    /**
     * 售后完成后推进订单到 REFUNDED（状态机 {@code REFUNDING --REFUND_SUCCESS--> REFUNDED}）
     *
     * <p>订单不在 REFUNDING（重复回调、已被其他流程处理）时只记 warn 不抛异常——
     * 售后单已置 COMPLETED 是既成事实，不能因此回滚。</p>
     *
     * @param afterSale 售后单
     */
    private void advanceOrderToRefunded(MallAfterSaleDO afterSale) {
        MallOrderDO order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null) {
            log.error("【需人工介入】售后完成但订单不存在: afterSaleNo={}, orderId={}",
                    afterSale.getAfterSaleNo(), afterSale.getOrderId());
            return;
        }
        try {
            Integer originStatus = order.getOrderStatus();
            Integer version = order.getVersion();
            stateMachine.transition(order, OrderEventEnum.REFUND_SUCCESS);
            int affected = orderMapper.updateStatusCas(order.getOrderNo(), order.getOrderStatus(),
                    originStatus, version, order.getPreRefundStatus());
            if (affected == 0) {
                log.error("【需人工介入】退款完成推进订单状态影响 0 行: orderNo={}", order.getOrderNo());
            }
        } catch (BusinessException e) {
            log.warn("售后完成但订单状态未推进（可能已被其他流程处理）: orderNo={}, status={}",
                    order.getOrderNo(), order.getOrderStatus());
        }
    }

    @Override
    public void refundFailedCallback(String afterSaleNo) {
        MallAfterSaleDO afterSale = afterSaleMapper.selectByAfterSaleNo(afterSaleNo);
        if (afterSale == null) {
            return;
        }
        // AfterSaleStatusEnum 没有 FAILED 值（设计文档 §8.4 要求置 FAILED）。
        // 保持 REFUNDING 并打「需人工介入」日志，不擅自置 CLOSED 以免丢失待处理语义。
        log.error("【需人工介入】退款失败，售后单停留 REFUNDING: afterSaleNo={}, afterSaleId={}",
                afterSaleNo, afterSale.getId());
    }

    // ═══════════════════════════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════════════════════════

    /**
     * 调起退款
     *
     * <p>失败不回滚售后单状态——审核通过是既成事实，退款失败需人工重试，
     * 不能把订单退回未审核态。失败仅记录 error 日志。</p>
     */
    private void invokeRefund(MallOrderDO order, MallAfterSaleDO afterSale) {
        try {
            RefundResultDTO result = paymentAdapter.refundByOrderNo(
                    order.getOrderNo(), afterSale.getAmount(), afterSale.getAfterSaleNo());
            if (result == null) {
                log.error("【需人工介入】退款调用返回空: afterSaleNo={}", afterSale.getAfterSaleNo());
            }
        } catch (Exception e) {
            log.error("【需人工介入】退款调用失败: afterSaleNo={}", afterSale.getAfterSaleNo(), e);
        }
    }

    /**
     * 售后类型 → 状态机事件
     *
     * <p>状态机自行判定当前订单状态是否允许该事件，不允许则抛 A0703。</p>
     */
    private OrderEventEnum resolveRefundEvent(Integer afterSaleType) {
        if (AfterSaleTypeEnum.REFUND_ONLY.getCode() == afterSaleType) {
            return OrderEventEnum.REFUND_ONLY;
        }
        if (AfterSaleTypeEnum.RETURN_REFUND.getCode() == afterSaleType) {
            return OrderEventEnum.RETURN_REFUND;
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID);
    }

    private MallAfterSaleDO requireAfterSale(Long afterSaleId) {
        MallAfterSaleDO afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return afterSale;
    }

    /**
     * 生成售后单号：AS + 毫秒时间戳(17) + 4 位随机，共 23 位
     */
    private String generateAfterSaleNo() {
        String ts = LocalDateTime.now().format(NO_FORMATTER);
        return "AS" + ts + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}