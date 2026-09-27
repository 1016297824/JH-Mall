import request from './client'
import type {
  CaptchaResp,
  LoginReq,
  RegisterReq,
  ResetPasswordReq,
  SessionInfo,
  TokenInfo,
} from '@/types'

/** 获取图形验证码 */
export function getCaptcha(): Promise<CaptchaResp> {
  return request.get('/auth/captcha').then((res) => res.data.data)
}

/** 验证码 + 密码登录 */
export function loginByCaptcha(req: LoginReq): Promise<TokenInfo> {
  return request.post('/auth/captcha/login', req).then((res) => res.data.data)
}

/** 验证码注册 */
export function registerByCaptcha(req: RegisterReq): Promise<TokenInfo> {
  return request.post('/auth/captcha/register', req).then((res) => res.data.data)
}

/** 验证码重置密码 */
export function resetPasswordByCaptcha(req: ResetPasswordReq): Promise<void> {
  return request.post('/auth/captcha/password/reset', req).then((res) => res.data.data)
}

/** 刷新 Token（旧 refreshToken 一次性作废） */
export function refreshSession(refreshToken: string): Promise<TokenInfo> {
  return request.post('/auth/sessions/refresh', { refreshToken }).then((res) => res.data.data)
}

/** 校验当前会话，返回登录用户 ID */
export function getCurrentSession(): Promise<SessionInfo> {
  return request.get('/auth/sessions/current').then((res) => res.data.data)
}

/** 登出当前会话 */
export function logout(): Promise<void> {
  return request.delete('/auth/sessions/current').then((res) => res.data.data)
}
