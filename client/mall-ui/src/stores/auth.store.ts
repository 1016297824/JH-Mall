import { defineStore } from 'pinia'

import { logout as logoutApi } from '@/api/auth'
import type { TokenInfo } from '@/types'

/** localStorage 持久化 Key，须与 api/client.ts 拦截器保持一致 */
const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    /** 访问令牌（页面刷新后从 localStorage 恢复，保证守卫判断正确） */
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY) ?? '',
    /** 刷新令牌（页面刷新后从 localStorage 恢复） */
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) ?? '',
  }),
  getters: {
    /** 是否已登录 */
    isLoggedIn: (state): boolean => Boolean(state.accessToken),
  },
  actions: {
    /** 写入令牌并持久化（供 axios 拦截器读取） */
    setToken(token: TokenInfo): void {
      this.accessToken = token.accessToken
      this.refreshToken = token.refreshToken
      localStorage.setItem(ACCESS_TOKEN_KEY, token.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, token.refreshToken)
    },
    /** 清空本地登录态，不请求后端 */
    clearAuth(): void {
      this.accessToken = ''
      this.refreshToken = ''
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    },
    /** 登出：后端失败不阻断本地登出，保证用户一定能退出 */
    async logout(): Promise<void> {
      try {
        await logoutApi()
      } catch {
        // 后端作废会话失败（网络/5xx）时以本地登出为准，避免用户被卡在登录态
      } finally {
        this.clearAuth()
      }
    },
  },
})
