import { Page } from '@playwright/test';
import { Application } from '../src/app/types/application';

// ── Seed data (mirrors the backend's InMemoryApplicationRepository) ──
const seedApplications: Application[] = [
  { id: 1, type: 'Merit',       academicYear: '2023/2024', semester: 'I',  createdAt: '9/15/2023',  status: 'Approved' },
  { id: 2, type: 'Social',      academicYear: '2024/2025', semester: 'I',  createdAt: '8/20/2024',  status: 'Pending Action' },
  { id: 3, type: 'Merit',       academicYear: '2024/2025', semester: 'II', createdAt: '1/10/2025',  status: 'Approved' },
  { id: 4, type: 'Performance', academicYear: '2024/2025', semester: 'II', createdAt: '2/5/2025',   status: 'Draft' },
  { id: 5, type: 'Social',      academicYear: '2025/2026', semester: 'I',  createdAt: '9/5/2025',   status: 'Approved' },
  { id: 6, type: 'Merit',       academicYear: '2025/2026', semester: 'II', createdAt: '2/15/2026',  status: 'Pending Action' },
  { id: 7, type: 'Performance', academicYear: '2025/2026', semester: 'I',  createdAt: '10/10/2025', status: 'Draft' },
  { id: 8, type: 'Social',      academicYear: '2025/2026', semester: 'II', createdAt: '3/1/2026',   status: 'Pending Action' },
];

const enumValues = {
  types: ['Merit', 'Social', 'Performance'],
  statuses: ['Draft', 'Pending Action', 'Approved'],
  semesters: ['I', 'II'],
  academicYears: ['2023/2024', '2024/2025', '2025/2026', '2026/2027', '2027/2028'],
};

/**
 * Install API route mocks on the given page so tests work without a backend.
 * Each page gets its own independent copy of the data.
 */
export async function mockApi(page: Page) {
  // Each page/test gets an isolated mutable copy
  let apps = structuredClone(seedApplications);
  let nextId = Math.max(...apps.map(a => a.id)) + 1;
  let generatorRunning = false;

  // ── Single GraphQL route handler ────────────────────────────────
  await page.route('**/graphql', async (route, request) => {
    if (request.method() !== 'POST') return route.fallback();

    const body = request.postDataJSON();
    const query: string = body.query ?? '';
    const variables = body.variables ?? {};

    // ── Health check (__typename) ──
    if (query.includes('__typename')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { __typename: 'Query' } }),
      });
    }

    // ── Query: applications ──
    if (query.includes('applications') && !query.includes('mutation')) {
      const page = variables.page ?? 0;
      const size = variables.size ?? 1000;
      const start = page * size;
      const content = apps.slice(start, start + size);
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: {
            applications: {
              content,
              page,
              size,
              totalElements: apps.length,
              totalPages: Math.ceil(apps.length / size),
            },
          },
        }),
      });
    }

    // ── Query: application (single by id) ──
    if (query.includes('application(') || (query.includes('application') && variables.id !== undefined && !query.includes('applications'))) {
      const id = Number(variables.id);
      const app = apps.find(a => a.id === id);
      if (!app) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: null,
            errors: [{ message: `Application with id ${id} not found`, extensions: { classification: 'NOT_FOUND' } }],
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { application: app } }),
      });
    }

    // ── Query: enums ──
    if (query.includes('enums')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { enums: enumValues } }),
      });
    }

    // ── Query: generatorStatus ──
    if (query.includes('generatorStatus')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { generatorStatus: { running: generatorRunning } } }),
      });
    }

    // ── Mutation: createApplication ──
    if (query.includes('createApplication')) {
      const input = variables.input;
      const now = new Date();
      const newApp: Application = {
        id: nextId++,
        type: input.type,
        academicYear: input.academicYear,
        semester: input.semester,
        status: input.status ?? 'Draft',
        createdAt: `${now.getMonth() + 1}/${now.getDate()}/${now.getFullYear()}`,
      };
      apps.push(newApp);
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { createApplication: newApp } }),
      });
    }

    // ── Mutation: updateApplication ──
    if (query.includes('updateApplication')) {
      const id = Number(variables.id);
      const idx = apps.findIndex(a => a.id === id);
      if (idx === -1) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: null,
            errors: [{ message: `Application with id ${id} not found` }],
          }),
        });
      }
      apps[idx] = { ...apps[idx], ...variables.input };
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { updateApplication: apps[idx] } }),
      });
    }

    // ── Mutation: deleteApplication ──
    if (query.includes('deleteApplication')) {
      const id = Number(variables.id);
      const idx = apps.findIndex(a => a.id === id);
      if (idx !== -1) apps.splice(idx, 1);
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { deleteApplication: true } }),
      });
    }

    // ── Mutation: startGenerator ──
    if (query.includes('startGenerator')) {
      generatorRunning = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { startGenerator: { running: true } } }),
      });
    }

    // ── Mutation: stopGenerator ──
    if (query.includes('stopGenerator')) {
      generatorRunning = false;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ data: { stopGenerator: { running: false } } }),
      });
    }

    // Unhandled query — pass through
    return route.fallback();
  });
}
