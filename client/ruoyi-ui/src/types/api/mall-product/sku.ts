import type { PageDomain, BaseEntity } from "../common";

/** SKU 管理配置分页查询参数 */
export interface SkuQueryParams extends PageDomain {
  /** 所属 SPU ID */
  spuId?: string;
  /** SKU 编码（全局唯一） */
  skuCode?: string;
  /** SKU 销售名称 */
  skuName?: string;
  /** 销售属性 JSON：[{"k":"颜色","v":"蓝色"}] */
  attrsJson?: string;
  /** 销售价（单位：分） */
  price?: string;
  /** 市场价/划线价（单位：分） */
  marketPrice?: string;
  /** 成本价（单位：分） */
  costPrice?: string;
  /** SKU 级图片 */
  image?: string;
  /** 重量（单位：克），用于运费计算 */
  weight?: string;
  /** 该 SKU 累计销量 */
  salesCount?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** SKU 管理配置信息 */
export interface MallProductSku extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 所属 SPU ID */
  spuId?: string;
  /** SKU 编码（全局唯一） */
  skuCode?: string;
  /** SKU 销售名称 */
  skuName?: string;
  /** 销售属性 JSON：[{"k":"颜色","v":"蓝色"}] */
  attrsJson?: string;
  /** 销售价（单位：分） */
  price?: string;
  /** 市场价/划线价（单位：分） */
  marketPrice?: string;
  /** 成本价（单位：分） */
  costPrice?: string;
  /** SKU 级图片 */
  image?: string;
  /** 重量（单位：克），用于运费计算 */
  weight?: string;
  /** 该 SKU 累计销量 */
  salesCount?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
