/** 用户资料（对齐后端 UserProfileVO） */
export interface UserProfile {
  /** 用户 ID */
  userId: string
  /** 昵称（可能未设置） */
  nickname: string | null
  /** 头像 URL（可能未设置） */
  avatar: string | null
  /** 性别：0未知/1男/2女 */
  gender: number | null
  /** 性别名称 */
  genderName: string | null
  /** 生日（可能未设置） */
  birthday: string | null
  /** 手机号（已脱敏） */
  phone: string | null
  /** 邮箱（可能未设置） */
  email: string | null
  /** 会员等级名称 */
  membershipLevel: string | null
  /** 会员等级图标 */
  membershipIcon: string | null
  /** 当前成长值 */
  growth: number
  /** 累计成长值 */
  totalGrowth: number
  /** 当前积分 */
  points: number
  /** 可用积分 */
  availablePoints: number
}
