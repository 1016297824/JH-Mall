import axios, {
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'

import { LOGIN_PATH, SUCCESS_CODE, UNAUTHORIZED_CODE } from '@/utils/constants'

/** 后端统一响应体；网关鉴权失败时使用 {code, msg} 格式 */
interface BackendBody {
  /** 业务错误码（MallResult） */
  errorCode?: string
  /** 技术错误信息 */
  errorMessage?: string
  /** 面向用户的提示 */
  userTip?: string
  /** 网关错误码 */
  code?: number
  /** 网关错误信息 */
  msg?: string
  /** 业务数据 */
  data?: unknown
}

/** 带重试标记的请求配置 */
type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 提取面向用户的错误文案
 *
 * <p>优先 userTip（后端面向用户的提示），其次 errorMessage 与网关 msg。</p>
 *
 * @param body 响应体
 * @param fallback 兜底文案
 * @returns 展示给用户的错误文案
 */
function resolveErrorMessage(body: BackendBody | undefined, fallback: string): string {
  const candidates = [body?.userTip, body?.errorMessage, body?.msg]
  return candidates.find((item) => item != null && item !== '') ?? fallback
}

/**
 * 是否为未授权响应
 *
 * <p>网关鉴权失败会以 HTTP 200 + `{code:401}` 返回，与真实 HTTP 401 需同等处理。</p>
 *
 * @param body 响应体
 * @param status HTTP 状态码
 * @returns 是否未授权
 */
function isUnauthorized(body: BackendBody | undefined, status: number | undefined): boolean {
  return status === UNAUTHORIZED_CODE || body?.code === UNAUTHORIZED_CODE
}

/** 清除本地令牌并跳转登录页 */
function redirectToLogin(): void {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  window.location.href = LOGIN_PATH
}

/**
 * 统一弹出接口错误提示
 *
 * <p>规范要求「接口错误统一在响应拦截器中处理」，各页面与 store 因此只需
 * 上抛异常、无需重复写提示。`grouping` 让并发失败产生的相同文案合并为一条，
 * 避免一次性刷屏。</p>
 *
 * @param message 展示给用户的文案
 */
function toastError(message: string): void {
  ElMessage.error({ message, grouping: true })
}

/** 进行中的刷新请求：refreshToken 为一次性轮换，并发 401 必须共享同一次刷新 */
let refreshInFlight: Promise<string> | null = null

/**
 * 用 refreshToken 换取新的 accessToken
 *
 * <p>请求地址复用 axios 实例的 baseURL，避免生产环境写死相对路径而打到静态资源站。</p>
 *
 * @returns 新的 accessToken
 */
async function doRefreshToken(): Promise<string> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    throw new Error('缺少 refreshToken')
  }
  const res = await axios.post(`${request.defaults.baseURL ?? '/api'}/auth/sessions/refresh`, {
    refreshToken,
  })
  const { accessToken, refreshToken: newRefreshToken } = res.data.data
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', newRefreshToken)
  return accessToken
}

/**
 * 获取新的 accessToken（并发调用共享同一次刷新）
 *
 * @returns 新的 accessToken
 */
function fetchNewAccessToken(): Promise<string> {
  refreshInFlight ??= doRefreshToken().finally(() => {
    // 清空必须挂在真正返回的 Promise 上：若放在 doRefreshToken 内部，
    // 同步失败时会先清空、再被调用方赋值为已 reject 的 Promise，
    // 导致缓存永久保留失败结果、后续刷新全部失败
    refreshInFlight = null
  })
  return refreshInFlight
}

/**
 * 换新令牌并重放原请求；失败则清理登录态
 *
 * @param config 原始请求配置
 * @param message 刷新失败时抛出的文案
 * @returns 重放后的响应
 */
async function refreshAndRetry(config: RetryableConfig, message: string): Promise<AxiosResponse> {
  config._retry = true
  let accessToken: string
  try {
    accessToken = await fetchNewAccessToken()
  } catch (error) {
    redirectToLogin()
    throw new Error(message, { cause: error })
  }
  config.headers.Authorization = `Bearer ${accessToken}`
  return await request(config)
}

request.interceptors.response.use(
  (response) => {
    const body = response.data as BackendBody
    if (isUnauthorized(body, response.status)) {
      const config = response.config as RetryableConfig
      // 重试后仍未授权：说明刷新无法解决问题，直接登出，避免无限刷新重放
      if (config._retry) {
        redirectToLogin()
        return Promise.reject(new Error(resolveErrorMessage(body, '登录状态已失效')))
      }
      return refreshAndRetry(config, resolveErrorMessage(body, '登录状态已失效'))
    }
    // 网关其他错误体（如 {code:500,msg:"服务未找到"}）：放行会让调用方拿到 undefined 并静默渲染空数据
    if (body?.code !== undefined) {
      const message = resolveErrorMessage(body, '请求失败')
      toastError(message)
      return Promise.reject(new Error(message))
    }
    if (body?.errorCode !== undefined && body.errorCode !== SUCCESS_CODE) {
      const message = resolveErrorMessage(body, '请求失败')
      toastError(message)
      return Promise.reject(new Error(message))
    }
    return response
  },
  async (error: AxiosError<BackendBody>) => {
    const config = error.config as RetryableConfig | undefined
    const body = error.response?.data
    const message = resolveErrorMessage(body, error.message)
    if (config && !config._retry && isUnauthorized(body, error.response?.status)) {
      return refreshAndRetry(config, message)
    }
    // 业务错误：后端以 HTTP 400 返回 MallResult，需取 userTip/errorMessage 而非 axios 文案
    if (body && (body.errorCode !== undefined || body.errorMessage || body.userTip)) {
      toastError(message)
      return Promise.reject(new Error(message))
    }
    // 网络层错误（超时、断网、服务未启动），axios 文案已是唯一线索
    toastError(message)
    return Promise.reject(error)
  },
)

export default request
