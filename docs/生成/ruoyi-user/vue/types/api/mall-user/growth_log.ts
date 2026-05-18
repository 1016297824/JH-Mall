import type { PageDomain, BaseEntity } from "../common";

/** 成长值流水配置分页查询参数 */
export interface Growth_logQueryParams extends PageDomain {
  /** 用户 ID */
  userId?: string;
  /** 业务类型：order（下单）/ signin（签到）/ refund（退款）/ admin（管理员调整） */
  bizType?: string;
  /** 业务单号 */
  bizNo?: string;
  /** 变动方向：1=增加 2=减少 */
  changeType?: string;
  /** 本次变动成长值 */
  growth?: string;
  /** 变动前成长值余额 */
  beforeGrowth?: string;
  /** 变动后成长值余额 */
  afterGrowth?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 成长值流水配置信息 */
export interface MallUserGrowthLog extends BaseEntity {
  /** 主键，自增 */
  id?: string;
  /** 用户 ID */
  userId?: string;
  /** 业务类型：order（下单）/ signin（签到）/ refund（退款）/ admin（管理员调整） */
  bizType?: string;
  /** 业务单号 */
  bizNo?: string;
  /** 变动方向：1=增加 2=减少 */
  changeType?: string;
  /** 本次变动成长值 */
  growth?: string;
  /** 变动前成长值余额 */
  beforeGrowth?: string;
  /** 变动后成长值余额 */
  afterGrowth?: string;
  /** 变动原因说明 */
  remark?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
