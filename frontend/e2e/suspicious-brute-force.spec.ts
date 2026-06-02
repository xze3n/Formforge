/**
 * Suspicious Behaviour Simulation #1: Brute-Force Login
 *
 * Simulates a bot that knows the target email address and hammers the
 * login endpoint with wrong passwords in rapid succession.
 *
 * Detection rule: BRUTE_FORCE_LOGIN
 *   ≥ 5 LOGIN_FAILURE events for the same user in a 10-minute window → HIGH
 *
 * How to run:
 *   npx playwright test --config playwright.suspicious.config.ts suspicious-brute-force
 *
 * To also assert the observation was persisted, supply an admin JWT:
 *   $env:ADMIN_TOKEN="<jwt>"; npx playwright test --config playwright.suspicious.config.ts suspicious-brute-force
 */

import { test, expect } from '@playwright/test';

const BACKEND = 'https://localhost:8443';
const ATTACK_ROUNDS = 7; // exceeds the detection threshold of 5

test.describe('Suspicious Behaviour: Brute-Force Login', () => {
  /**
   * Each run registers a fresh throwaway user so repeated test runs
   * don't accumulate stale state and detection fires cleanly.
   */
  let targetEmail: string;
  let targetPassword: string;

  test.beforeAll(async ({ request }) => {
    const ts = Date.now();
    targetEmail    = `bot-victim-${ts}@formforge-test.com`;
    targetPassword = `RealPass${ts}!`;
    const username = `botvictim${ts}`;

    const reg = await request.post(`${BACKEND}/api/auth/register`, {
      data: { username, email: targetEmail, password: targetPassword },
    });
    expect(reg.status(), `Registration failed: ${await reg.text()}`).toBe(201);
    console.log(`[setup] Registered target account: ${targetEmail}`);
  });

  test('fires 7 rapid failed logins and triggers BRUTE_FORCE_LOGIN detection', async ({ request }) => {
    console.log(`\n[attack] Sending ${ATTACK_ROUNDS} failed login attempts for ${targetEmail} …`);

    for (let i = 1; i <= ATTACK_ROUNDS; i++) {
      const res = await request.post(`${BACKEND}/api/auth/login`, {
        data: { identifier: targetEmail, password: `WrongPassword${i}!` },
      });
      // Backend always returns 401 for wrong password — that is the expected behaviour
      expect(res.status()).toBe(401);
      console.log(`  attempt ${i}/${ATTACK_ROUNDS} → ${res.status()} ✓`);
    }

    // Allow the async ThreatDetectionService + Ollama to process
    console.log('\n[wait] Allowing 5 s for async threat detection + AI explanation …');
    await new Promise(r => setTimeout(r, 5_000));

    // Optional: verify the observation was created (requires admin token)
    const adminToken = process.env.ADMIN_TOKEN;
    if (adminToken) {
      console.log('[check] ADMIN_TOKEN set — verifying observation …');
      const obs = await request.get(`${BACKEND}/api/admin/observations?unresolvedOnly=false&size=100`, {
        headers: { Authorization: `Bearer ${adminToken}` },
      });
      expect(obs.status()).toBe(200);
      const body = await obs.json();
      const brute = body.content.find((o: any) => o.reason === 'BRUTE_FORCE_LOGIN');
      expect(brute, 'Expected BRUTE_FORCE_LOGIN observation').toBeDefined();
      expect(brute.severity).toBe('HIGH');
      console.log(`[check] ✓ BRUTE_FORCE_LOGIN observation found (id=${brute.id}, severity=${brute.severity})`);
      if (brute.aiExplanation) {
        console.log(`[AI]    ${brute.aiExplanation}`);
      } else {
        console.log('[AI]    explanation still pending (Ollama may still be processing)');
      }
    } else {
      console.log('[check] No ADMIN_TOKEN — skipping observation assertion.');
      console.log('        Open https://localhost:5173/admin to see the detection in the dashboard.');
    }
  });
});
