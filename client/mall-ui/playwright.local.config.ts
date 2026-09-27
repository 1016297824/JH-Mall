import { defineConfig, devices } from '@playwright/test'

/**
 * 本机验证用临时配置：复用已在 5173 运行的 dev server，改用系统 Edge（免下载浏览器）。
 * 不进入版本库，验证完成后删除。
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30 * 1000,
  expect: { timeout: 5000 },
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:5173',
    headless: true,
    trace: 'off',
  },
  projects: [
    {
      name: 'msedge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' },
    },
  ],
})
