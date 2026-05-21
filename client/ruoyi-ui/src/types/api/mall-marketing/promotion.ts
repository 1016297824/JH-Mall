import type { PageDomain, BaseEntity } from "../common";

/** 活动管理配置分页查询参数 */
export interface PromotionQueryParams extends PageDomain {
  /** 活动名称 */
  promotionName?: string;
  /** 活动类型 */
  promotionType?: string;
  /** 活动开始时间 */
  startTime?: string;
  /** 活动结束时间 */
  endTime?: string;
  /** 活动状态 */
  promotionStatus?: string;
  /** 活动描述 */
  description?: string;
  /** 活动 Banner 图 URL */
  bannerImage?: string;
  /** 排序值 */
  sortOrder?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 活动管理配置信息 */
export interface MallMarketingPromotion extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 活动名称 */
  promotionName?: string;
  /** 活动类型 */
  promotionType?: string;
  /** 活动开始时间 */
  startTime?: string;
  /** 活动结束时间 */
  endTime?: string;
  /** 活动状态 */
  promotionStatus?: string;
  /** 活动描述 */
  description?: string;
  /** 活动 Banner 图 URL */
  bannerImage?: string;
  /** 排序值 */
  sortOrder?: string;
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
  /** 促销规则信息 */
  mallMarketingPromotionRuleList?: MallMarketingPromotionRule[];
}

/** 促销规则配置信息 */
export interface MallMarketingPromotionRule extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 关联活动 ID */
  promotionId?: string;
  /** 规则类型 */
  ruleType?: string;
  /** 门槛金额（单位：分） */
  thresholdAmount?: string;
  /** 优惠金额（单位：分），满减专用 */
  benefitAmount?: string;
  /** 折扣率，满折专用，80=8折 */
  benefitRate?: string;
  /** 是否互斥 */
  isExclusive?: string;
  /** 优先级，越小越优先匹配 */
  sortOrder?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
