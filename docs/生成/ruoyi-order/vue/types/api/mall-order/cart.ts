import type { PageDomain, BaseEntity } from "../common";

/** 购物车配置分页查询参数 */
export interface CartQueryParams extends PageDomain {
  /** 用户 ID */
  userId?: string;
  /** SKU ID */
  skuId?: string;
  /** SPU ID */
  spuId?: string;
  /** SKU 编码（冗余，快速展示） */
  skuCode?: string;
  /** SKU 销售名称（冗余） */
  skuName?: string;
  /** 商品主图 URL（冗余） */
  mainImage?: string;
  /** 当前销售价（单位：分），加购时快照 */
  price?: string;
  /** 加入数量 */
  quantity?: string;
  /** 是否选中 */
  isSelected?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 购物车配置信息 */
export interface MallOrderCart extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 用户 ID */
  userId?: string;
  /** SKU ID */
  skuId?: string;
  /** SPU ID */
  spuId?: string;
  /** SKU 编码（冗余，快速展示） */
  skuCode?: string;
  /** SKU 销售名称（冗余） */
  skuName?: string;
  /** 商品主图 URL（冗余） */
  mainImage?: string;
  /** 当前销售价（单位：分），加购时快照 */
  price?: string;
  /** 加入数量 */
  quantity?: string;
  /** 是否选中 */
  isSelected?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
