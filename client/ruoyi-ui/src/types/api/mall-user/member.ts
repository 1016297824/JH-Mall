import type { PageDomain, BaseEntity } from "../common";

/** 用户会员信息配置分页查询参数 */
export interface MemberQueryParams extends PageDomain {
  /** 用户 ID，与 mall_user 一对一 */
  userId?: string;
  /** 当前会员等级 ID，关联 mall_user_member_level */
  levelId?: string;
  /** 当前成长值 */
  growth?: string;
  /** 累计获取成长值，仅增不减 */
  totalGrowth?: string;
  /** 最近一次等级生效时间 */
  levelStartTime?: string;
  /** 等级到期时间 */
  levelEndTime?: string;
  /** 首次成为会员时间 */
  becomeTime?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 用户会员信息配置信息 */
export interface MallUserMember extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 用户 ID，与 mall_user 一对一 */
  userId?: string;
  /** 当前会员等级 ID，关联 mall_user_member_level */
  levelId?: string;
  /** 当前成长值 */
  growth?: string;
  /** 累计获取成长值，仅增不减 */
  totalGrowth?: string;
  /** 最近一次等级生效时间 */
  levelStartTime?: string;
  /** 等级到期时间 */
  levelEndTime?: string;
  /** 首次成为会员时间 */
  becomeTime?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
