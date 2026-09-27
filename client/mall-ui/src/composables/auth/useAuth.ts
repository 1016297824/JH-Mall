import { computed, type ComputedRef } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth.store'
import { LOGIN_PATH } from '@/utils/constants'

/** useAuth 返回值 */
export interface UseAuthReturn {
  /** 是否已登录 */
  isLoggedIn: ComputedRef<boolean>
  /** 登出并跳转登录页 */
  logout: () => Promise<void>
}

/**
 * 认证状态与操作
 *
 * @returns 登录态与登出方法
 */
export function useAuth(): UseAuthReturn {
  const authStore = useAuthStore()
  const router = useRouter()

  const isLoggedIn = computed(() => authStore.isLoggedIn)

  /** 登出：本地令牌由 store 清理，这里只负责跳转 */
  async function logout(): Promise<void> {
    await authStore.logout()
    await router.push({ path: LOGIN_PATH })
  }

  return { isLoggedIn, logout }
}
