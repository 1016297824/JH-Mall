import type { PageDomain, BaseEntity } from "../common";

/** 积分流水配置分页查询参数 */
export interface Points_logQueryParams extends PageDomain {
  /** 用户 ID */
  userId?: string;
  /** 业务类型：order（下单）/ signin（签到）/ refund（退款）/ admin（管理员调整） */
  bizType?: string;
  /** 业务单号 */
  bizNo?: string;
  /** 变动方向：1=增加 2=减少 */
  changeType?: string;
  /** 本次变动积分值 */
  points?: string;
  /** 变动前积分余额 */
  beforePoints?: string;
  /** 变动后积分余额 */
  afterPoints?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
}

/** 积分流水配置信息 */
export interface MallUserPointsLog extends BaseEntity {
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
  /** 本次变动积分值 */
  points?: string;
  /** 变动前积分余额 */
  beforePoints?: string;
  /** 变动后积分余额 */
  afterPoints?: string;
  /** 变动原因说明 */
  remark?: string;
  /** 逻辑删除标志 */
  isDeleted?: string;
  /** 创建时间 */
  createTime?: string;
  /** 修改时间 */
  updateTime?: string;
}
