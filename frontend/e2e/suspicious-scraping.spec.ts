/**
 * Suspicious Behaviour Simulation #2: Mass Data Scraping
 *
 * Simulates a bot that logs in and then fires 35 rapid paginated
 * read-all-applications GraphQL queries to exfiltrate the full dataset.
 *
 * Detection rule: MASS_ENUMERATION
 *   ≥ 30 READ_APPLICATIONS audit events for the same user in 1 minute → MEDIUM
 *
 * Each GraphQL `applications` query routes through ApplicationService which
 * carries @Audited(action = READ_APPLICATIONS), so every call is logged.
 *
 * How to run:
 *   npx playwright test --config playwright.suspicious.config.ts suspicious-scraping
 *
 * To also assert the observation:
 *   $env:ADMIN_TOKEN="<jwt>"; npx playwright test --config playwright.suspicious.config.ts suspicious-scraping
 */

import { test, expect } from '@playwright/test';

const BACKEND     = 'https://localhost:8443';
const QUERY_ROUNDS = 35; // exceeds the threshold of 30

const GQL_LIST_APPLICATIONS = `
  query {
    applications(page: 0, size: 5) {
      content { id type status }
      totalElements
    }
  }
`;

test.describe('Suspicious Behaviour: Mass Data Scraping', () => {
  let scraperToken: string;
  let scraperEmail: string;

  test.beforeAll(async ({ request }) => {
    const ts    = Date.now();
    scraperEmail = `bot-scraper-${ts}@formforge-test.com`;
    const username = `botscraper${ts}`;
    const password = `ScraperPass${ts}!`;

    // Register — the register endpoint returns a full LoginResponse with JWT,
    // so we use that token directly instead of doing a separate login step.
    // (The login endpoint now triggers 2FA and does not return a JWT directly.)
    const reg = await request.post(`${BACKEND}/api/auth/register`, {
      data: { username, email: scraperEmail, password },
    });
    expect(reg.status(), `Registration failed: ${await reg.text()}`).toBe(201);
    scraperToken = (await reg.json()).token;
    console.log(`[setup] Scraper account registered and authenticated: ${scraperEmail}`);
  });

  test('fires 35 rapid bulk-read queries and triggers MASS_ENUMERATION detection', async ({ request }) => {
    console.log(`\n[attack] Sending ${QUERY_ROUNDS} rapid GraphQL applications queries …`);

    for (let i = 1; i <= QUERY_ROUNDS; i++) {
      const res = await request.post(`${BACKEND}/graphql`, {
        headers: {
          Authorization:  `Bearer ${scraperToken}`,
          'Content-Type': 'application/json',
        },
        data: { query: GQL_LIST_APPLICATIONS },
      });
      // GraphQL always returns 200 even for auth/permission errors
      expect(res.status()).toBe(200);
      if (i % 10 === 0) console.log(`  query ${i}/${QUERY_ROUNDS} → ${res.status()} ✓`);
    }

    console.log('\n[wait] Allowing 5 s for async threat detection + AI explanation …');
    await new Promise(r => setTimeout(r, 5_000));

    const adminToken = process.env.ADMIN_TOKEN;
    if (adminToken) {
      console.log('[check] ADMIN_TOKEN set — verifying observation …');
      const obs = await request.get(`${BACKEND}/api/admin/observations?unresolvedOnly=false&size=100`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      });
      expect(obs.status()).toBe(200);
      const body = await obs.json();
      const scraping = body.content.find((o: any) => o.reason === 'MASS_ENUMERATION');
      expect(scraping, 'Expected MASS_ENUMERATION observation').toBeDefined();
      expect(scraping.severity).toBe('MEDIUM');
      console.log(`[check] ✓ MASS_ENUMERATION observation found (id=${scraping.id})`);
      if (scraping.aiExplanation) {
        console.log(`[AI]    ${scraping.aiExplanation}`);
      } else {
        console.log('[AI]    explanation still pending');
      }
    } else {
      console.log('[check] No ADMIN_TOKEN — skipping observation assertion.');
      console.log('        Open https://localhost:5173/admin to see the detection in the dashboard.');
    }
  });
});
