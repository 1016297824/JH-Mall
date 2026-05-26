package com.mall.common.constant;

/**
 * C 端 MQ Topic 常量
 *
 * <p>按领域内部类分组，格式统一为 {@code mall:{domain}:{action}}。
 * 所有 Topic 常量由各模块生产者/消费者使用，禁止硬编码 Topic 字符串。</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class MqTopicConstants {

    /** 订单域 */
    public static final class Order {
        public static final String CREATED = "mall:order:created";
        public static final String PAID = "mall:order:paid";
        public static final String CANCELLED = "mall:order:cancelled";
        public static final String DELIVERED = "mall:order:delivered";
        public static final String COMPLETED = "mall:order:completed";
        public static final String REFUNDED = "mall:order:refunded";
        public static final String TIMEOUT = "mall:order:timeout";

        private Order() {
        }
    }

    /** 支付/退款域 */
    public static final class Payment {
        public static final String CREATED = "mall:payment:created";
        public static final String PAID = "mall:payment:paid";
        public static final String FAILED = "mall:payment:failed";
        public static final String REFUND_CREATED = "mall:refund:created";
        public static final String REFUND_SUCCEEDED = "mall:refund:succeeded";

        private Payment() {
        }
    }

    /** 用户域 */
    public static final class User {
        public static final String REGISTERED = "mall:user:registered";

        private User() {
        }
    }

    /** 库存域 */
    public static final class Stock {
        public static final String RESERVED = "mall:stock:reserved";
        public static final String RELEASED = "mall:stock:released";

        private Stock() {
        }
    }

    /** 营销域 */
    public static final class Coupon {
        public static final String LOCKED = "mall:coupon:locked";
        public static final String USED = "mall:coupon:used";
        public static final String RELEASED = "mall:coupon:released";

        private Coupon() {
        }
    }

    /** 搜索域 */
    public static final class Search {
        public static final String SYNC = "mall:search:sync";
        public static final String REBUILD = "mall:search:rebuild";

        private Search() {
        }
    }

    private MqTopicConstants() {
    }
}
