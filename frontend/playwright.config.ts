import { defineConfig } from '@playwright/test'

if (!process.env.E2E_BASE_URL) throw new Error('Укажите E2E_BASE_URL для браузерных тестов')
export default defineConfig({
  testDir: './tests',
  workers: 1,
  fullyParallel: false,
  timeout: 45000,
  use: { baseURL: process.env.E2E_BASE_URL, locale: 'ru-RU', viewport: { width: 1440, height: 1000 }, trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  reporter: 'list',
})
