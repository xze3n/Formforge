/**
 * Suspicious Behaviour Simulation #3: Rapid Automated Mutations
 *
 * Simulates a bot that authenticates and then mass-creates scholarship
 * applications in rapid succession — typical of automated form-stuffing
 * or content-spam attacks.
 *
 * Detection rule: MASS_MUTATION
 *   ≥ 10 write operations (CREATE/UPDATE_APPLICATION, CREATE/UPDATE_DOCUMENT)
 *   for the same user in 2 minutes → MEDIUM
 *
 * Each GraphQL createApplication mutation routes through ApplicationService
 * which carries @Audited(action = CREATE_APPLICATION).
 *
 * How to run:
 *   npx playwright test --config playwright.suspicious.config.ts suspicious-mutations
 *
 * To also assert the observation:
 *   $env:ADMIN_TOKEN="<jwt>"; npx playwright test --config playwright.suspicious.config.ts suspicious-mutations
 */

import { test, expect } from '@playwright/test';

const BACKEND         = 'https://localhost:8443';
const MUTATION_ROUNDS = 11; // exceeds the threshold of 10

const GQL_CREATE_APPLICATION = `
  mutation CreateApp($input: ApplicationInput!) {
    createApplication(input: $input) {
      id
      type
      status
    }
  }
`;

const ACADEMIC_YEARS = ['2023/2024', '2024/2025', '2025/2026'];
const SEMESTERS      = ['FIRST', 'SECOND'];
const TYPES          = ['MERIT', 'SOCIAL', 'PERFORMANCE'];

test.describe('Suspicious Behaviour: Rapid Automated Mutations', () => {
  let mutatorToken: string;
  let mutatorEmail: string;

  test.beforeAll(async ({ request }) => {
    const ts      = Date.now();
    mutatorEmail  = `bot-mutator-${ts}@formforge-test.com`;
    const username = `botmutator${ts}`;
    const password = `MutatorPass${ts}!`;

    // Register — the register endpoint returns a full LoginResponse with JWT,
    // so we use that token directly instead of doing a separate login step.
    // (The login endpoint now triggers 2FA and does not return a JWT directly.)
    const reg = await request.post(`${BACKEND}/api/auth/register`, {
      data: { username, email: mutatorEmail, password },
    });
    expect(reg.status(), `Registration failed: ${await reg.text()}`).toBe(201);
    mutatorToken = (await reg.json()).token;
    console.log(`[setup] Mutator account registered and authenticated: ${mutatorEmail}`);
  });

  test('fires 11 rapid createApplication mutations and triggers MASS_MUTATION detection', async ({ request }) => {
    console.log(`\n[attack] Sending ${MUTATION_ROUNDS} rapid createApplication mutations …`);

    for (let i = 1; i <= MUTATION_ROUNDS; i++) {
      const input = {
        type:         TYPES[i % TYPES.length],
        academicYear: ACADEMIC_YEARS[i % ACADEMIC_YEARS.length],
        semester:     SEMESTERS[i % SEMESTERS.length],
      };

      const res = await request.post(`${BACKEND}/graphql`, {
        headers: {
          Authorization:  `Bearer ${mutatorToken}`,
          'Content-Type': 'application/json',
        },
        data: { query: GQL_CREATE_APPLICATION, variables: { input } },
      });
      expect(res.status()).toBe(200);

      const body = await res.json();
      // A GraphQL-level permission error still returns HTTP 200 with errors array
      if (body.errors) {
        console.warn(`  mutation ${i} → GraphQL error: ${body.errors[0]?.message}`);
      } else {
        console.log(`  mutation ${i}/${MUTATION_ROUNDS} → created id=${body.data?.createApplication?.id} ✓`);
      }
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
      const massMutation = body.content.find((o: any) => o.reason === 'MASS_MUTATION');
      expect(massMutation, 'Expected MASS_MUTATION observation').toBeDefined();
      expect(massMutation.severity).toBe('MEDIUM');
      console.log(`[check] ✓ MASS_MUTATION observation found (id=${massMutation.id})`);
      if (massMutation.aiExplanation) {
        console.log(`[AI]    ${massMutation.aiExplanation}`);
      } else {
        console.log('[AI]    explanation still pending');
      }
    } else {
      console.log('[check] No ADMIN_TOKEN — skipping observation assertion.');
      console.log('        Open https://localhost:5173/admin to see the detection in the dashboard.');
    }
  });
});
