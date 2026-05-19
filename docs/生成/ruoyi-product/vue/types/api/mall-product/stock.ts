import type { PageDomain, BaseEntity } from "../common";

/** 库存管理配置分页查询参数 */
export interface StockQueryParams extends PageDomain {
  /** SKU ID，与 SKU 一对一 */
  skuId?: string;
  /** 总库存 = 可用 + 锁定 + 已售 + 冻结 */
  totalStock?: string;
  /** 可用库存 */
  availableStock?: string;
  /** 锁定库存 */
  lockedStock?: string;
  /** 已售库存 */
  soldStock?: string;
  /** 冻结库存 */
  frozenStock?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 乐观锁版本号，库存扣减防超卖 */
  version?: string;
}

/** 库存管理配置信息 */
export interface MallProductSkuStock extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** SKU ID，与 SKU 一对一 */
  skuId?: string;
  /** 总库存 = 可用 + 锁定 + 已售 + 冻结 */
  totalStock?: string;
  /** 可用库存 */
  availableStock?: string;
  /** 锁定库存 */
  lockedStock?: string;
  /** 已售库存 */
  soldStock?: string;
  /** 冻结库存 */
  frozenStock?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 乐观锁版本号，库存扣减防超卖 */
  version?: string;
}
