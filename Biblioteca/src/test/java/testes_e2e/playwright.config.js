// playwright.config.js
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './testes_e2e',
  fullyParallel: false,       // rodar em sequência (dados compartilhados)
  retries: 1,                 // 1 retry em caso de falha flaky
  reporter: [
    ['list'],                 // saída no terminal
    ['html', { outputFolder: 'playwright-report', open: 'never' }]
  ],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',  // grava trace em caso de retry
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    headless: true,
  },
  projects: [
    {
      name: 'Chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // Inicia a aplicação antes dos testes (ajuste o comando conforme o projeto)
  // webServer: {
  //   command: 'npm start',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: true,
  // },
});
