import type { PageDomain, BaseEntity } from "../common";

/** 订单管理配置分页查询参数 */
export interface OrderQueryParams extends PageDomain {
  /** 订单号，格式 JH + 时间戳 + 随机数 */
  orderNo?: string;
  /** 用户 ID */
  userId?: string;
  /** 订单状态 */
  orderStatus?: string;
  /** 商品总金额（单位：分） */
  totalAmount?: string;
  /** 优惠总金额（单位：分） */
  discountAmount?: string;
  /** 运费金额（单位：分） */
  freightAmount?: string;
  /** 实付金额（单位：分） */
  payAmount?: string;
  /** 支付成功时间 */
  payTime?: string;
  /** 发货时间 */
  deliveryTime?: string;
  /** 交易完成时间 */
  completeTime?: string;
  /** 取消时间 */
  cancelTime?: string;
  /** 取消类型 */
  cancelType?: string;
  /** 取消原因 */
  cancelReason?: string;
  /** 支付过期时间，默认创建后 30 分钟 */
  payExpireTime?: string;
  /** 幂等键 */
  idempotentKey?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 乐观锁版本号 */
  version?: string;
}

/** 订单管理配置信息 */
export interface MallOrder extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 订单号，格式 JH + 时间戳 + 随机数 */
  orderNo?: string;
  /** 用户 ID */
  userId?: string;
  /** 订单状态 */
  orderStatus?: string;
  /** 商品总金额（单位：分） */
  totalAmount?: string;
  /** 优惠总金额（单位：分） */
  discountAmount?: string;
  /** 运费金额（单位：分） */
  freightAmount?: string;
  /** 实付金额（单位：分） */
  payAmount?: string;
  /** 支付成功时间 */
  payTime?: string;
  /** 发货时间 */
  deliveryTime?: string;
  /** 交易完成时间 */
  completeTime?: string;
  /** 取消时间 */
  cancelTime?: string;
  /** 取消类型 */
  cancelType?: string;
  /** 取消原因 */
  cancelReason?: string;
  /** 支付过期时间，默认创建后 30 分钟 */
  payExpireTime?: string;
  /** 买家备注 */
  remark?: string;
  /** 幂等键 */
  idempotentKey?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建人（管理员代下单时记录） */
  createBy?: string;
  /** 修改人 */
  updateBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号 */
  version?: string;
  /** 订单项信息 */
  mallOrderItemList?: MallOrderItem[];
}

/** 订单项配置信息 */
export interface MallOrderItem extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 订单 ID */
  orderId?: string;
  /** SPU ID（快照） */
  spuId?: string;
  /** SKU ID（快照） */
  skuId?: string;
  /** SKU 编码（快照） */
  skuCode?: string;
  /** SKU 名称（快照） */
  skuName?: string;
  /** SPU 名称（快照） */
  spuName?: string;
  /** 商品主图快照 URL */
  mainImage?: string;
  /** 销售属性 JSON 快照 */
  attrsJson?: string;
  /** 购买数量 */
  quantity?: string;
  /** 成交单价（单位：分） */
  price?: string;
  /** 单项总价（单位：分） */
  totalPrice?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
