package com.mall.common.constant;

/**
 * C 端 MQ Topic 常量
 *
 * <p>按领域内部类分组，格式统一为 {@code mall:{domain}:{action}}。
 * 所有 Topic 常量由各模块生产者/消费者使用，禁止硬编码 Topic 字符串。</p>
 *
 * <p><b>通用约束：</b>
 * <ul>
 *   <li>Payload 统一 JSON + lowerCamelCase，禁止序列化数据库实体</li>
 *   <li>消费端通过 {@code messageId + consumerGroup} + Redis SETNX 去重（TTL 24h）</li>
 *   <li>死信队列命名：{@code {topic}:dlq}</li>
 *   <li>退避算法：{@code min(base * 2^retry, max=120s) + random(0,5s)}，最多重试 3 次</li>
 *   <li>采用普通消息 + 状态机保证顺序，不使用严格顺序消息</li>
 * </ul>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class MqTopicConstants {

    /**
     * 订单域（生产者：mall-order）
     *
     * <p>消费组命名：{@code mall-order:mall-order:{action}}、{@code mall-product:mall-order:{action}} 等。</p>
     */
    public static final class Order {

        /**
         * 订单创建（预留通知）
         *
         * <p>Topic：{@code mall:order:created}</p>
         * <ul>
         *   <li>生产者：mall-order（下单成功，同一事务写 Outbox）</li>
         *   <li>消费者：暂无（预留通知）</li>
         *   <li>Payload：{@code orderNo, userId, payAmount, payExpireTime}</li>
         * </ul>
         */
        public static final String CREATED = "mall:order:created";

        /**
         * 订单支付成功
         *
         * <p>Topic：{@code mall:order:paid}</p>
         * <ul>
         *   <li>生产者：mall-order（WAIT_PAY → PAID 后写 Outbox）</li>
         *   <li>消费者：mall-marketing（核销优惠券 LOCKED → USED）</li>
         *   <li>Payload：{@code orderNo, userId, paidTime}</li>
         * </ul>
         */
        public static final String PAID = "mall:order:paid";

        /**
         * 订单取消
         *
         * <p>Topic：{@code mall:order:cancelled}</p>
         * <ul>
         *   <li>生产者：mall-order（取消 / 超时关闭后写 Outbox）</li>
         *   <li>消费者：mall-product（释放库存）+ mall-marketing（释放优惠券）</li>
         *   <li>Payload：{@code orderNo, userId, cancelReason}（USER_CANCEL / PAY_TIMEOUT / FORCE_CANCEL）</li>
         * </ul>
         */
        public static final String CANCELLED = "mall:order:cancelled";

        /**
         * 订单已发货（预留物流通知）
         *
         * <p>Topic：{@code mall:order:delivered}</p>
         * <ul>
         *   <li>生产者：mall-order（商家发货后写 Outbox）</li>
         *   <li>消费者：暂无（预留物流通知）</li>
         *   <li>Payload：{@code orderNo, logisticsCompany, logisticsNo}</li>
         * </ul>
         */
        public static final String DELIVERED = "mall:order:delivered";

        /**
         * 订单完成
         *
         * <p>Topic：{@code mall:order:completed}</p>
         * <ul>
         *   <li>生产者：mall-order（确认收货后写 Outbox）</li>
         *   <li>消费者：mall-user（赠送积分 / 成长值）</li>
         *   <li>Payload：{@code orderNo, userId}</li>
         * </ul>
         */
        public static final String COMPLETED = "mall:order:completed";

        /**
         * 退款完成（预留通知）
         *
         * <p>Topic：{@code mall:order:refunded}</p>
         * <ul>
         *   <li>生产者：mall-order（退款完成后写 Outbox）</li>
         *   <li>消费者：暂无（预留通知）</li>
         *   <li>Payload：{@code orderNo, userId, refundAmount}</li>
         * </ul>
         */
        public static final String REFUNDED = "mall:order:refunded";

        /**
         * 订单超时关单（延迟消息）
         *
         * <p>Topic：{@code mall:order:timeout}</p>
         * <ul>
         *   <li>生产者：mall-order（下单时写 Outbox，scheduled_time = NOW + 30min）</li>
         *   <li>消费者：mall-order 自身（OrderTimeoutConsumer，乐观锁关单）</li>
         *   <li>Payload：{@code orderNo}</li>
         *   <li>关单成功后再投递 {@code mall:order:cancelled} 释放库存/优惠券</li>
         * </ul>
         */
        public static final String TIMEOUT = "mall:order:timeout";

        private Order() {
        }
    }

    /**
     * 支付/退款域（生产者：mall-payment）
     *
     * <p>消费组命名：{@code mall-order:mall-payment:{action}}。</p>
     */
    public static final class Payment {

        /**
         * 支付单创建（预留）
         *
         * <p>Topic：{@code mall:payment:created}</p>
         * <ul>
         *   <li>生产者：mall-payment</li>
         *   <li>消费者：暂无</li>
         * </ul>
         */
        public static final String CREATED = "mall:payment:created";

        /**
         * 支付成功
         *
         * <p>Topic：{@code mall:payment:paid}</p>
         * <ul>
         *   <li>生产者：mall-payment（支付回调处理成功后写 Outbox）</li>
         *   <li>消费者：mall-order（推进订单 WAIT_PAY → PAID）</li>
         *   <li>Payload：{@code paymentNo, orderNo, userId, payAmount（分）, payTime, channelPaymentNo, channelCode}</li>
         * </ul>
         */
        public static final String PAID = "mall:payment:paid";

        /**
         * 支付失败（预留）
         *
         * <p>Topic：{@code mall:payment:failed}</p>
         * <ul>
         *   <li>生产者：mall-payment</li>
         *   <li>消费者：暂无</li>
         * </ul>
         */
        public static final String FAILED = "mall:payment:failed";

        /**
         * 退款单创建（预留）
         *
         * <p>Topic：{@code mall:refund:created}</p>
         * <ul>
         *   <li>生产者：mall-payment</li>
         *   <li>消费者：暂无</li>
         * </ul>
         */
        public static final String REFUND_CREATED = "mall:refund:created";

        /**
         * 退款成功
         *
         * <p>Topic：{@code mall:refund:succeeded}</p>
         * <ul>
         *   <li>生产者：mall-payment（退款回调成功后写 Outbox）</li>
         *   <li>消费者：mall-order（推进售后状态 / 退货退款回补库存）</li>
         *   <li>Payload：{@code refundNo, paymentNo, orderNo, afterSaleNo, userId, refundAmount（分）, refundTime, channelRefundNo}</li>
         * </ul>
         */
        public static final String REFUND_SUCCEEDED = "mall:refund:succeeded";

        private Payment() {
        }
    }

    /**
     * 用户域（生产者：mall-auth）
     *
     * <p>消费组命名：{@code mall-user:mall-user:registered}。</p>
     */
    public static final class User {

        /**
         * 用户注册成功
         *
         * <p>Topic：{@code mall:user:registered}</p>
         * <ul>
         *   <li>生产者：mall-auth（注册成功后直接同步发送，无 Outbox）</li>
         *   <li>消费者：mall-user（初始化积分账户 + 成长值）</li>
         *   <li>Payload：{@code userId, phone（脱敏）, registerTime, channel}（PHONE / WECHAT）</li>
         * </ul>
         */
        public static final String REGISTERED = "mall:user:registered";

        private User() {
        }
    }

    /**
     * 库存域（预留）
     *
     * <p>当前库存锁定/释放通过 Feign 同步调用或消费 {@code mall:order:cancelled} 完成。</p>
     */
    public static final class Stock {

        /**
         * 库存预占（预留）
         *
         * <p>Topic：{@code mall:stock:reserved}</p>
         * <ul>
         *   <li>当前库存锁定通过 Feign 同步调用完成，该 Topic 预留用于异步解耦</li>
         * </ul>
         */
        public static final String RESERVED = "mall:stock:reserved";

        /**
         * 库存释放（预留）
         *
         * <p>Topic：{@code mall:stock:released}</p>
         * <ul>
         *   <li>当前库存释放通过消费 {@code mall:order:cancelled} 完成，该 Topic 预留用于异步解耦</li>
         * </ul>
         */
        public static final String RELEASED = "mall:stock:released";

        private Stock() {
        }
    }

    /**
     * 营销域（生产者：mall-marketing）
     *
     * <p>消费组命名：{@code mall-marketing:mall-coupon:{action}}。</p>
     */
    public static final class Coupon {

        /**
         * 优惠券锁定（预留）
         *
         * <p>Topic：{@code mall:coupon:locked}</p>
         * <ul>
         *   <li>当前锁券通过 Feign 同步调用完成，该 Topic 预留用于异步解耦</li>
         * </ul>
         */
        public static final String LOCKED = "mall:coupon:locked";

        /**
         * 优惠券核销
         *
         * <p>Topic：{@code mall:coupon:used}</p>
         * <ul>
         *   <li>生产者：mall-marketing（订单支付成功核销优惠券后写 Outbox）</li>
         *   <li>消费者：暂无（预留数据统计）</li>
         *   <li>Payload：{@code couponRecordId, couponId, userId, orderNo, faceValue（分）, useTime}</li>
         * </ul>
         */
        public static final String USED = "mall:coupon:used";

        /**
         * 优惠券释放（预留）
         *
         * <p>Topic：{@code mall:coupon:released}</p>
         * <ul>
         *   <li>当前释放通过消费 {@code mall:order:cancelled} 完成，该 Topic 预留用于异步解耦</li>
         * </ul>
         */
        public static final String RELEASED = "mall:coupon:released";

        private Coupon() {
        }
    }

    /**
     * 商品域（生产者：mall-product）
     *
     * <p>消费组命名：{@code mall-search:mall-search:sync}。</p>
     */
    public static final class Product {

        /**
         * 搜索索引同步
         *
         * <p>Topic：{@code mall:search:sync}</p>
         * <ul>
         *   <li>生产者：mall-product（商品信息变更后写 Outbox）</li>
         *   <li>消费者：mall-search（SearchSyncConsumer，幂等去重后增量写入 ES）</li>
         *   <li>Payload：{@code spuId, operation}（UPSERT / DELETE），{@code timestamp}</li>
         * </ul>
         */
        public static final String SEARCH_SYNC = "mall:search:sync";

        private Product() {
        }
    }

    /**
     * 搜索域（生产者：mall-product）
     *
     * <p>消费组命名：{@code mall-search:mall-search:sync}。</p>
     */
    public static final class Search {

        /**
         * 商品索引增量同步
         *
         * <p>Topic：{@code mall:search:sync}</p>
         * <ul>
         *   <li>生产者：mall-product（商品信息变更后写 Outbox）</li>
         *   <li>消费者：mall-search（SearchSyncConsumer，幂等去重后增量写入 ES）</li>
         *   <li>Payload：{@code spuId, operation}（UPSERT / DELETE），{@code timestamp}</li>
         * </ul>
         */
        public static final String SYNC = "mall:search:sync";

        /**
         * 全量重建通知（预留）
         *
         * <p>Topic：{@code mall:search:rebuild}</p>
         * <ul>
         *   <li>预留用于全量索引重建完成后的通知</li>
         * </ul>
         */
        public static final String REBUILD = "mall:search:rebuild";

        private Search() {
        }
    }

    private MqTopicConstants() {
    }
}
