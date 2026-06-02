import { defineConfig, devices } from '@playwright/test';

/**
 * Standalone config for suspicious-behaviour simulations.
 *
 * Unlike the main playwright.config.ts, this config:
 *   - targets the backend HTTPS API directly (no frontend webServer)
 *   - ignores self-signed TLS certificate errors
 *   - runs tests sequentially (workers: 1) so detection windows aren't polluted
 *     by parallel users from the same machine
 *
 * Usage:
 *   npx playwright test --config playwright.suspicious.config.ts
 *
 * To verify that observations were created you need an admin JWT.
 * Obtain one via POST https://localhost:8443/api/auth/login and pass it:
 *   ADMIN_TOKEN=<jwt> npx playwright test --config playwright.suspicious.config.ts
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/suspicious-*.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL:           'https://localhost:8443',
    ignoreHTTPSErrors: true,
    trace:             'on-first-retry',
  },
  projects: [
    {
      name: 'suspicious-behaviour',
      use: { ...devices['Desktop Chrome'], ignoreHTTPSErrors: true },
    },
  ],
  // No webServer — tests call the backend API directly via request fixture
});
