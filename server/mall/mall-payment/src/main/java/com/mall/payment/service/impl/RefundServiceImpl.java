package com.mall.payment.service.impl;

import com.mall.api.feign.RemotePaymentService.PaymentStatusDTO;
import com.mall.api.feign.RemotePaymentService.RefundDTO;
import com.mall.api.feign.RemotePaymentService.RefundResultDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.payment.RefundStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.DO.MallRefundDO;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.channel.RefundResult;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.mapper.MallRefundMapper;
import com.mall.payment.service.RefundService;
import com.mall.payment.statemachine.PaymentEventEnum;
import com.mall.payment.statemachine.PaymentStateMachine;
import com.mall.payment.statemachine.RefundEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款服务实现
 *
 * <p>对应设计文档 {@code docs/design/13_mall-payment详细设计.md} §6 退款流程：
 * 幂等校验 → 支付单状态推进 → 渠道路由 → 落库。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    /** 退款单号前缀，与 DO 注释「REF + 雪花」一致 */
    private static final String REFUND_NO_PREFIX = "REF";

    /** 退款单号时间部分格式：毫秒时间戳（17 位） */
    private static final DateTimeFormatter REFUND_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 退款单号随机后缀上界（6 位） */
    private static final int REFUND_NO_RANDOM_BOUND = 1_000_000;

    private final MallRefundMapper refundMapper;

    private final MallPaymentMapper paymentMapper;

    private final MallPaymentChannelMapper channelMapper;

    private final PaymentChannelFactory channelFactory;

    private final PaymentStateMachine paymentStateMachine;

    /**
     * 创建退款单并调渠道退款
     *
     * <p>幂等键为 {@code afterSaleNo_channelCode}，命中已有退款单直接复用返回，
     * 既不新建也不重复调渠道。</p>
     *
     * @param refundDTO 退款请求（含支付单号与渠道编码）
     * @return 退款结果（含退款单号与退款状态）
     * @throws BusinessException 支付单不存在（A0501）、金额非法（A0602）、
     *                           支付单状态不允许（A0702）、渠道请求未送达（C0211）
     */
    @Override
    public RefundResultDTO createRefund(RefundDTO refundDTO) {
        String afterSaleNo = refundDTO.getAfterSaleNo();
        String channelCode = refundDTO.getChannelCode();

        // 幂等优先级最高：命中已有退款单则直接复用返回。
        // 此时不做任何渠道交互（连渠道配置都不必读）
        String idempotentKey = buildIdempotentKey(afterSaleNo, channelCode);
        MallRefundDO existing = refundMapper.selectByIdempotentKey(idempotentKey);
        if (existing != null) {
            log.info("退款幂等命中，复用已有退款单: idempotentKey={}, refundNo={}",
                    idempotentKey, existing.getRefundNo());
            return toResultDTO(existing);
        }

        // 解析支付单：不存在按资源不存在处理
        MallPaymentDO payment = paymentMapper.selectByPaymentNo(refundDTO.getPaymentNo());
        if (payment == null) {
            log.warn("发起退款失败，支付单不存在: paymentNo={}", refundDTO.getPaymentNo());
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 渠道配置仅在真正要发起退款时才读
        MallPaymentChannelDO channel = channelMapper.selectByChannelCode(channelCode);

        return executeRefund(payment, afterSaleNo, refundDTO.getRefundAmount(),
                channelCode, idempotentKey, channel);
    }

    /**
     * 售后审核通过后按订单号发起退款
     *
     * <p>先完成 {@code orderNo → 已支付单} 解析，再复用退款主流程。</p>
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额（单位：分）
     * @param afterSaleNo  售后单业务单号（非主键 id）
     * @return 退款结果（含退款单号与退款状态）
     * @throws BusinessException 订单下无已支付单（A0501）、金额非法（A0602）、
     *                           支付单状态不允许（A0702）、渠道请求未送达（C0211）
     */
    @Override
    public RefundResultDTO refundByOrderNo(String orderNo, Long refundAmount, String afterSaleNo) {
        // ① 解析订单下「实际支付成功」的支付单
        MallPaymentDO payment = paymentMapper.selectPaidByOrderNo(orderNo);
        if (payment == null) {
            log.warn("发起退款失败，订单下无已支付单: orderNo={}", orderNo);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // ② 复用主流程：幂等键与退款单的 after_sale_no 都直接用业务售后单号，
        // 这样退款成功回调带回的 afterSaleNo 才能被 mall-order 按业务单号查回售后单
        String channelCode = payment.getChannelCode();

        String idempotentKey = buildIdempotentKey(afterSaleNo, channelCode);
        MallRefundDO existing = refundMapper.selectByIdempotentKey(idempotentKey);
        if (existing != null) {
            log.info("退款幂等命中，复用已有退款单: idempotentKey={}, refundNo={}",
                    idempotentKey, existing.getRefundNo());
            return toResultDTO(existing);
        }

        // 渠道配置仅在真正要发起退款时才读
        MallPaymentChannelDO channel = channelMapper.selectByChannelCode(channelCode);

        return executeRefund(payment, afterSaleNo, refundAmount, channelCode, idempotentKey, channel);
    }

    /**
     * 查询支付单的可退款状态快照
     *
     * <p>累计已退款金额与超额校验同口径：{@code sumRefundedAmount} 统计
     * {@code SUCCESS} 与 {@code PROCESSING} 两种状态。退款是异步的，
     * 若只统计成功态，同一支付单的并发退款请求会各自看到「未超额」而超退。</p>
     *
     * @param paymentNo 支付单号
     * @return 支付状态快照，支付单不存在返回 {@code null}
     */
    @Override
    public PaymentStatusDTO getPaymentStatus(String paymentNo) {
        MallPaymentDO payment = paymentMapper.selectByPaymentNo(paymentNo);
        if (payment == null) {
            log.debug("查询支付单状态：支付单不存在, paymentNo={}", paymentNo);
            return null;
        }

        Long refundedAmount = refundMapper.sumRefundedAmount(payment.getId());
        return new PaymentStatusDTO(payment.getPaymentNo(), payment.getOrderNo(),
                payment.getPaymentStatus(), payment.getPayAmount(),
                refundedAmount == null ? 0L : refundedAmount);
    }

    /**
     * 执行退款主流程（幂等校验与支付单解析已完成）
     *
     * <p>顺序为「金额校验 → 支付单状态推进 → 调渠道 → 落库」：金额校验失败或渠道请求
     * 未送达时均不落退款单。</p>
     *
     * @param payment       已解析的支付单
     * @param afterSaleNo   售后单号
     * @param refundAmount  退款金额（单位：分）
     * @param channelCode   渠道编码
     * @param idempotentKey 幂等键，格式 {@code afterSaleNo_channelCode}
     * @param channel       渠道配置
     * @return 退款结果
     * @throws BusinessException 金额非法（A0602）、支付单状态不允许（A0702）、渠道请求未送达（C0211）
     */
    private RefundResultDTO executeRefund(MallPaymentDO payment, String afterSaleNo, Long refundAmount,
                                          String channelCode, String idempotentKey,
                                          MallPaymentChannelDO channel) {
        // ① 金额校验：本次非正或累计退款超额一律拒绝
        validateRefundAmount(payment, refundAmount);

        // ② 支付单状态推进 PAID → REFUNDING（UNPAID 等非法状态由状态机抛 A0702）
        paymentStateMachine.transition(payment, PaymentEventEnum.REFUND_START);

        // ③ 构建退款单（先不落库，待渠道请求送达后再插入）
        MallRefundDO refund = buildRefund(payment, afterSaleNo, refundAmount, channelCode, idempotentKey);

        // ④ 调渠道；请求未送达则抛错且不落退款单
        PaymentChannelAdapter adapter = channelFactory.getAdapter(channelCode);
        RefundResult channelResult = adapter.invokeRefund(payment, refund, channel);
        if (channelResult == null || !channelResult.isSuccess()) {
            log.warn("渠道发起退款失败: paymentNo={}, refundNo={}, reason={}",
                    payment.getPaymentNo(), refund.getRefundNo(),
                    channelResult == null ? "渠道无响应" : channelResult.getFailReason());
            throw new BusinessException(ErrorCode.REFUND_SERVICE_ERROR);
        }

        // ⑤ 渠道已受理：落退款单 + 支付单 CAS 推进 REFUNDING
        refundMapper.insert(refund);
        paymentMapper.markRefunding(payment.getPaymentNo(), payment.getVersion());

        // ⑥ 立即回填渠道退款单号：退款回调只携带渠道侧单号，
        //    不回填则处于 PROCESSING 的退款单在收到回调时将无法被定位
        refund.setChannelRefundNo(channelResult.getChannelRefundNo());
        refund.setChannelRefundStatus(channelResult.getChannelRefundStatus());
        refundMapper.updateChannelRefundNo(refund.getRefundNo(),
                channelResult.getChannelRefundNo(), channelResult.getChannelRefundStatus());

        // ⑦ 处理渠道即时终态；非终态保持处理中，等待异步回调推进
        applyChannelRefundStatus(refund, payment, channelResult);

        log.info("发起退款成功: paymentNo={}, refundNo={}, refundStatus={}",
                payment.getPaymentNo(), refund.getRefundNo(), refund.getRefundStatus());
        return toResultDTO(refund);
    }

    /**
     * 校验退款金额
     *
     * <p>本次退款必须为正；且「累计已退 + 本次」不得超过支付金额，恰好等于为允许边界。</p>
     *
     * @param payment      支付单
     * @param refundAmount 本次退款金额（单位：分）
     * @throws BusinessException 金额非正或超额（A0602）
     */
    private void validateRefundAmount(MallPaymentDO payment, Long refundAmount) {
        if (refundAmount == null || refundAmount <= 0L) {
            log.warn("退款金额非正: paymentNo={}, refundAmount={}", payment.getPaymentNo(), refundAmount);
            throw new BusinessException(ErrorCode.AMOUNT_EXCEED_LIMIT);
        }
        Long refunded = refundMapper.sumRefundedAmount(payment.getId());
        long alreadyRefunded = refunded == null ? 0L : refunded;
        if (alreadyRefunded + refundAmount > payment.getPayAmount()) {
            log.warn("退款金额超额: paymentNo={}, alreadyRefunded={}, refundAmount={}, payAmount={}",
                    payment.getPaymentNo(), alreadyRefunded, refundAmount, payment.getPayAmount());
            throw new BusinessException(ErrorCode.AMOUNT_EXCEED_LIMIT);
        }
    }

    /**
     * 构建待落库的退款单
     *
     * @param payment       支付单
     * @param afterSaleNo   售后单号
     * @param refundAmount  退款金额（单位：分）
     * @param channelCode   渠道编码
     * @param idempotentKey 幂等键
     * @return 退款单（初始状态 PROCESSING）
     */
    private MallRefundDO buildRefund(MallPaymentDO payment, String afterSaleNo, Long refundAmount,
                                     String channelCode, String idempotentKey) {
        MallRefundDO refund = new MallRefundDO();
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderNo(payment.getOrderNo());
        refund.setAfterSaleNo(afterSaleNo);
        refund.setUserId(payment.getUserId());
        refund.setRefundAmount(refundAmount);
        refund.setChannelCode(channelCode);
        refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
        refund.setIdempotentKey(idempotentKey);
        refund.setIsDeleted(0);
        refund.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        refund.setCreateTime(now);
        refund.setUpdateTime(now);
        return refund;
    }

    /**
     * 按渠道即时返回推进退款单与支付单状态
     *
     * <p>渠道同步返回 SUCCESS 时推进退款单 SUCCESS 并写回渠道退款单号、支付单 REFUNDED；
     * 返回 FAILED 时推进退款单 FAILED、支付单回退 PAID；其余（如 PROCESSING）保持处理中，
     * 等待异步回调推进终态。</p>
     *
     * @param refund        退款单
     * @param payment       支付单
     * @param channelResult 渠道退款结果
     */
    private void applyChannelRefundStatus(MallRefundDO refund, MallPaymentDO payment,
                                          RefundResult channelResult) {
        Integer refundStatus = channelResult.getRefundStatus();
        if (refundStatus == null) {
            return;
        }
        if (refundStatus == RefundStatusEnum.SUCCESS.getCode()) {
            // 退款单 PROCESSING → SUCCESS
            paymentStateMachine.refundTransition(refund, RefundEventEnum.REFUND_SUCCESS_CALLBACK);
            refundMapper.markSuccess(refund.getRefundNo(), channelResult.getChannelRefundNo(),
                    channelResult.getChannelRefundStatus(), refund.getVersion());
            // 支付单 REFUNDING → REFUNDED
            paymentStateMachine.transition(payment, PaymentEventEnum.REFUND_SUCCESS_CALLBACK);
            paymentMapper.markRefunded(payment.getPaymentNo(), payment.getVersion());
        } else if (refundStatus == RefundStatusEnum.FAILED.getCode()) {
            // 退款单 PROCESSING → FAILED
            paymentStateMachine.refundTransition(refund, RefundEventEnum.REFUND_FAIL_CALLBACK);
            refundMapper.markFailed(refund.getRefundNo(),
                    channelResult.getChannelRefundStatus(), refund.getVersion());
            // 支付单 REFUNDING → PAID，避免永久卡在退款中
            paymentStateMachine.transition(payment, PaymentEventEnum.REFUND_FAIL_CALLBACK);
            paymentMapper.revertToPaid(payment.getPaymentNo(), payment.getVersion());
        }
    }

    /**
     * 构造幂等键
     *
     * <p>格式 {@code afterSaleNo_channelCode}，与 DB 唯一约束一致。</p>
     *
     * @param afterSaleNo 售后单号
     * @param channelCode 渠道编码
     * @return 幂等键
     */
    private String buildIdempotentKey(String afterSaleNo, String channelCode) {
        return afterSaleNo + "_" + channelCode;
    }

    /**
     * 退款单 DO 转响应 DTO
     *
     * @param refund 退款单
     * @return 退款结果
     */
    private RefundResultDTO toResultDTO(MallRefundDO refund) {
        RefundResultDTO result = new RefundResultDTO();
        result.setRefundNo(refund.getRefundNo());
        result.setRefundStatus(refund.getRefundStatus());
        result.setChannelRefundNo(refund.getChannelRefundNo());
        return result;
    }

    /**
     * 生成退款单号
     *
     * <p>格式：{@code REF + 毫秒时间戳（17 位） + 6 位随机数}，由唯一索引保证唯一。</p>
     *
     * @return 退款单号
     */
    private String generateRefundNo() {
        return REFUND_NO_PREFIX + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(REFUND_NO_RANDOM_BOUND));
    }
}
