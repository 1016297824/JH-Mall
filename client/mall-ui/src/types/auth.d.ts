/** 令牌信息（登录/注册/刷新统一响应） */
export interface TokenInfo {
  /** 访问令牌 */
  accessToken: string
  /** 刷新令牌 */
  refreshToken: string
  /** 访问令牌有效期（秒） */
  expiresIn: number
}

/** 图形验证码 */
export interface CaptchaResp {
  /** 验证码 Key，提交时回传 */
  captchaKey: string
  /** 验证码图片 Base64 */
  captchaImage: string
}

/** 验证码登录请求 */
export interface LoginReq {
  /** 手机号 */
  phone: string
  /** 密码 */
  password: string
  /** 验证码 Key */
  captchaKey: string
  /** 用户输入的验证码 */
  captchaCode: string
}

/** 验证码注册请求 */
export interface RegisterReq extends LoginReq {
  /** 是否同意隐私协议 */
  isPrivacyAgreed: boolean
}

/** 验证码重置密码请求 */
export interface ResetPasswordReq {
  /** 手机号 */
  phone: string
  /** 新密码（8-32 位，须含字母与数字） */
  newPassword: string
  /** 验证码 Key */
  captchaKey: string
  /** 用户输入的验证码 */
  captchaCode: string
}

/** 会话信息 */
export interface SessionInfo {
  /** 当前登录用户 ID */
  userId: string
}
