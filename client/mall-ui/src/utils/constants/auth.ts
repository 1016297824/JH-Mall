/** 手机号正则 */
export const PHONE_PATTERN = /^1[3-9]\d{9}$/

/** 密码正则：必须同时包含字母和数字，须与后端 CaptchaController.PASSWORD_PATTERN 保持一致 */
export const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d).+$/

/** 后端业务成功码 */
export const SUCCESS_CODE = '00000'

/** HTTP 未授权状态码（网关亦以 HTTP 200 + code=401 返回） */
export const UNAUTHORIZED_CODE = 401

/** 登录页路径 */
export const LOGIN_PATH = '/login'
