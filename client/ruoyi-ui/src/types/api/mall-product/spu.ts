import type { PageDomain, BaseEntity } from "../common";

/** SPU 管理配置分页查询参数 */
export interface SpuQueryParams extends PageDomain {
  /** 所属三级类目 ID */
  categoryId?: string;
  /** 所属品牌 ID */
  brandId?: string;
  /** SPU 名称（商品标题） */
  spuName?: string;
  /** 商品详情描述（富文本 HTML） */
  spuDescription?: string;
  /** 商品主图 URL */
  mainImage?: string;
  /** 轮播图 JSON 数组 */
  imagesJson?: string;
  /** 最低销售价（单位：分） */
  priceMin?: string;
  /** 最高销售价（单位：分） */
  priceMax?: string;
  /** 累计销量 */
  salesCount?: string;
  /** 评价条数 */
  reviewCount?: string;
  /** 上下架状态 */
  publishStatus?: string;
  /** 审核状态 */
  verifyStatus?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 乐观锁版本号 */
  version?: string;
}

/** SPU 管理配置信息 */
export interface MallProductSpu extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 所属三级类目 ID */
  categoryId?: string;
  /** 所属品牌 ID */
  brandId?: string;
  /** SPU 名称（商品标题） */
  spuName?: string;
  /** 商品详情描述（富文本 HTML） */
  spuDescription?: string;
  /** 商品主图 URL */
  mainImage?: string;
  /** 轮播图 JSON 数组 */
  imagesJson?: string;
  /** 最低销售价（单位：分） */
  priceMin?: string;
  /** 最高销售价（单位：分） */
  priceMax?: string;
  /** 累计销量 */
  salesCount?: string;
  /** 评价条数 */
  reviewCount?: string;
  /** 上下架状态 */
  publishStatus?: string;
  /** 审核状态 */
  verifyStatus?: string;
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
  /** SKU 管理信息 */
  mallProductSkuList?: MallProductSku[];
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
