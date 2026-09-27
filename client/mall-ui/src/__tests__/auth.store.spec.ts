import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

/** mock 登出接口，隔离网络 */
const mocks = vi.hoisted(() => ({
  logoutApi: vi.fn<() => Promise<void>>(),
}))

vi.mock('@/api/auth', () => ({ logout: mocks.logoutApi }))

import { useAuthStore } from '@/stores/auth.store'

/** 测试用 Token 信息 */
const TOKEN = {
  accessToken: 'access-token-abc',
  refreshToken: 'refresh-token-xyz',
  expiresIn: 1800,
}

describe('auth.store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始状态为未登录', () => {
    const store = useAuthStore()

    expect(store.isLoggedIn).toBe(false)
    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
  })

  it('localStorage 已有令牌时新建 store 即为已登录（页面刷新场景）', () => {
    localStorage.setItem('accessToken', TOKEN.accessToken)
    localStorage.setItem('refreshToken', TOKEN.refreshToken)

    const store = useAuthStore()

    expect(store.isLoggedIn).toBe(true)
    expect(store.accessToken).toBe(TOKEN.accessToken)
    expect(store.refreshToken).toBe(TOKEN.refreshToken)
  })

  it('setToken 写入状态并持久化到 localStorage', () => {
    const store = useAuthStore()
    store.setToken(TOKEN)

    expect(store.accessToken).toBe(TOKEN.accessToken)
    expect(store.refreshToken).toBe(TOKEN.refreshToken)
    expect(store.isLoggedIn).toBe(true)
    // client.ts 拦截器直接读写这两个 key，必须保持一致
    expect(localStorage.getItem('accessToken')).toBe(TOKEN.accessToken)
    expect(localStorage.getItem('refreshToken')).toBe(TOKEN.refreshToken)
  })

  it('clearAuth 清空状态与 localStorage', () => {
    const store = useAuthStore()
    store.setToken(TOKEN)
    store.clearAuth()

    expect(store.isLoggedIn).toBe(false)
    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('logout 成功后清空本地登录态', async () => {
    mocks.logoutApi.mockResolvedValue(undefined)
    const store = useAuthStore()
    store.setToken(TOKEN)

    await store.logout()

    expect(mocks.logoutApi).toHaveBeenCalledTimes(1)
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('后端登出失败时仍清空本地登录态且不抛异常', async () => {
    mocks.logoutApi.mockRejectedValue(new Error('network down'))
    const store = useAuthStore()
    store.setToken(TOKEN)

    await expect(store.logout()).resolves.toBeUndefined()

    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })
})
