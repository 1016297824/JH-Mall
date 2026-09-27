import { test, expect } from '@playwright/test'

test('访问根路径时应用挂载并渲染主布局', async ({ page }) => {
  await page.goto('/')
  // 首页数据依赖后端接口，此处只断言应用壳与主布局挂载成功
  await expect(page.locator('#app')).toBeVisible()
  await expect(page.locator('.app-layout')).toBeVisible()
})
