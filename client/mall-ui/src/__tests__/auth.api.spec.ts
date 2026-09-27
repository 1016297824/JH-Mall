import { beforeEach, describe, expect, it, vi } from 'vitest'

/** mock axios 实例，隔离网络 */
const mocks = vi.hoisted(() => ({
  get: vi.fn<(url: string) => Promise<{ data: { data: unknown } }>>(),
  post: vi.fn<(url: string, body?: unknown) => Promise<{ data: { data: unknown } }>>(),
  delete: vi.fn<(url: string) => Promise<{ data: { data: unknown } }>>(),
}))

vi.mock('@/api/client', () => ({ default: mocks }))

import {
  getCaptcha,
  getCurrentSession,
  loginByCaptcha,
  logout,
  refreshSession,
  registerByCaptcha,
  resetPasswordByCaptcha,
} from '@/api/auth'

describe('api/auth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.get.mockResolvedValue({ data: { data: null } })
    mocks.post.mockResolvedValue({ data: { data: null } })
    mocks.delete.mockResolvedValue({ data: { data: null } })
  })

  it('getCaptcha 请求 GET /auth/captcha 并解包 data', async () => {
    mocks.get.mockResolvedValue({
      data: { data: { captchaKey: 'k1', captchaImage: 'base64' } },
    })

    const result = await getCaptcha()

    expect(mocks.get).toHaveBeenCalledWith('/auth/captcha')
    expect(result).toEqual({ captchaKey: 'k1', captchaImage: 'base64' })
  })

  it('loginByCaptcha 以请求体 POST /auth/captcha/login', async () => {
    const req = {
      phone: '13800000000',
      password: 'pass1234',
      captchaKey: 'k1',
      captchaCode: '8888',
    }

    await loginByCaptcha(req)

    expect(mocks.post).toHaveBeenCalledWith('/auth/captcha/login', req)
  })

  it('registerByCaptcha 传递隐私协议标记', async () => {
    const req = {
      phone: '13800000000',
      password: 'pass1234',
      captchaKey: 'k1',
      captchaCode: '8888',
      isPrivacyAgreed: true,
    }

    await registerByCaptcha(req)

    expect(mocks.post).toHaveBeenCalledWith('/auth/captcha/register', req)
  })

  it('resetPasswordByCaptcha 使用 newPassword 字段', async () => {
    const req = {
      phone: '13800000000',
      newPassword: 'newpass1234',
      captchaKey: 'k1',
      captchaCode: '8888',
    }

    await resetPasswordByCaptcha(req)

    expect(mocks.post).toHaveBeenCalledWith('/auth/captcha/password/reset', req)
  })

  it('refreshSession 包装 refreshToken 字段', async () => {
    await refreshSession('refresh-token-xyz')

    expect(mocks.post).toHaveBeenCalledWith('/auth/sessions/refresh', {
      refreshToken: 'refresh-token-xyz',
    })
  })

  it('getCurrentSession 请求 GET /auth/sessions/current', async () => {
    await getCurrentSession()

    expect(mocks.get).toHaveBeenCalledWith('/auth/sessions/current')
  })

  it('logout 请求 DELETE /auth/sessions/current', async () => {
    await logout()

    expect(mocks.delete).toHaveBeenCalledWith('/auth/sessions/current')
  })
})
