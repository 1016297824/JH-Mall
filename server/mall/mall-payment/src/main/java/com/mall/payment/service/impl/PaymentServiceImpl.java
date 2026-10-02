package com.mall.payment.service.impl;

import com.mall.api.feign.RemoteOrderService.OrderDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.enums.payment.PaymentStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.payment.DO.MallPaymentChannelDO;
import com.mall.payment.DO.MallPaymentDO;
import com.mall.payment.config.MallPaymentConfigProperties;
import com.mall.payment.dto.request.PayRequestDTO;
import com.mall.payment.dto.response.PayResultDTO;
import com.mall.payment.infrastructure.channel.PayResult;
import com.mall.payment.infrastructure.channel.PaymentChannelAdapter;
import com.mall.payment.infrastructure.channel.PaymentChannelFactory;
import com.mall.payment.infrastructure.feign.RemoteOrderAdapter;
import com.mall.payment.mapper.MallPaymentChannelMapper;
import com.mall.payment.mapper.MallPaymentMapper;
import com.mall.payment.service.PaymentService;
import com.mall.payment.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现
 *
 * <p>对应设计文档 §4 发起支付流程：幂等 → 订单校验 → 渠道路由 → 回填渠道单号。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** 支付单号前缀，与 DO 注释「PAY + 雪花」一致 */
    private static final String PAYMENT_NO_PREFIX = "PAY";

    /** 支付单号时间部分格式：毫秒时间戳（17 位） */
    private static final DateTimeFormatter PAYMENT_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 支付单号随机后缀上界（6 位） */
    private static final int PAYMENT_NO_RANDOM_BOUND = 1_000_000;

    /** 渠道异步通知地址路径模板，与 callbackBaseUrl 拼接 */
    private static final String NOTIFY_URL_PATH = "/callback/payment/";

    private final MallPaymentMapper paymentMapper;

    private final MallPaymentChannelMapper channelMapper;

    private final RemoteOrderAdapter remoteOrderAdapter;

    private final PaymentChannelFactory channelFactory;

    private final MallPaymentConfigProperties configProperties;

    @Override
    public PayResultDTO createPayment(Long userId, PayRequestDTO req) {
        String idempotentKey = buildIdempotentKey(userId, req);

        // ① 幂等优先级最高：命中已有支付单则直接复用，完全跳过订单校验
        MallPaymentDO existing = paymentMapper.selectByIdempotentKey(idempotentKey);
        boolean isNew = existing == null;
        MallPaymentDO payment = isNew ? buildNewPayment(userId, req, idempotentKey) : existing;
        if (!isNew) {
            log.info("发起支付幂等命中，复用已有支付单: idempotentKey={}, paymentNo={}",
                    idempotentKey, payment.getPaymentNo());
        }

        // ② 路由渠道并发起支付（失败不得落支付单）
        MallPaymentChannelDO channel = channelMapper.selectByChannelCode(req.getChannelCode());
        if (channel == null) {
            throw new BusinessException(ErrorCode.PAYMENT_SERVICE_ERROR);
        }
        PaymentChannelAdapter adapter = channelFactory.getAdapter(req.getChannelCode());
        PayResult payResult = adapter.invokePay(payment, channel, req.getOpenid());
        if (payResult == null || !payResult.isSuccess()) {
            log.warn("渠道发起支付失败: paymentNo={}, channelCode={}, reason={}",
                    payment.getPaymentNo(), req.getChannelCode(),
                    payResult == null ? "渠道无响应" : payResult.getFailReason());
            throw new BusinessException(ErrorCode.PAYMENT_SERVICE_ERROR);
        }

        // ③ 渠道路由成功后才落库，保证失败路径不产生支付单
        if (isNew) {
            paymentMapper.insert(payment);
        }

        // ④ 回填渠道单号与异步通知地址
        String notifyUrl = configProperties.getCallbackBaseUrl() + NOTIFY_URL_PATH + req.getChannelCode();
        paymentMapper.updateChannelPaymentNo(payment.getPaymentNo(), payResult.getChannelPaymentNo(), notifyUrl);
        log.info("发起支付成功: userId={}, paymentNo={}, channelCode={}",
                userId, payment.getPaymentNo(), req.getChannelCode());

        return new PayResultDTO(payment.getPaymentNo(), payResult.getPayParams());
    }

    @Override
    public PaymentVO getPayment(Long userId, Long paymentId) {
        MallPaymentDO payment = paymentMapper.selectById(paymentId);
        // 不存在与非本人返回同一错误码，避免泄露支付单归属
        if (payment == null || !userId.equals(payment.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        PaymentVO vo = new PaymentVO();
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderNo(payment.getOrderNo());
        vo.setPayAmount(payment.getPayAmount());
        vo.setPaymentStatus(payment.getPaymentStatus());
        vo.applyStatusText(payment.getPaymentStatus());
        vo.setPaySuccessTime(payment.getPaySuccessTime());
        vo.setExpireTime(payment.getExpireTime());
        return vo;
    }

    /**
     * 构造幂等键
     *
     * <p>格式 {@code userId_orderNo_channelCode}，与 DB 唯一约束一致。</p>
     *
     * @param userId 付款用户 ID
     * @param req    发起支付请求
     * @return 幂等键
     */
    private String buildIdempotentKey(Long userId, PayRequestDTO req) {
        return userId + "_" + req.getOrderNo() + "_" + req.getChannelCode();
    }

    /**
     * 校验订单快照并构造待落库的支付单（不落库）
     *
     * <p>校验顺序：订单存在 → 状态为待支付 → 未过期 → 应付金额为正。
     * 金额与过期时间均取订单快照，不重新计算。</p>
     *
     * @param userId        付款用户 ID
     * @param req           发起支付请求
     * @param idempotentKey 幂等键
     * @return 待落库的支付单
     */
    private MallPaymentDO buildNewPayment(Long userId, PayRequestDTO req, String idempotentKey) {
        OrderDTO order = remoteOrderAdapter.queryOrder(req.getOrderNo());
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 归属校验：只能为自己的订单付款。刻意与「订单不存在」返回同一错误码，
        // 避免攻击者靠错误码差异探测他人订单是否存在。
        if (!Objects.equals(order.getUserId(), userId)) {
            log.warn("发起支付被拒：订单归属不符, orderNo={}, orderUserId={}, currentUserId={}",
                    req.getOrderNo(), order.getUserId(), userId);
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.WAIT_PAY.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        LocalDateTime expireTime = parseExpireTime(order.getPayExpireTime());
        if (expireTime == null || !expireTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        if (order.getPayAmount() == null || order.getPayAmount() <= 0L) {
            throw new BusinessException(ErrorCode.AMOUNT_EXCEED_LIMIT);
        }

        MallPaymentDO payment = new MallPaymentDO();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setPayAmount(order.getPayAmount());
        payment.setChannelCode(req.getChannelCode());
        payment.setPaymentStatus(PaymentStatusEnum.UNPAID.getCode());
        payment.setExpireTime(expireTime);
        payment.setIdempotentKey(idempotentKey);
        payment.setIsDeleted(0);
        payment.setVersion(0);
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        return payment;
    }

    /**
     * 解析订单的支付过期时间（ISO-8601 字符串）
     *
     * @param payExpireTime 订单支付过期时间，ISO-8601 字符串
     * @return 解析后的过期时间；入参为空或格式非法时返回 null
     */
    private LocalDateTime parseExpireTime(String payExpireTime) {
        if (payExpireTime == null || payExpireTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(payExpireTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (RuntimeException e) {
            log.warn("订单支付过期时间格式非法: payExpireTime={}", payExpireTime);
            return null;
        }
    }

    /**
     * 生成支付单号
     *
     * <p>格式：{@code PAY + 毫秒时间戳（17 位） + 6 位随机数}，由 {@code uk_payment_no} 保证唯一。</p>
     *
     * @return 支付单号
     */
    private String generatePaymentNo() {
        return PAYMENT_NO_PREFIX + LocalDateTime.now().format(PAYMENT_NO_FORMATTER)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(PAYMENT_NO_RANDOM_BOUND));
    }
}
