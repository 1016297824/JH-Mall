package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.api.feign.RemoteMarketingService.CalculationReq.CalculationItem;
import com.mall.api.feign.RemoteMarketingService.CalculationResp;
import com.mall.api.feign.RemoteProductService.ReserveStockItemRequest;
import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.constant.CacheConstants;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.ErrorCode;
import com.mall.common.enums.order.OrderStatusEnum;
import com.mall.common.exception.BusinessException;
import com.mall.order.DO.MallCartDO;
import com.mall.order.DO.MallOrderAmountDO;
import com.mall.order.DO.MallOrderDO;
import com.mall.order.DO.MallOrderItemDO;
import com.mall.order.VO.OrderVO;
import com.mall.order.config.MallOrderConfigProperties;
import com.mall.order.convert.response.OrderConvert;
import com.mall.order.dto.request.CreateOrderRequest;
import com.mall.order.infrastructure.feign.RemoteMarketingAdapter;
import com.mall.order.infrastructure.feign.RemoteProductAdapter;
import com.mall.order.infrastructure.feign.RemoteUserAdapter;
import com.mall.order.infrastructure.outbox.OutboxPublisher;
import com.mall.order.mapper.MallCartMapper;
import com.mall.order.mapper.MallOrderAmountMapper;
import com.mall.order.mapper.MallOrderItemMapper;
import com.mall.order.mapper.MallOrderMapper;
import com.mall.order.service.OrderService;
import com.mall.order.statemachine.OrderEventEnum;
import com.mall.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 *
 * <p>对应设计文档 §5（下单）、§6（状态推进）。</p>
 *
 * <p><b>事务边界说明</b>（§5.8）：锁库存、锁优惠券是远端独立事务，
 * 本地事务只覆盖「订单主表 + 订单项 + 金额快照 + Outbox」。
 * Feign 失败不会回滚远端事务，因此失败路径<b>必须显式补偿</b>（§5.7），
 * 见 {@link #compensate}。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 幂等键 TTL，与设计文档 §5.2 的 SETNX EX 1800 一致 */
    private static final long IDEMPOTENT_TTL_MINUTES = 30L;

    /** 运费暂固定 0：设计文档未给出运费规则，见 §5.6 说明 */
    private static final long FREIGHT_AMOUNT = 0L;

    private final MallCartMapper cartMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final MallOrderAmountMapper orderAmountMapper;
    private final OrderStateMachine stateMachine;
    private final RemoteProductAdapter productAdapter;
    private final RemoteUserAdapter userAdapter;
    private final RemoteMarketingAdapter marketingAdapter;
    private final OutboxPublisher outboxPublisher;
    private final MallOrderConfigProperties config;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    // ═══════════════════════════════════════════════════════════
    // 下单
    // ═══════════════════════════════════════════════════════════

    @Override
    public String createOrder(Long userId, String idempotentKey, CreateOrderRequest req) {
        if (idempotentKey == null || idempotentKey.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }
        if (req == null || req.getAddressId() == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING);
        }

        // ① 幂等校验（Redis）
        String redisKey = CacheConstants.Order.IDEMPOTENT + userId + ":" + idempotentKey;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.info("幂等命中，返回已有订单: userId={}, orderNo={}", userId, cached);
            return String.valueOf(cached);
        }

        // ② 参数校验
        List<MallCartDO> cartList = cartMapper.selectSelectedByUserId(userId);
        if (cartList.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        if (!userAdapter.validateAddress(userId, req.getAddressId())) {
            // 不区分「地址不存在」与「非本人地址」
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Map<Long, ProductSkuDTO> skuMap = loadAndValidateSku(cartList);

        // ③ 优惠试算（不锁定）
        CalculationResp calculation = marketingAdapter.calculate(
                userId, buildCalculationItems(cartList, skuMap), req.getCouponRecordId());
        if (calculation == null || calculation.getFinalAmount() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        String orderNo = generateOrderNo();

        // ④ 锁库存 → 锁优惠券（远端独立事务）
        boolean stockLocked = false;
        boolean couponLocked = false;
        try {
            productAdapter.reserveStock(orderNo, buildReserveItems(cartList, skuMap));
            stockLocked = true;

            if (req.getCouponRecordId() != null) {
                if (!marketingAdapter.lockCoupon(orderNo, req.getCouponRecordId())) {
                    throw new BusinessException(ErrorCode.COUPON_CONDITION_NOT_MET);
                }
                couponLocked = true;
            }
        } catch (RuntimeException e) {
            compensate(orderNo, stockLocked, couponLocked);
            throw e;
        }

        // ⑤ 本地事务落库
        try {
            persistOrder(orderNo, userId, idempotentKey, req, cartList, skuMap, calculation);
        } catch (DuplicateKeyException e) {
            // uk_idempotent_key 兜底：并发重复请求已有一张单落库。
            // 本次锁的库存/券必须释放，然后把已存在的订单号返回给客户端。
            compensate(orderNo, stockLocked, couponLocked);
            MallOrderDO existingOrder = orderMapper.selectByIdempotentKey(userId + ":" + idempotentKey);
            if (existingOrder == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            log.info("幂等键撞唯一索引，返回已有订单: userId={}, orderNo={}", userId, existingOrder.getOrderNo());
            return existingOrder.getOrderNo();
        } catch (RuntimeException e) {
            compensate(orderNo, stockLocked, couponLocked);
            throw e;
        }

        // ⑥ 回填幂等键
        redisTemplate.opsForValue().set(redisKey, orderNo, IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("下单成功: userId={}, orderNo={}, payAmount={}", userId, orderNo, calculation.getFinalAmount());
        return orderNo;
    }

    /**
     * 本地事务：订单主表 + 订单项 + 金额快照 + Outbox
     *
     * <p>用 {@link TransactionTemplate} 而非 {@code @Transactional}，
     * 因为本方法是同类内调用，注解式事务不会生效（自调用失效）。</p>
     */
    private void persistOrder(String orderNo, Long userId, String clientIdempotentKey,
                              CreateOrderRequest req, List<MallCartDO> cartList,
                              Map<Long, ProductSkuDTO> skuMap, CalculationResp calculation) {
        String idempotentKey = userId + ":" + clientIdempotentKey;
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(config.getPayExpireMinutes());

        transactionTemplate.executeWithoutResult(txStatus -> {
            MallOrderDO order = new MallOrderDO();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setOrderStatus(OrderStatusEnum.WAIT_PAY.getCode());
            order.setTotalAmount(calculation.getOriginalAmount());
            order.setDiscountAmount(
                    nvl(calculation.getCouponDiscount()) + nvl(calculation.getPromotionDiscount()));
            order.setFreightAmount(FREIGHT_AMOUNT);
            order.setPayAmount(calculation.getFinalAmount());
            order.setPayExpireTime(expireTime);
            order.setIdempotentKey(idempotentKey);
            order.setRemark(req.getRemark());
            order.setIsDeleted(0);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            orderItemMapper.batchInsert(buildOrderItems(order.getId(), cartList, skuMap));
            orderAmountMapper.insert(buildAmountSnapshot(order.getId(), calculation));

            // Outbox：下单成功事件 + 支付超时延迟消息
            Map<String, Object> createdEvent = new LinkedHashMap<>();
            createdEvent.put("orderNo", orderNo);
            createdEvent.put("userId", userId);
            createdEvent.put("payAmount", calculation.getFinalAmount());
            createdEvent.put("payExpireTime", expireTime.toString());
            outboxPublisher.publish(MqTopicConstants.Order.CREATED, "OrderCreated", orderNo, createdEvent);

            Map<String, Object> timeoutEvent = new LinkedHashMap<>();
            timeoutEvent.put("orderNo", orderNo);
            outboxPublisher.publishScheduled(MqTopicConstants.Order.TIMEOUT, "OrderTimeout",
                    orderNo, timeoutEvent, expireTime);
        });
    }

    /**
     * 补偿已锁定的远端资源（设计文档 §5.7）
     *
     * <p>补偿失败只记 error 不再抛——此时本地事务已回滚，
     * 再抛会掩盖原始异常。日志中标记 orderNo 供人工排查。</p>
     */
    private void compensate(String orderNo, boolean stockLocked, boolean couponLocked) {
        if (couponLocked) {
            try {
                marketingAdapter.releaseCoupon(orderNo);
            } catch (Exception e) {
                log.error("【需人工介入】补偿释放优惠券失败, orderNo={}", orderNo, e);
            }
        }
        if (stockLocked) {
            try {
                if (!productAdapter.releaseStock(orderNo)) {
                    log.error("【需人工介入】补偿释放库存未全部成功（预扣记录已保留待核对）, orderNo={}", orderNo);
                }
            } catch (Exception e) {
                log.error("【需人工介入】补偿释放库存失败, orderNo={}", orderNo, e);
            }
        }
    }

    /**
     * 批量拉取 SKU 并校验在售与库存
     */
    private Map<Long, ProductSkuDTO> loadAndValidateSku(List<MallCartDO> cartList) {
        List<Long> skuIds = cartList.stream().map(MallCartDO::getSkuId).distinct().toList();
        Map<Long, ProductSkuDTO> skuMap = productAdapter.batchGetSkuSafely(skuIds);
        if (skuMap.isEmpty()) {
            // 全部降级：无法确认在售与库存，拒绝下单而非冒险成交
            throw new BusinessException(ErrorCode.PRODUCT_OFFLINE);
        }
        for (MallCartDO cart : cartList) {
            ProductSkuDTO sku = skuMap.get(cart.getSkuId());
            if (sku == null) {
                throw new BusinessException(ErrorCode.PRODUCT_OFFLINE);
            }
            if (!Boolean.TRUE.equals(sku.getIsOnSale())) {
                throw new BusinessException(ErrorCode.PRODUCT_OFFLINE);
            }
            if (sku.getAvailableQty() == null || sku.getAvailableQty() < cart.getQuantity()) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
        }
        return skuMap;
    }

    private List<CalculationItem> buildCalculationItems(List<MallCartDO> cartList,
                                                        Map<Long, ProductSkuDTO> skuMap) {
        List<CalculationItem> items = new ArrayList<>(cartList.size());
        for (MallCartDO cart : cartList) {
            ProductSkuDTO sku = skuMap.get(cart.getSkuId());
            // 实时价：实时数据不可用时回退购物车冗余价
            Long price = sku != null && sku.getPrice() != null ? sku.getPrice() : cart.getPrice();
            items.add(new CalculationItem(cart.getSkuId(), price, cart.getQuantity()));
        }
        return items;
    }

    private List<ReserveStockItemRequest> buildReserveItems(List<MallCartDO> cartList,
                                                            Map<Long, ProductSkuDTO> skuMap) {
        List<ReserveStockItemRequest> items = new ArrayList<>(cartList.size());
        for (MallCartDO cart : cartList) {
            items.add(new ReserveStockItemRequest(cart.getSkuId(), cart.getQuantity()));
        }
        return items;
    }

    private List<MallOrderItemDO> buildOrderItems(Long orderId, List<MallCartDO> cartList,
                                                  Map<Long, ProductSkuDTO> skuMap) {
        List<MallOrderItemDO> items = new ArrayList<>(cartList.size());
        for (MallCartDO cart : cartList) {
            ProductSkuDTO sku = skuMap.get(cart.getSkuId());
            // 实时价优先；实时数据不可用时回退购物车冗余价
            Long price = sku != null && sku.getPrice() != null ? sku.getPrice() : cart.getPrice();

            MallOrderItemDO item = new MallOrderItemDO();
            item.setOrderId(orderId);
            item.setSpuId(cart.getSpuId());
            item.setSkuId(cart.getSkuId());
            item.setSkuCode(cart.getSkuCode());
            item.setSkuName(cart.getSkuName());
            // ProductSkuDTO 不含 SPU 名称，此处留空，由后续 SPU 查询补齐
            item.setSpuName(null);
            item.setMainImage(cart.getMainImage());
            // 销售属性 JSON 未从 mall-product 获取（SKU DTO 不含该字段），留空
            item.setAttrsJson(null);
            item.setQuantity(cart.getQuantity());
            item.setPrice(price);
            item.setTotalPrice(price * cart.getQuantity());
            item.setIsDeleted(0);
            items.add(item);
        }
        return items;
    }

    private MallOrderAmountDO buildAmountSnapshot(Long orderId, CalculationResp calculation) {
        MallOrderAmountDO amount = new MallOrderAmountDO();
        amount.setOrderId(orderId);
        amount.setTotalAmount(calculation.getOriginalAmount());
        amount.setDiscountAmount(
                nvl(calculation.getCouponDiscount()) + nvl(calculation.getPromotionDiscount()));
        amount.setFreightAmount(FREIGHT_AMOUNT);
        amount.setPayAmount(calculation.getFinalAmount());
        amount.setPointsDiscount(0L);
        amount.setIsDeleted(0);
        return amount;
    }

    /**
     * 生成订单号：毫秒时间戳(17) + 6 位随机，共 23 位
     *
     * <p>{@code uk_order_no} 保证唯一；撞键时由 {@link #createOrder} 的
     * DuplicateKey 分支回查处理。</p>
     */
    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return ts + String.format("%06d", random);
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    // ═══════════════════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════════════════

    @Override
    public OrderVO getDetail(Long userId, String orderNo) {
        MallOrderDO order = requireOwnedOrder(userId, orderNo);
        List<MallOrderItemDO> items = orderItemMapper.selectByOrderId(order.getId());
        return OrderConvert.toVO(order, items, stateMachine);
    }

    @Override
    public List<OrderVO> listOrders(Long userId, Integer status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 1);
        List<MallOrderDO> orders = orderMapper.selectList(
                new LambdaQueryWrapper<MallOrderDO>()
                        .eq(MallOrderDO::getUserId, userId)
                        .eq(MallOrderDO::getIsDeleted, 0)
                        .eq(status != null, MallOrderDO::getOrderStatus, status)
                        .orderByDesc(MallOrderDO::getCreateTime)
                        .last("LIMIT " + ((safePage - 1) * safeSize) + "," + safeSize));
        return orders.stream().map(OrderConvert::toVO).toList();
    }

    /**
     * 取订单并校验归属，防越权
     */
    private MallOrderDO requireOwnedOrder(Long userId, String orderNo) {
        MallOrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    // ═══════════════════════════════════════════════════════════
    // 状态推进
    // ═══════════════════════════════════════════════════════════

    @Override
    public void cancelOrder(Long userId, String orderNo) {
        MallOrderDO order = requireOwnedOrder(userId, orderNo);
        transitionOrder(order, OrderEventEnum.USER_CANCEL, MqTopicConstants.Order.CANCELLED, "OrderCancelled");
    }

    @Override
    public void confirmReceipt(Long userId, String orderNo) {
        MallOrderDO order = requireOwnedOrder(userId, orderNo);
        transitionOrder(order, OrderEventEnum.CONFIRM_RECEIPT, MqTopicConstants.Order.COMPLETED, "OrderCompleted");
    }

    @Override
    public void payCallback(String orderNo) {
        MallOrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("payCallback 订单不存在: orderNo={}", orderNo);
            return;
        }
        // 幂等：重复投递时状态机抛 A0702，此处不视为失败
        try {
            transitionOrder(order, OrderEventEnum.PAY_SUCCESS, MqTopicConstants.Order.PAID, "OrderPaid");
        } catch (BusinessException e) {
            if (ErrorCode.ORDER_STATUS_ERROR.getCode().equals(e.getErrorCode())) {
                log.info("订单已是终态，忽略重复支付回调: orderNo={}, status={}", orderNo, order.getOrderStatus());
                return;
            }
            throw e;
        }
    }

    /**
     * 通用状态推进：状态机校验 → 乐观锁落库 → 写 Outbox
     */
    private void transitionOrder(MallOrderDO order, OrderEventEnum event,
                                 String outboxTopic, String eventType) {
        Integer originStatus = order.getOrderStatus();
        Integer version = order.getVersion();

        stateMachine.transition(order, event);

        transactionTemplate.executeWithoutResult(txStatus -> {
            int affected = orderMapper.updateStatusCas(order.getOrderNo(), order.getOrderStatus(),
                    originStatus, version, order.getPreRefundStatus());
            if (affected == 0) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderNo", order.getOrderNo());
            payload.put("userId", order.getUserId());
            outboxPublisher.publish(outboxTopic, eventType, order.getOrderNo(), payload);
        });
    }
}