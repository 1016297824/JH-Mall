import type { PageDomain, BaseEntity } from "../common";

/** 品牌管理配置分页查询参数 */
export interface BrandQueryParams extends PageDomain {
  /** 品牌名称 */
  name?: string;
  /** 品牌 Logo URL */
  logo?: string;
  /** 品牌简介 */
  description?: string;
  /** 排序值 */
  sortOrder?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 品牌管理配置信息 */
export interface MallProductBrand extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 品牌名称 */
  name?: string;
  /** 品牌 Logo URL */
  logo?: string;
  /** 品牌简介 */
  description?: string;
  /** 排序值 */
  sortOrder?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
