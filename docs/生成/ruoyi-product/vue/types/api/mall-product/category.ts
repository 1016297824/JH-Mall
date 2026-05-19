import type { PageDomain, BaseEntity } from "../common";

/** 商品类目配置分页查询参数 */
export interface CategoryQueryParams extends PageDomain {
  /** 父类目 ID，0 表示顶级类目 */
  parentId?: string;
  /** 类目名称 */
  name?: string;
  /** 类目层级 */
  level?: string;
  /** 类目标识图标 URL */
  icon?: string;
  /** 排序值，越小越靠前 */
  sortOrder?: string;
  /** 是否前端可见 */
  isVisible?: string;
  /** 类目路径，如 /1/10/100 */
  path?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 商品类目配置信息 */
export interface MallProductCategory extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 父类目 ID，0 表示顶级类目 */
  parentId?: string;
  /** 类目名称 */
  name?: string;
  /** 类目层级 */
  level?: string;
  /** 类目标识图标 URL */
  icon?: string;
  /** 排序值，越小越靠前 */
  sortOrder?: string;
  /** 是否前端可见 */
  isVisible?: string;
  /** 类目路径，如 /1/10/100 */
  path?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
