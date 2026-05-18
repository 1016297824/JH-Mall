import type { PageDomain, BaseEntity } from "../common";

/** 积分账户配置分页查询参数 */
export interface AccountQueryParams extends PageDomain {
  /** 用户 ID，与用户一对一 */
  userId?: string;
  /** 累计获取积分，仅增不减 */
  totalPoints?: string;
  /** 可用积分余额 */
  availablePoints?: string;
  /** 已使用积分 */
  usedPoints?: string;
  /** 已过期积分 */
  expiredPoints?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 积分账户配置信息 */
export interface MallUserPointsAccount extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 用户 ID，与用户一对一 */
  userId?: string;
  /** 累计获取积分，仅增不减 */
  totalPoints?: string;
  /** 可用积分余额 */
  availablePoints?: string;
  /** 已使用积分 */
  usedPoints?: string;
  /** 已过期积分 */
  expiredPoints?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
