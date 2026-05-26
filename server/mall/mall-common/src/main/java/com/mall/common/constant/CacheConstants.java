package com.mall.common.constant;

public class CacheConstants {

    /** 认证模块 Redis Key */
    public static final class Auth {
        public static final String SESSION = "mall:auth:session:";
        public static final String REFRESH = "mall:auth:refresh:";
        public static final String BLACKLIST = "mall:auth:blacklist:";
        public static final String SMS_CODE = "mall:auth:sms:code:";
        public static final String SMS_LIMIT = "mall:auth:sms:limit:";
        public static final String SMS_TRY = "mall:auth:sms:try:";
        public static final String SMS_IP = "mall:auth:sms:ip:";
        public static final String PWD_ERR = "mall:auth:pwd_err:";
        public static final String DECRYPT = "mall:auth:decrypt:";
        public static final String CAPTCHA = "mall:auth:captcha:";
        public static final String CAPTCHA_IP = "mall:auth:captcha:ip:";

        private Auth() {
        }
    }

    /** 用户模块 Redis Key */
    public static final class User {
        public static final String PROFILE = "mall:user:profile:";
        public static final String SIGN = "mall:user:sign:";

        private User() {
        }
    }

    /** 商品模块 Redis Key */
    public static final class Product {
        public static final String SKU = "mall:product:sku:";
        public static final String CATEGORY_TREE = "mall:product:category:tree";
        public static final String CATEGORY = "mall:product:category:";
        public static final String NEWEST_LIST = "mall:product:newest:list";
        public static final String TAG = "mall:product:tag:";
        public static final String HOT_RANK = "mall:product:hot:rank";
        public static final String UV = "mall:product:uv:";

        private Product() {
        }
    }

    /** 订单模块 Redis Key */
    public static final class Order {
        public static final String CART = "mall:order:cart:";
        public static final String IDEMPOTENT = "mall:order:idempotent:";

        private Order() {
        }
    }

    /** 支付模块 Redis Key */
    public static final class Payment {
        public static final String CALLBACK = "mall:payment:callback:";
        public static final String REFUND_CALLBACK = "mall:payment:refund_callback:";
        public static final String IDEMPOTENT = "mall:payment:idempotent:";

        private Payment() {
        }
    }

    /** 营销模块 Redis Key */
    public static final class Marketing {
        public static final String COUPON_LOCK = "mall:marketing:coupon_lock:";

        private Marketing() {
        }
    }

    /** 搜索模块 Redis Key */
    public static final class Search {
        public static final String RESULT = "mall:search:result:";
        public static final String INDEX_REBUILD_LOCK = "mall:search:index:rebuild_lock";
        public static final String DEDUP = "mall:search:dedup:";
        public static final String SUGGESTION_HOT_KEYWORDS = "mall:search:suggestion:hot_keywords";
        public static final String SUGGEST = "mall:search:suggest:";

        private Search() {
        }
    }

    /** MQ 基础设施 Redis Key */
    public static final class MQ {
        public static final String DEDUP = "mall:mq:dedup:";

        private MQ() {
        }
    }

    /** 分布式任务锁 Redis Key */
    public static final class Job {
        public static final String LOCK_ORDER_TIMEOUT = "mall:job:lock:order_timeout";

        private Job() {
        }
    }

    private CacheConstants() {
    }
}
