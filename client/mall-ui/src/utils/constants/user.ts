/**
 * 用户域常量
 *
 * <p>取值必须与后端保持一致：积分流水字段见 {@code PointsRecordVO}。</p>
 */

/** 积分变更类型：1 收入 / 2 支出 */
export const POINTS_CHANGE_TYPE = {
  /** 收入 */
  INCOME: 1,
  /** 支出 */
  EXPENSE: 2,
} as const

/** 变更类型 → 中文描述 */
export const POINTS_CHANGE_TYPE_TEXT: Record<number, string> = {
  [POINTS_CHANGE_TYPE.INCOME]: '收入',
  [POINTS_CHANGE_TYPE.EXPENSE]: '支出',
}

/** 积分流水分页大小 */
export const POINTS_PAGE_SIZE = 20
