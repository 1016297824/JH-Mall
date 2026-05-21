import type { PageDomain, BaseEntity } from "../common";

/** 回调日志配置分页查询参数 */
export interface LogQueryParams extends PageDomain {
  /** 关联支付单号 */
  paymentNo?: string;
  /** 关联退款单号 */
  refundNo?: string;
  /** 渠道编码 */
  channelCode?: string;
  /** 回调类型 */
  callbackType?: string;
  /** 原始回调报文 JSON */
  rawBody?: string;
  /** 验签结果 */
  isVerified?: string;
  /** 处理状态 */
  processStatus?: string;
  /** 处理完成时间 */
  processTime?: string;
  /** 处理结果说明 */
  processResult?: string;
  /** 回调防重放 nonce */
  nonce?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 回调日志配置信息 */
export interface MallPaymentCallbackLog extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 关联支付单号 */
  paymentNo?: string;
  /** 关联退款单号 */
  refundNo?: string;
  /** 渠道编码 */
  channelCode?: string;
  /** 回调类型 */
  callbackType?: string;
  /** 原始回调报文 JSON */
  rawBody?: string;
  /** 验签结果 */
  isVerified?: string;
  /** 处理状态 */
  processStatus?: string;
  /** 处理完成时间 */
  processTime?: string;
  /** 处理结果说明 */
  processResult?: string;
  /** 回调防重放 nonce */
  nonce?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
