import type { Router } from 'vue-router'

import { useAuthStore } from '@/stores/auth.store'
import { LOGIN_PATH } from '@/utils/constants'

/** 已登录用户访问登录页时的回跳路径 */
const HOME_PATH = '/'

/**
 * 注册全局导航守卫
 *
 * <p>受保护路由由路由表中的 `meta.requiresAuth` 标记；未登录访问时携带
 * `redirect` 跳转登录页，登录成功后可原路返回。</p>
 *
 * @param router 路由实例
 */
export function registerGuards(router: Router): void {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
      return { path: LOGIN_PATH, query: { redirect: to.fullPath } }
    }

    if (to.path === LOGIN_PATH && authStore.isLoggedIn) {
      return { path: HOME_PATH }
    }

    return true
  })
}
