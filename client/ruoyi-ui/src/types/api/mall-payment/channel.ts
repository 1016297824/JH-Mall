import type { PageDomain, BaseEntity } from "../common";

/** 支付渠道配置分页查询参数 */
export interface ChannelQueryParams extends PageDomain {
  /** 渠道编码 */
  channelCode?: string;
  /** 渠道展示名称 */
  channelName?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 渠道配置 */
  configJson?: string;
  /** 是否启用 */
  isEnabled?: string;
  /** 排序值 */
  sortOrder?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 支付渠道配置信息 */
export interface MallPaymentChannel extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 渠道编码 */
  channelCode?: string;
  /** 渠道展示名称 */
  channelName?: string;
  /** 渠道类型 */
  channelType?: string;
  /** 渠道配置 */
  configJson?: string;
  /** 是否启用 */
  isEnabled?: string;
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
}
