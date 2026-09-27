import { beforeEach, describe, expect, it, vi } from 'vitest'

/** mock 验证码接口，隔离网络 */
const mocks = vi.hoisted(() => ({
  getCaptcha: vi.fn<() => Promise<{ captchaKey: string; captchaImage: string }>>(),
}))

vi.mock('@/api/auth', () => ({ getCaptcha: mocks.getCaptcha }))

import { useCaptcha } from '@/composables/auth/useCaptcha'

describe('useCaptcha', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('refresh 后暴露 captchaKey，并为纯 base64 补 data URI 前缀', async () => {
    mocks.getCaptcha.mockResolvedValue({ captchaKey: 'k1', captchaImage: 'AAAA' })

    const captcha = useCaptcha()
    await captcha.refresh()

    expect(captcha.captchaKey.value).toBe('k1')
    expect(captcha.hasImage.value).toBe(true)
    expect(captcha.imageSrc.value).toBe('data:image/png;base64,AAAA')
    expect(captcha.loading.value).toBe(false)
  })

  it('后端已返回 data URI 时不重复添加前缀', async () => {
    mocks.getCaptcha.mockResolvedValue({
      captchaKey: 'k2',
      captchaImage: 'data:image/png;base64,BBBB',
    })

    const captcha = useCaptcha()
    await captcha.refresh()

    expect(captcha.imageSrc.value).toBe('data:image/png;base64,BBBB')
  })

  it('获取失败时清空图片并向外收敛异常（页面可继续重试）', async () => {
    mocks.getCaptcha.mockRejectedValue(new Error('boom'))

    const captcha = useCaptcha()
    await expect(captcha.refresh()).resolves.toBeUndefined()

    expect(captcha.hasImage.value).toBe(false)
    expect(captcha.loading.value).toBe(false)
  })
})
