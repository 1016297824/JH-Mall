import type { PageDomain, BaseEntity } from "../common";

/** 支付单配置分页查询参数 */
export interface PaymentQueryParams extends PageDomain {
  /** 支付单号，格式 PAY + 时间戳 + 随机数 */
  paymentNo?: string;
  /** 关联订单号 */
  orderNo?: string;
  /** 付款用户 ID */
  userId?: string;
  /** 支付金额（单位：分） */
  payAmount?: string;
  /** 支付渠道编码 */
  channelCode?: string;
  /** 渠道侧支付单号，对账用 */
  channelPaymentNo?: string;
  /** 渠道侧支付状态 */
  channelPayStatus?: string;
  /** 支付单状态 */
  paymentStatus?: string;
  /** 支付成功时间 */
  paySuccessTime?: string;
  /** 支付过期时间 */
  expireTime?: string;
  /** 异步通知地址 */
  notifyUrl?: string;
  /** 幂等键 */
  idempotentKey?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 乐观锁版本号 */
  version?: string;
}

/** 支付单配置信息 */
export interface MallPayment extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 支付单号，格式 PAY + 时间戳 + 随机数 */
  paymentNo?: string;
  /** 关联订单号 */
  orderNo?: string;
  /** 付款用户 ID */
  userId?: string;
  /** 支付金额（单位：分） */
  payAmount?: string;
  /** 支付渠道编码 */
  channelCode?: string;
  /** 渠道侧支付单号，对账用 */
  channelPaymentNo?: string;
  /** 渠道侧支付状态 */
  channelPayStatus?: string;
  /** 支付单状态 */
  paymentStatus?: string;
  /** 支付成功时间 */
  paySuccessTime?: string;
  /** 支付过期时间 */
  expireTime?: string;
  /** 异步通知地址 */
  notifyUrl?: string;
  /** 幂等键 */
  idempotentKey?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建人 */
  createBy?: string;
  /** 修改人 */
  updateBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号 */
  version?: string;
  /** 退款单信息 */
  mallPaymentRefundList?: MallPaymentRefund[];
}

/** 退款单配置信息 */
export interface MallPaymentRefund extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 退款单号，格式 REF + 时间戳 + 随机数 */
  refundNo?: string;
  /** 关联支付单 ID */
  paymentId?: string;
  /** 关联订单号 */
  orderNo?: string;
  /** 关联售后单号 */
  afterSaleNo?: string;
  /** 退款用户 ID */
  userId?: string;
  /** 退款金额（单位：分） */
  refundAmount?: string;
  /** 退款原因 */
  refundReason?: string;
  /** 退款渠道编码，必须与原始支付渠道一致 */
  channelCode?: string;
  /** 渠道侧退款单号，对账用 */
  channelRefundNo?: string;
  /** 渠道侧退款状态 */
  channelRefundStatus?: string;
  /** 退款单状态 */
  refundStatus?: string;
  /** 退款成功时间 */
  refundSuccessTime?: string;
  /** 幂等键 */
  idempotentKey?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建人 */
  createBy?: string;
  /** 修改人 */
  updateBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号 */
  version?: string;
}
