import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './provider-tests',
  outputDir: './test-results/adzuna',
  workers: 2, retries: 0, reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:8081' },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    command: 'mvn -B -ntp -f ../backend/pom.xml "-Dspring-boot.run.profiles=adzuna-local" spring-boot:run',
    env: { SERVER_PORT: '8081', ADZUNA_APP_ID: '', ADZUNA_APP_KEY: '' },
    url: 'http://127.0.0.1:8081/api/providers/adzuna/availability',
    timeout: 180000, reuseExistingServer: false,
  },
});
