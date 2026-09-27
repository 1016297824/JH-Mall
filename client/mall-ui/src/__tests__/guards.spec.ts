import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'

import { registerGuards } from '@/router/guards'
import { useAuthStore } from '@/stores/auth.store'

/** 构造带守卫的最小测试路由 */
function createTestRouter(): Router {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      {
        path: '/cart',
        name: 'cart',
        component: { template: '<div />' },
        meta: { requiresAuth: true },
      },
    ],
  })
  registerGuards(router)
  return router
}

/** 让 store 处于已登录状态 */
function login(): void {
  useAuthStore().setToken({
    accessToken: 'access-token-abc',
    refreshToken: 'refresh-token-xyz',
    expiresIn: 1800,
  })
}

describe('router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('未登录访问受保护路由时重定向到登录页并保留 redirect', async () => {
    const router = createTestRouter()
    await router.push('/cart')

    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/cart')
  })

  it('已登录访问受保护路由时放行', async () => {
    login()
    const router = createTestRouter()
    await router.push('/cart')

    expect(router.currentRoute.value.path).toBe('/cart')
  })

  it('未登录可正常访问公开路由', async () => {
    const router = createTestRouter()
    await router.push('/')

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('已登录访问登录页时跳回首页', async () => {
    login()
    const router = createTestRouter()
    await router.push('/login')

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('页面刷新后（localStorage 已有令牌）新建 store 视为已登录并放行', async () => {
    localStorage.setItem('accessToken', 'access-token-abc')
    localStorage.setItem('refreshToken', 'refresh-token-xyz')

    const router = createTestRouter()
    await router.push('/cart')

    expect(router.currentRoute.value.path).toBe('/cart')
  })
})
