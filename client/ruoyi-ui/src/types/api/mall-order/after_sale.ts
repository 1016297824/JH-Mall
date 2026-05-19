import type { PageDomain, BaseEntity } from "../common";

/** 售后管理配置分页查询参数 */
export interface After_saleQueryParams extends PageDomain {
  /** 售后单号 */
  afterSaleNo?: string;
  /** 关联订单 ID */
  orderId?: string;
  /** 关联订单项 ID，为空则整单退款 */
  orderItemId?: string;
  /** 申请人用户 ID */
  userId?: string;
  /** 售后类型 */
  afterSaleType?: string;
  /** 退款原因 */
  reason?: string;
  /** 退款金额（单位：分） */
  amount?: string;
  /** 售后状态 */
  afterSaleStatus?: string;
  /** 申请时间 */
  applyTime?: string;
  /** 审核时间 */
  approveTime?: string;
  /** 审核意见 */
  approveRemark?: string;
  /** 退货物流公司 */
  returnExpressCompany?: string;
  /** 退货物流单号 */
  returnExpressNo?: string;
  /** 商家确认收货时间 */
  receiptTime?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 售后管理配置信息 */
export interface MallOrderAfterSale extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 售后单号 */
  afterSaleNo?: string;
  /** 关联订单 ID */
  orderId?: string;
  /** 关联订单项 ID，为空则整单退款 */
  orderItemId?: string;
  /** 申请人用户 ID */
  userId?: string;
  /** 售后类型 */
  afterSaleType?: string;
  /** 退款原因 */
  reason?: string;
  /** 退款金额（单位：分） */
  amount?: string;
  /** 售后状态 */
  afterSaleStatus?: string;
  /** 申请时间 */
  applyTime?: string;
  /** 审核时间 */
  approveTime?: string;
  /** 审核意见 */
  approveRemark?: string;
  /** 退货物流公司 */
  returnExpressCompany?: string;
  /** 退货物流单号 */
  returnExpressNo?: string;
  /** 商家确认收货时间 */
  receiptTime?: string;
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
}
