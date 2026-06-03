package com.mall.common.constant;

/**
 * C 端 Redis Key 常量
 *
 * <p>按模块内部类分组，格式统一为 {@code mall:{service}:{biz}:{id}}。
 * 所有 Key 常量由各模块 Service 使用，禁止硬编码 Redis Key 字符串。</p>
 *
 * <p><b>通用约束：</b>
 * <ul>
 *   <li>所有 Key 必须设 TTL，严禁永久 Key</li>
 *   <li>Redis 不作为唯一事实来源，核心状态必须落 MySQL</li>
 *   <li>分布式锁统一 SETNX + UUID + Lua 释放</li>
 * </ul>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public class CacheConstants {

    /**
     * 认证模块 Redis Key（mall-auth）
     *
     * <p>覆盖登录鉴权、Token 管理、短信验证码、图片验证码等场景。</p>
     */
    public static final class Auth {


        /**
         * refreshToken 映射
         *
         * <p>Key 模式：{@code mall:auth:refresh:{jti}}</p>
         * <ul>
         *   <li>TTL：7d</li>
         *   <li>数据结构：String，存储 userId</li>
         *   <li>刷新 token 时校验 refreshToken 有效性</li>
         * </ul>
         */
        public static final String REFRESH = "mall:auth:refresh:";

        /**
         * Token 黑名单
         *
         * <p>Key 模式：{@code mall:auth:blacklist:{jti}}</p>
         * <ul>
         *   <li>TTL：原 token 剩余有效期</li>
         *   <li>数据结构：String</li>
         *   <li>注销 / 刷新 token 时加入，防止旧 token 继续使用</li>
         * </ul>
         */
        public static final String BLACKLIST = "mall:auth:blacklist:";

        /**
         * 短信验证码存储
         *
         * <p>Key 模式：{@code mall:auth:sms:code:{phone}:{scene}}</p>
         * <ul>
         *   <li>TTL：300s（5min）</li>
         *   <li>数据结构：String</li>
         *   <li>scene 取值：register / login / reset_password / change_phone / bind_phone</li>
         *   <li>一次性消费，校验通过后立即删除</li>
         * </ul>
         */
        public static final String SMS_CODE = "mall:auth:sms:code:";

        /**
         * 短信发送冷却
         *
         * <p>Key 模式：{@code mall:auth:sms:limit:{phone}}</p>
         * <ul>
         *   <li>TTL：60s</li>
         *   <li>数据结构：String（SETNX），同一手机号最短发送间隔</li>
         * </ul>
         */
        public static final String SMS_LIMIT = "mall:auth:sms:limit:";

        /**
         * 短信验证失败计数
         *
         * <p>Key 模式：{@code mall:auth:sms:try:{phone}}</p>
         * <ul>
         *   <li>TTL：24h</li>
         *   <li>数据结构：Counter</li>
         *   <li>同一手机号日最大验证尝试 5 次</li>
         * </ul>
         */
        public static final String SMS_TRY = "mall:auth:sms:try:";

        /**
         * IP 日发送限制
         *
         * <p>Key 模式：{@code mall:auth:sms:ip:{ip}}</p>
         * <ul>
         *   <li>TTL：24h</li>
         *   <li>数据结构：Counter</li>
         *   <li>同一 IP 日最大 10 次</li>
         * </ul>
         */
        public static final String SMS_IP = "mall:auth:sms:ip:";

        /**
         * 密码错误计数
         *
         * <p>Key 模式：{@code mall:auth:pwd_err:{userId}}</p>
         * <ul>
         *   <li>TTL：30min</li>
         *   <li>数据结构：Counter</li>
         *   <li>连续 5 次错误锁定 30min</li>
         * </ul>
         */
        public static final String PWD_ERR = "mall:auth:pwd_err:";

        /**
         * 解密结果缓存
         *
         * <p>Key 模式：{@code mall:auth:decrypt:{sha256(ciphertext)}}</p>
         * <ul>
         *   <li>TTL：60s</li>
         *   <li>数据结构：String</li>
         *   <li>AES-256-GCM 解密结果缓存，减少重复解密开销</li>
         * </ul>
         */
        public static final String DECRYPT = "mall:auth:decrypt:";

        /**
         * 图片验证码
         *
         * <p>Key 模式：{@code mall:auth:captcha:{captchaKey}}</p>
         * <ul>
         *   <li>TTL：300s（5min）</li>
         *   <li>数据结构：String</li>
         *   <li>EasyCaptcha 生成 4 位字符，Base64 返回</li>
         * </ul>
         */
        public static final String CAPTCHA = "mall:auth:captcha:";

        /**
         * 图片验证码 IP 防刷
         *
         * <p>Key 模式：{@code mall:auth:captcha:ip:{ip}}</p>
         * <ul>
         *   <li>TTL：24h</li>
         *   <li>数据结构：Counter</li>
         *   <li>同一 IP 校验失败计数，超出阈值限流</li>
         * </ul>
         */
        public static final String CAPTCHA_IP = "mall:auth:captcha:ip:";

        /**
         * 用户 token 版本号
         *
         * <p>Key 模式：{@code mall:auth:user_version:{userId}}</p>
         * <ul>
         *   <li>TTL：Nacos {@code mall.auth.token-version-cache-ttl} 配置（默认 30d）</li>
         *   <li>数据结构：Integer</li>
         *   <li>缓存 miss 时回源 DB 兜底</li>
         * </ul>
         */
        public static final String USER_VERSION = "mall:auth:user_version:";

        private Auth() {
        }
    }

    /**
     * 用户模块 Redis Key（mall-user）
     *
     * <p>覆盖用户资料缓存、签到记录等场景。</p>
     */
    public static final class User {

        /**
         * 用户资料缓存
         *
         * <p>Key 模式：{@code mall:user:profile:{userId}}</p>
         * <ul>
         *   <li>TTL：600s（10min），对应 Nacos {@code mall.user.profile.cache-ttl}</li>
         *   <li>数据结构：String (JSON)</li>
         *   <li>读时回种（cache aside），写时删除</li>
         * </ul>
         */
        public static final String PROFILE = "mall:user:profile:";

        /**
         * 签到记录
         *
         * <p>Key 模式：{@code mall:user:sign:{userId}:{yyyyMM}}</p>
         * <ul>
         *   <li>TTL：跨月（自然过期）</li>
         *   <li>数据结构：Bitmap</li>
         *   <li>按月分片，每日签到在对应 offset 位置 1</li>
         * </ul>
         */
        public static final String SIGN = "mall:user:sign:";

        private User() {
        }
    }

    /**
     * 商品模块 Redis Key（mall-product）
     *
     * <p>覆盖 SKU 缓存、类目树、排行榜、UV 统计等场景。</p>
     */
    public static final class Product {

        /**
         * SKU 信息缓存
         *
         * <p>Key 模式：{@code mall:product:sku:{skuId}}</p>
         * <ul>
         *   <li>TTL：600s（10min）</li>
         *   <li>数据结构：String (JSON)，缓存完整 SkuDTO（price / stock / name / image / isOnSale）</li>
         *   <li>读时回种，写时删除 + 双删（延迟删除防并发）</li>
         * </ul>
         */
        public static final String SKU = "mall:product:sku:";

        /**
         * 类目树缓存
         *
         * <p>Key 模式：{@code mall:product:category:tree}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：String (JSON)，缓存完整三级类目树（嵌套 children）</li>
         *   <li>应用启动预热，类目变更时刷新</li>
         * </ul>
         */
        public static final String CATEGORY_TREE = "mall:product:category:tree";

        /**
         * 单类目缓存
         *
         * <p>Key 模式：{@code mall:product:category:{categoryId}}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：String (JSON)</li>
         *   <li>应用启动时预热加载</li>
         * </ul>
         */
        public static final String CATEGORY = "mall:product:category:";

        /**
         * 最新商品列表
         *
         * <p>Key 模式：{@code mall:product:newest:list}</p>
         * <ul>
         *   <li>TTL：无（手动维护）</li>
         *   <li>数据结构：List，LPUSH + LTRIM 保留固定长度</li>
         * </ul>
         */
        public static final String NEWEST_LIST = "mall:product:newest:list";

        /**
         * 商品标签集合
         *
         * <p>Key 模式：{@code mall:product:tag:{tagId}}</p>
         * <ul>
         *   <li>TTL：无（手动维护）</li>
         *   <li>数据结构：Set，存储标签关联的商品 ID</li>
         *   <li>支持交集 / 差集运算</li>
         * </ul>
         */
        public static final String TAG = "mall:product:tag:";

        /**
         * 热销排行榜
         *
         * <p>Key 模式：{@code mall:product:hot:rank}</p>
         * <ul>
         *   <li>TTL：无（手动维护）</li>
         *   <li>数据结构：ZSet，按销量排序的商品排行</li>
         * </ul>
         */
        public static final String HOT_RANK = "mall:product:hot:rank";

        /**
         * 商品 UV 统计
         *
         * <p>Key 模式：{@code mall:product:uv:{productId}}</p>
         * <ul>
         *   <li>TTL：无（按需维护）</li>
         *   <li>数据结构：HyperLogLog</li>
         *   <li>误差 0.81% 的去重 UV 计数</li>
         * </ul>
         */
        public static final String UV = "mall:product:uv:";

        /**
         * 库存扣减幂等键
         *
         * <p>Key 模式：{@code mall:product:stock:reserve:{orderNo}:{skuId}}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：String（SETNX），value 存预留数量</li>
         *   <li>防止同一订单重复扣减库存</li>
         * </ul>
         */
        public static final String STOCK_RESERVE = "mall:product:stock:reserve:";

        /**
         * Outbox 消息幂等键
         *
         * <p>Key 模式：{@code mall:product:outbox:{messageId}}</p>
         * <ul>
         *   <li>TTL：86400s（24h）</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>Outbox 发件箱投递消息前幂等去重</li>
         * </ul>
         */
        public static final String OUTBOX = "mall:product:outbox:";

        /**
         * 订单支付消息消费幂等键
         *
         * <p>Key 模式：{@code mall:product:hot:paid:{orderNo}}</p>
         * <ul>
         *   <li>TTL：604800s（7d）</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>热点排名增量更新幂等去重</li>
         * </ul>
         */
        public static final String HOT_PAID = "mall:product:hot:paid:";

        private Product() {
        }
    }

    /**
     * 订单模块 Redis Key（mall-order）
     *
     * <p>覆盖购物车缓存、下单幂等等场景。</p>
     */
    public static final class Order {

        /**
         * 购物车缓存
         *
         * <p>Key 模式：{@code mall:order:cart:{userId}}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：Hash，field = skuId，value = quantity</li>
         *   <li>加速购物车查询，MySQL 为唯一事实来源</li>
         * </ul>
         */
        public static final String CART = "mall:order:cart:";

        /**
         * 下单幂等键
         *
         * <p>Key 模式：{@code mall:order:idempotent:{userId}:{clientRequestNo}}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：String（SETNX），value 存 orderNo</li>
         *   <li>防止重复下单，命中直接返回已有 orderNo</li>
         * </ul>
         */
        public static final String IDEMPOTENT = "mall:order:idempotent:";

        private Order() {
        }
    }

    /**
     * 支付模块 Redis Key（mall-payment）
     *
     * <p>覆盖支付/退款回调防重放、支付幂等等场景。</p>
     */
    public static final class Payment {

        /**
         * 支付回调防重放
         *
         * <p>Key 模式：{@code mall:payment:callback:{channel}:{tradeNo}}</p>
         * <ul>
         *   <li>TTL：86400s（24h），覆盖支付平台最大重试窗口</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>微信重试 15 次/6h，支付宝重试窗口 24h</li>
         * </ul>
         */
        public static final String CALLBACK = "mall:payment:callback:";

        /**
         * 退款回调防重放
         *
         * <p>Key 模式：{@code mall:payment:refund_callback:{channel}:{refundNo}}</p>
         * <ul>
         *   <li>TTL：86400s（24h）</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>refundNo 维度去重</li>
         * </ul>
         */
        public static final String REFUND_CALLBACK = "mall:payment:refund_callback:";

        /**
         * 支付幂等键
         *
         * <p>Key 模式：{@code mall:payment:idempotent:{userId}:{orderNo}:{channelCode}}</p>
         * <ul>
         *   <li>TTL：1800s（30min）</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>DB 唯一约束冗余缓存层，减少 DuplicateKeyException 次数</li>
         * </ul>
         */
        public static final String IDEMPOTENT = "mall:payment:idempotent:";

        private Payment() {
        }
    }

    /**
     * 营销模块 Redis Key（mall-marketing）
     *
     * <p>覆盖优惠券锁等场景。</p>
     */
    public static final class Marketing {

        /**
         * 优惠券锁定标记
         *
         * <p>Key 模式：{@code mall:marketing:coupon_lock:{orderNo}}</p>
         * <ul>
         *   <li>TTL：1800s（30min），含看门狗自动续期</li>
         *   <li>数据结构：String（SETNX + UUID）</li>
         *   <li>分布式锁，下单锁券时防止并发修改，超时自动释放</li>
         * </ul>
         */
        public static final String COUPON_LOCK = "mall:marketing:coupon_lock:";

        private Marketing() {
        }
    }

    /**
     * 搜索模块 Redis Key（mall-search）
     *
     * <p>覆盖搜索结果缓存、索引重建锁、同步去重、搜索建议等场景。</p>
     */
    public static final class Search {

        /**
         * 搜索结果缓存
         *
         * <p>Key 模式：{@code mall:search:result:{md5(query)}}</p>
         * <ul>
         *   <li>TTL：60s（1min），对应 Nacos {@code mall.search.result.cache-ttl}</li>
         *   <li>数据结构：String (JSON)</li>
         *   <li>热点词缓存命中率 &gt;60% 的场景不走 ES</li>
         * </ul>
         */
        public static final String RESULT = "mall:search:result:";

        /**
         * 全量索引重建分布式锁
         *
         * <p>Key 模式：{@code mall:search:index:rebuild_lock}</p>
         * <ul>
         *   <li>TTL：3600s（1h），含看门狗自动续期</li>
         *   <li>数据结构：String（SETNX + UUID）</li>
         *   <li>防止并发触发全量重建，重建完成后主动 DEL</li>
         * </ul>
         */
        public static final String INDEX_REBUILD_LOCK = "mall:search:index:rebuild_lock";

        /**
         * 索引同步幂等去重
         *
         * <p>Key 模式：{@code mall:search:dedup:{productId}:{operation}}</p>
         * <ul>
         *   <li>TTL：3600s（1h），覆盖最长补偿周期</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>operation 取值：UPSERT / DELETE</li>
         * </ul>
         */
        public static final String DEDUP = "mall:search:dedup:";

        /**
         * 热门搜索词缓存
         *
         * <p>Key 模式：{@code mall:search:suggestion:hot_keywords}</p>
         * <ul>
         *   <li>TTL：3600s（1h）</li>
         *   <li>数据结构：String (JSON)，缓存 Top 20 关键词</li>
         *   <li>服务启动时预热加载</li>
         * </ul>
         */
        public static final String SUGGESTION_HOT_KEYWORDS = "mall:search:suggestion:hot_keywords";

        /**
         * 搜索建议缓存
         *
         * <p>Key 模式：{@code mall:search:suggest:{prefix}}</p>
         * <ul>
         *   <li>TTL：300s（5min），对应 Nacos {@code mall.search.suggest.cache-ttl}</li>
         *   <li>数据结构：String (JSON)</li>
         *   <li>热门搜索补全建议结果缓存</li>
         * </ul>
         */
        public static final String SUGGEST = "mall:search:suggest:";

        private Search() {
        }
    }

    /**
     * 内部服务签名 Redis Key（共用）
     *
     * <p>InnerSignatureFilter 验签使用。</p>
     */
    public static final class Internal {

        /**
         * 签名 Nonce 防重放
         *
         * <p>Key 模式：{@code mall:internal:nonce:{nonce}}</p>
         * <ul>
         *   <li>TTL：{@code internal-timestamp-tolerance-seconds}（默认 300s）</li>
         *   <li>数据结构：String（SETNX），同一 nonce 不可重复使用</li>
         * </ul>
         */
        public static final String NONCE = "mall:internal:nonce:";

        /**
         * Nonce 占位值（防重放 SETNX 写入的固定 value）
         */
        public static final String NONCE_VALUE = "1";

        private Internal() {
        }
    }

    /**
     * MQ 基础设施 Redis Key（共用）
     *
     * <p>所有 MQ Consumer 共用此去重 Key。</p>
     */
    public static final class MQ {

        /**
         * MQ 消费幂等去重
         *
         * <p>Key 模式：{@code mall:mq:dedup:{messageId}:{consumerGroup}}</p>
         * <ul>
         *   <li>TTL：86400s（24h），超过消息最长存活时间</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>消费前 SETNX，命中直接 ACK 跳过</li>
         * </ul>
         */
        public static final String DEDUP = "mall:mq:dedup:";

        private MQ() {
        }
    }

    /**
     * 分布式任务锁 Redis Key（共用）
     *
     * <p>ruoyi-job 调度时防止多实例并发执行。</p>
     */
    public static final class Job {

        /**
         * 订单超时关单分布式锁
         *
         * <p>Key 模式：{@code mall:job:lock:order_timeout}</p>
         * <ul>
         *   <li>TTL：120s</li>
         *   <li>数据结构：String（SETNX + UUID）</li>
         *   <li>ruoyi-job 超时关单任务多实例互斥</li>
         * </ul>
         */
        public static final String LOCK_ORDER_TIMEOUT = "mall:job:lock:order_timeout";

        /**
         * 热点排名刷新分布式锁
         *
         * <p>Key 模式：{@code mall:job:lock:hot_rank}</p>
         * <ul>
         *   <li>TTL：600s（10min），覆盖全量刷新的最大耗时窗口</li>
         *   <li>数据结构：String（SETNX）</li>
         *   <li>ruoyi-job 热点排名刷新任务多实例互斥</li>
         * </ul>
         */
        public static final String LOCK_HOT_RANK = "mall:job:lock:hot_rank";

        private Job() {
        }
    }

    private CacheConstants() {
    }
}
