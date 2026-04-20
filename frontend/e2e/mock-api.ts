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

  // ── GET /api/enums ──────────────────────────────────────────────
  await page.route('**/api/enums', async (route, request) => {
    if (request.method() === 'HEAD') {
      return route.fulfill({ status: 200 });
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(enumValues),
    });
  });

  // ── GET /api/applications?page=…&size=… ─────────────────────────
  await page.route('**/api/applications?*', async (route, request) => {
    if (request.method() !== 'GET') return route.fallback();
    const url = new URL(request.url());
    const pageNum = parseInt(url.searchParams.get('page') ?? '0');
    const size = parseInt(url.searchParams.get('size') ?? '1000');
    const start = pageNum * size;
    const content = apps.slice(start, start + size);
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content,
        page: pageNum,
        size,
        totalElements: apps.length,
        totalPages: Math.ceil(apps.length / size),
      }),
    });
  });

  // ── POST /api/applications ──────────────────────────────────────
  await page.route('**/api/applications', async (route, request) => {
    if (request.method() !== 'POST') return route.fallback();
    const input = request.postDataJSON();
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
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify(newApp),
    });
  });

  // ── GET | PUT | DELETE /api/applications/:id ────────────────────
  await page.route(/\/api\/applications\/(\d+)$/, async (route, request) => {
    const id = parseInt(request.url().match(/\/api\/applications\/(\d+)/)![1]);
    const method = request.method();

    if (method === 'GET') {
      const app = apps.find(a => a.id === id);
      if (!app) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ error: 'Not found' }) });
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(app) });
    }

    if (method === 'PUT') {
      const idx = apps.findIndex(a => a.id === id);
      if (idx === -1) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ error: 'Not found' }) });
      const input = request.postDataJSON();
      apps[idx] = { ...apps[idx], ...input };
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(apps[idx]) });
    }

    if (method === 'DELETE') {
      const idx = apps.findIndex(a => a.id === id);
      if (idx === -1) return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ error: 'Not found' }) });
      apps.splice(idx, 1);
      return route.fulfill({ status: 204 });
    }

    return route.fallback();
  });

  // ── POST /api/generator/start ───────────────────────────────────
  await page.route('**/api/generator/start', async (route, request) => {
    if (request.method() !== 'POST') return route.fallback();
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ running: true, started: true }),
    });
  });

  // ── POST /api/generator/stop ────────────────────────────────────
  await page.route('**/api/generator/stop', async (route, request) => {
    if (request.method() !== 'POST') return route.fallback();
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ running: false, stopped: true }),
    });
  });

  // ── GET /api/generator/status ───────────────────────────────────
  await page.route('**/api/generator/status', async (route, request) => {
    if (request.method() !== 'GET') return route.fallback();
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ running: false }),
    });
  });
}
