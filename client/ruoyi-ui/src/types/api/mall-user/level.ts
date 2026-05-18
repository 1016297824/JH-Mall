import type { PageDomain, BaseEntity } from "../common";

/** 会员等级定义配置分页查询参数 */
export interface LevelQueryParams extends PageDomain {
  /** 等级名称，如"普通会员"、"银卡会员"、"金卡会员"、"钻石会员" */
  levelName?: string;
  /** 等级值，数值越大等级越高 */
  levelValue?: string;
  /** 该等级所需的最低成长值 */
  minGrowth?: string;
  /** 该等级的最高成长值 */
  maxGrowth?: string;
  /** 等级图标 URL */
  icon?: string;
  /** 权益 JSON，如折扣率、免邮费、积分倍数 */
  benefitsJson?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 会员等级定义配置信息 */
export interface MallUserMemberLevel extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 等级名称，如"普通会员"、"银卡会员"、"金卡会员"、"钻石会员" */
  levelName?: string;
  /** 等级值，数值越大等级越高 */
  levelValue?: string;
  /** 该等级所需的最低成长值 */
  minGrowth?: string;
  /** 该等级的最高成长值 */
  maxGrowth?: string;
  /** 等级图标 URL */
  icon?: string;
  /** 权益 JSON，如折扣率、免邮费、积分倍数 */
  benefitsJson?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
