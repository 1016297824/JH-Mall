import { execSync } from 'node:child_process'

import { test, expect, type Page } from '@playwright/test'

/**
 * 真实后端全链路验证（本机临时用）
 *
 * <p>不 mock 任何业务接口：请求经 vite proxy 打到真实网关(8080) → mall-auth/mall-user → MySQL/Redis。
 * 唯一无法自动化的是识别图片验证码，因此在验证码接口处读取 Redis 明文来模拟"用户看图输入"。</p>
 */

/** 从 Redis 读取验证码明文（后端以 Jackson 序列化存储，值带双引号） */
function readCaptchaFromRedis(captchaKey: string): string {
  const raw = execSync(`docker exec mall-redis redis-cli GET mall:auth:captcha:${captchaKey}`, {
    encoding: 'utf8',
  })
  return raw.trim().replace(/^"|"$/g, '')
}

/** 拦截验证码接口：直连真实网关取验证码并原样回给页面，同时记录明文供表单填写 */
async function spyCaptcha(page: Page, holder: { code: string }): Promise<void> {
  await page.route(/\/api\/auth\/captcha$/, async (route) => {
    const upstream = await fetch('http://localhost:8080/api/auth/captcha')
    const body = await upstream.json()
    const key: string | undefined = body?.data?.captchaKey
    if (key) {
      holder.code = readCaptchaFromRedis(key)
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(body),
    })
  })
}

/** 生成一个未注册的手机号 */
function uniquePhone(): string {
  return `139${String(Date.now()).slice(-8)}`
}

test.beforeEach(async ({ page }) => {
  // addInitScript 会在每次导航时执行；若不加标记，后续 goto('/profile') 会把刚写入的 token 清掉
  await page.addInitScript(() => {
    if (!sessionStorage.getItem('__e2e_initialized')) {
      localStorage.clear()
      sessionStorage.setItem('__e2e_initialized', '1')
    }
  })
})

test('真实后端：注册 → 令牌落库 → 网关鉴权个人中心 → 登出', async ({ page }) => {
  const captcha = { code: '' }
  await spyCaptcha(page, captcha)
  const phone = uniquePhone()

  // 1) 注册（真实调用 /api/auth/captcha/register）
  await page.goto('/register')
  await expect(page.locator('.register-page__captcha-image img')).toBeVisible()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('8-32 位，须含字母和数字').fill('pass1234')
  await page.getByPlaceholder('请再次输入密码').fill('pass1234')
  await page.getByPlaceholder('请输入验证码').fill(captcha.code)
  await page.locator('.el-checkbox').click()
  await page.getByRole('button', { name: '注册' }).click()

  // 2) 注册成功 → 自动登录并回首页
  await expect(page).toHaveURL(/localhost:5173\/$/)
  const token = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(token, '注册后应写入真实 accessToken').toBeTruthy()

  // 3) 带真实 JWT 访问受保护页：网关校验 → mall-user 返回真实资料
  await page.goto('/profile')
  await expect(page.locator('.user-center__name')).toBeVisible()
  await expect(page.locator('.user-center__stats')).toBeVisible()

  // 4) 登出（真实 DELETE /api/auth/sessions/current）
  await page.getByRole('button', { name: '退出登录' }).click()
  await expect(page).toHaveURL(/\/login/)
  expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull()
})

test('真实后端：注册后可用密码登录并进入个人中心', async ({ page }) => {
  const captcha = { code: '' }
  await spyCaptcha(page, captcha)
  const phone = uniquePhone()

  // 先注册一个真实账号
  await page.goto('/register')
  await expect(page.locator('.register-page__captcha-image img')).toBeVisible()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('8-32 位，须含字母和数字').fill('pass1234')
  await page.getByPlaceholder('请再次输入密码').fill('pass1234')
  await page.getByPlaceholder('请输入验证码').fill(captcha.code)
  await page.locator('.el-checkbox').click()
  await page.getByRole('button', { name: '注册' }).click()
  await expect(page).toHaveURL(/localhost:5173\/$/)

  // 清掉登录态后直接访问受保护页，验证"未登录 → 引导登录 → 登录后原路返回"完整闭环
  await page.evaluate(() => localStorage.clear())
  await page.goto('/profile')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.locator('.login-page__captcha-image img')).toBeVisible()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('请输入密码').fill('pass1234')
  await page.getByPlaceholder('请输入验证码').fill(captcha.code)
  await page.getByRole('button', { name: '登录' }).click()

  // 密码登录成功 → 回跳个人中心并持有真实令牌
  await expect(page.locator('.user-center__name')).toBeVisible({ timeout: 10_000 })
  expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeTruthy()
})

test('真实后端：密码错误时展示后端 userTip 而非 axios 英文文案', async ({ page }) => {
  const captcha = { code: '' }
  await spyCaptcha(page, captcha)
  const phone = uniquePhone()

  await page.goto('/register')
  await expect(page.locator('.register-page__captcha-image img')).toBeVisible()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('8-32 位，须含字母和数字').fill('pass1234')
  await page.getByPlaceholder('请再次输入密码').fill('pass1234')
  await page.getByPlaceholder('请输入验证码').fill(captcha.code)
  await page.locator('.el-checkbox').click()
  await page.getByRole('button', { name: '注册' }).click()
  await expect(page).toHaveURL(/localhost:5173\/$/)

  // 用错误密码登录：后端返回 HTTP 400 + {errorCode, errorMessage, userTip}
  await page.evaluate(() => localStorage.clear())
  await page.goto('/login')
  await expect(page.locator('.login-page__captcha-image img')).toBeVisible()
  await page.getByPlaceholder('请输入手机号').fill(phone)
  await page.getByPlaceholder('请输入密码').fill('wrongpass9')
  await page.getByPlaceholder('请输入验证码').fill(captcha.code)
  await page.getByRole('button', { name: '登录' }).click()

  // 应展示后端面向用户的 userTip，而不是 "Request failed with status code 400"
  await expect(page.locator('.el-message').first()).toHaveText('密码错误')
})
