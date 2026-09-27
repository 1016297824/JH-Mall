import { test, expect, type Page } from '@playwright/test'

/**
 * C 端认证闭环端到端测试
 *
 * <p>在真实浏览器中驱动真实 Vue 应用，按后端真实契约模拟 `/api/auth/**`。
 * 覆盖：未登录拦截 → 登录 → 令牌持久化 → 个人中心 → 登出，以及表单校验与一次性验证码刷新。</p>
 */

/** 1×1 透明 PNG，充当后端返回的图片验证码 */
const CAPTCHA_IMAGE =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg=='

/** 后端统一响应体 MallResult */
function mallResult(data: unknown, errorCode = '00000', errorMessage = '') {
  return { errorCode, errorMessage, data }
}

interface MockOptions {
  /** 登录接口是否返回业务错误 */
  loginFails?: boolean
  /** 验证码请求计数（用于断言一次性验证码被重新获取） */
  captchaCounter?: { count: number }
}

/** 按后端真实契约模拟认证相关接口 */
async function mockAuthApi(page: Page, options: MockOptions = {}): Promise<void> {
  const { loginFails = false, captchaCounter } = options

  // 只拦截真正的后端 API：不能写成 glob '**/api/**'，否则会命中前端源码模块（如 /src/api/client.ts）
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const { pathname } = new URL(route.request().url())
    const method = route.request().method()

    if (pathname === '/api/auth/captcha' && method === 'GET') {
      if (captchaCounter) captchaCounter.count += 1
      await route.fulfill({
        json: mallResult({
          captchaKey: `key-${captchaCounter?.count ?? 1}`,
          captchaImage: CAPTCHA_IMAGE,
        }),
      })
      return
    }

    if (pathname === '/api/auth/captcha/login' && method === 'POST') {
      await route.fulfill({
        json: loginFails
          ? mallResult(null, 'A0210', '手机号或密码错误')
          : mallResult({
              accessToken: 'e2e-access-token',
              refreshToken: 'e2e-refresh-token',
              expiresIn: 1800,
            }),
      })
      return
    }

    if (pathname === '/api/auth/captcha/register' && method === 'POST') {
      await route.fulfill({
        json: mallResult({
          accessToken: 'e2e-access-token',
          refreshToken: 'e2e-refresh-token',
          expiresIn: 1800,
        }),
      })
      return
    }

    if (pathname === '/api/user/profile' && method === 'GET') {
      await route.fulfill({
        json: mallResult({
          userId: '1001',
          nickname: '测试用户',
          avatar: null,
          gender: 1,
          genderName: '男',
          birthday: null,
          phone: '138****0000',
          email: null,
          membershipLevel: '黄金会员',
          membershipIcon: null,
          growth: 1200,
          totalGrowth: 1500,
          points: 300,
          availablePoints: 280,
        }),
      })
      return
    }

    if (pathname === '/api/auth/sessions/current' && method === 'DELETE') {
      await route.fulfill({ json: mallResult(null) })
      return
    }

    // 其余接口（首页商品等）统一返回空数组，避免干扰
    await route.fulfill({ json: mallResult([]) })
  })
}

/** 每个用例开始前清空本地令牌（仅在首次导航时执行，避免后续 goto 清掉刚写入的 token） */
test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    if (!sessionStorage.getItem('__e2e_initialized')) {
      localStorage.clear()
      sessionStorage.setItem('__e2e_initialized', '1')
    }
  })
})

/** 填登录表单并提交 */
async function submitLogin(page: Page, phone: string, password: string): Promise<void> {
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByPlaceholder('请输入验证码').fill('8888')
  await page.getByRole('button', { name: '登录' }).click()
}

test('未登录访问受保护页时重定向到登录页并保留 redirect', async ({ page }) => {
  await mockAuthApi(page)

  await page.goto('/profile')

  await expect(page).toHaveURL(/\/login/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe('/profile')
  await expect(page.locator('.login-page__title')).toHaveText('欢迎回来')
})

test('登录页加载图形验证码', async ({ page }) => {
  await mockAuthApi(page)

  await page.goto('/login')

  await expect(page.locator('.login-page__captcha-image img')).toBeVisible()
})

test('手机号格式非法时阻止提交且不发起登录请求', async ({ page }) => {
  let loginRequested = false
  await mockAuthApi(page)
  await page.route('**/api/auth/captcha/login', async (route) => {
    loginRequested = true
    await route.fulfill({ json: mallResult(null) })
  })

  await page.goto('/login')
  await submitLogin(page, '12345', 'pass1234')

  await expect(page.locator('.el-form-item__error')).toContainText('手机号格式不正确')
  expect(loginRequested).toBe(false)
})

test('登录成功后写入令牌、回跳受保护页并展示资料', async ({ page }) => {
  await mockAuthApi(page)

  await page.goto('/profile')
  await submitLogin(page, '13800000000', 'pass1234')

  await expect(page).toHaveURL(/\/profile/)
  await expect(page.locator('.user-center__name')).toHaveText('测试用户')
  await expect(page.locator('.user-center__level')).toHaveText('黄金会员')

  const token = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(token).toBe('e2e-access-token')
})

test('登录失败后展示后端错误并刷新一次性验证码', async ({ page }) => {
  const captchaCounter = { count: 0 }
  await mockAuthApi(page, { loginFails: true, captchaCounter })

  await page.goto('/login')
  await expect(page.locator('.login-page__captcha-image img')).toBeVisible()
  expect(captchaCounter.count).toBe(1)

  await submitLogin(page, '13800000000', 'pass1234')

  await expect(page.locator('.el-message').first()).toContainText('手机号或密码错误')
  // 验证码一次性消费，失败后必须重新获取
  await expect.poll(() => captchaCounter.count).toBe(2)
})

test('登出后清除令牌并回到登录页', async ({ page }) => {
  await mockAuthApi(page)

  await page.goto('/profile')
  await submitLogin(page, '13800000000', 'pass1234')
  await expect(page).toHaveURL(/\/profile/)

  await page.getByRole('button', { name: '退出登录' }).click()

  await expect(page).toHaveURL(/\/login/)
  expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull()
  expect(await page.evaluate(() => localStorage.getItem('refreshToken'))).toBeNull()
})

test('注册成功后自动登录并回到首页', async ({ page }) => {
  await mockAuthApi(page)

  await page.goto('/register')
  await page.getByPlaceholder('请输入手机号').fill('13800000000')
  await page.getByPlaceholder('8-32 位，须含字母和数字').fill('pass1234')
  await page.getByPlaceholder('请再次输入密码').fill('pass1234')
  await page.getByPlaceholder('请输入验证码').fill('8888')
  await page.locator('.el-checkbox').click()
  await page.getByRole('button', { name: '注册' }).click()

  await expect(page).toHaveURL(/localhost:5173\/$/)
  expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBe('e2e-access-token')
})
