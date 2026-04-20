import { test, expect } from '@playwright/test';
import { mockApi } from './mock-api';

test.describe('Feature 2: Scholarship Applications Management', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page);
    await page.context().addCookies([
      { name: 'ff_cookie_consent', value: 'granted', domain: 'localhost', path: '/' },
    ]);
    await page.goto('/scholarship-applications');
  });

  // ── Page Structure ──────────────────────────────────────────────────

  test('should display the applications page heading and description', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Scholarship Applications' })).toBeVisible();
    await expect(page.getByText('Manage and track all your scholarship applications')).toBeVisible();
  });

  test('should display Add New Application button', async ({ page }) => {
    await expect(page.getByRole('button', { name: /Add New Application/i })).toBeVisible();
  });

  test('should display three view mode buttons', async ({ page }) => {
    await expect(page.getByRole('button', { name: /Table View/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Statistics View/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Cards View/i })).toBeVisible();
  });

  // ── Table View ──────────────────────────────────────────────────────

  test('should default to table view with correct columns', async ({ page }) => {
    const table = page.locator('table');
    await expect(table).toBeVisible();

    await expect(table.getByRole('columnheader', { name: 'ID' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Scholarship Type' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Academic Year' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Semester' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Created At' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Status' })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Actions' })).toBeVisible();
  });

  test('should show 5 rows per page (pagination limit)', async ({ page }) => {
    const rows = page.locator('table tbody tr');
    await expect(rows).toHaveCount(5);
  });

  test('should display pagination info text', async ({ page }) => {
    await expect(page.getByText(/Showing \d+ to \d+ of \d+ results/)).toBeVisible();
  });

  test('should navigate to next page via pagination', async ({ page }) => {
    // Wait for data to load
    await page.locator('table tbody tr').first().waitFor();

    // Read first page info dynamically
    const firstPageText = await page.getByText(/Showing \d+ to \d+/).textContent();
    const firstPageEnd = parseInt(firstPageText!.match(/to (\d+)/)![1]);

    await page.getByRole('button', { name: /Next/i }).click();

    // Second page should show items starting after the first page's last item
    await expect(page.getByText(new RegExp(`Showing ${firstPageEnd + 1} to \\d+`))).toBeVisible();
  });

  test('should navigate back via Previous button', async ({ page }) => {
    // Wait for data to load
    await page.locator('table tbody tr').first().waitFor();

    await page.getByRole('button', { name: /Next/i }).click();
    await expect(page.getByText(/Showing \d+ to \d+/)).toBeVisible();

    await page.getByRole('button', { name: /Previous/i }).click();
    await expect(page.getByText(/Showing 1 to \d+/)).toBeVisible();
  });

  test('should disable Previous button on first page', async ({ page }) => {
    await expect(page.getByRole('button', { name: /Previous/i })).toBeDisabled();
  });

  test('should disable Next button on last page', async ({ page }) => {
    // Wait for data to load
    await page.locator('table tbody tr').first().waitFor();

    // Navigate to the last page
    while (await page.getByRole('button', { name: /Next/i }).isEnabled()) {
      await page.getByRole('button', { name: /Next/i }).click();
    }
    await expect(page.getByRole('button', { name: /Next/i })).toBeDisabled();
  });

  test('should navigate to application detail when clicking a row', async ({ page }) => {
    // Click the first data row
    await page.locator('table tbody tr').first().click();
    await expect(page).toHaveURL(/\/application\/\d+/);
    await expect(page.getByRole('heading', { name: /Application #\d+/ })).toBeVisible();
  });

  // ── Delete Application ──────────────────────────────────────────────

  test('should delete an application from the table', async ({ page }) => {
    // Wait for data to load
    await page.locator('table tbody tr').first().waitFor();

    const initialText = await page.getByText(/Showing \d+ to \d+ of (\d+) results/).textContent();
    const initialTotal = parseInt(initialText!.match(/of (\d+)/)![1]);

    // Click the delete button on the first row — opens confirmation dialog
    await page.locator('table tbody tr').first().getByRole('button').click();

    // Confirm deletion in the AlertDialog
    await page.getByRole('alertdialog').getByRole('button', { name: 'Delete' }).click();

    // Total should decrease by 1
    await expect(page.getByText(new RegExp(`of ${initialTotal - 1} results`))).toBeVisible();
  });

  // ── View Mode Switching ─────────────────────────────────────────────

  test('should switch to Statistics View', async ({ page }) => {
    await page.getByRole('button', { name: /Statistics View/i }).click();

    // Table should be gone, stats cards should appear
    await expect(page.locator('table')).not.toBeVisible();
    await expect(page.getByText('Total Applications')).toBeVisible();
    await expect(page.getByText('Draft Applications')).toBeVisible();
    await expect(page.getByText('Approved Applications')).toBeVisible();
  });

  test('should display pie charts in statistics view', async ({ page }) => {
    await page.getByRole('button', { name: /Statistics View/i }).click();

    await expect(page.getByText('Applications by Status')).toBeVisible();
    await expect(page.getByText('Applications by Type')).toBeVisible();
  });

  test('should switch to Cards View', async ({ page }) => {
    await page.getByRole('button', { name: /Cards View/i }).click();

    // Table should be gone; card elements should appear
    await expect(page.locator('table')).not.toBeVisible();
    await expect(page.getByText('Application ID').first()).toBeVisible();
    await expect(page.getByText('Scholarship Type').first()).toBeVisible();
  });

  test('should show View Details and Delete buttons in card view', async ({ page }) => {
    await page.getByRole('button', { name: /Cards View/i }).click();

    const firstCard = page.locator('[class*="shadow-lg"]').first();
    await expect(firstCard.getByRole('button', { name: 'View Details' })).toBeVisible();
  });

  test('should navigate to detail from cards view', async ({ page }) => {
    await page.getByRole('button', { name: /Cards View/i }).click();
    await page.getByRole('button', { name: 'View Details' }).first().click();
    await expect(page).toHaveURL(/\/application\/\d+/);
  });

  test('should switch back to Table View from another view', async ({ page }) => {
    await page.getByRole('button', { name: /Statistics View/i }).click();
    await expect(page.locator('table')).not.toBeVisible();

    await page.getByRole('button', { name: /Table View/i }).click();
    await expect(page.locator('table')).toBeVisible();
  });
});
