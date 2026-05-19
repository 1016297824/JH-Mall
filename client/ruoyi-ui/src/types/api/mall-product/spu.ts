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
}
