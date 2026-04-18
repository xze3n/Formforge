import { test, expect } from '@playwright/test';

test.describe('Feature 3: Add Application & Application Detail', () => {
  test.beforeEach(async ({ page }) => {
    await page.context().addCookies([
      { name: 'ff_cookie_consent', value: 'granted', domain: 'localhost', path: '/' },
    ]);
  });
  // ── Navigating to Add Application ───────────────────────────────────

  test('should navigate to add-application page from applications list', async ({ page }) => {
    await page.goto('/scholarship-applications');
    await page.getByRole('button', { name: /Add New Application/i }).click();
    await expect(page).toHaveURL('/add-application');
    await expect(page.getByRole('heading', { name: 'New Scholarship Application' })).toBeVisible();
  });

  // ── Form Structure ──────────────────────────────────────────────────

  test('should display all form fields and buttons', async ({ page }) => {
    await page.goto('/add-application');

    await expect(page.getByLabel('Scholarship Type')).toBeVisible();
    await expect(page.getByLabel('Academic Year')).toBeVisible();
    await expect(page.getByLabel('Semester')).toBeVisible();
    await expect(page.getByRole('button', { name: /Fill in my data/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Cancel/i })).toBeVisible();
  });

  // ── Validation ──────────────────────────────────────────────────────

  test('should show validation error when submitting empty form', async ({ page }) => {
    await page.goto('/add-application');
    await page.getByRole('button', { name: /Fill in my data/i }).click();

    await expect(page.getByText('Please fill in all required fields')).toBeVisible();
    // Should stay on the same page
    await expect(page).toHaveURL('/add-application');
  });

  test('should show error if only scholarship type is selected', async ({ page }) => {
    await page.goto('/add-application');

    // Select only scholarship type
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Merit' }).click();

    await page.getByRole('button', { name: /Fill in my data/i }).click();
    await expect(page.getByText('Please fill in all required fields')).toBeVisible();
  });

  test('should show error if only two fields are selected', async ({ page }) => {
    await page.goto('/add-application');

    // Select scholarship type
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Social' }).click();

    // Select academic year
    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2025/2026' }).click();

    // Leave semester empty
    await page.getByRole('button', { name: /Fill in my data/i }).click();
    await expect(page.getByText('Please fill in all required fields')).toBeVisible();
  });

  // ── Successful Submission ───────────────────────────────────────────

  test('should create application and redirect to applications list', async ({ page }) => {
    await page.goto('/add-application');

    // Fill in all fields
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Merit' }).click();

    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2025/2026' }).click();

    await page.locator('#semester').click();
    await page.getByRole('option', { name: 'I', exact: true }).click();

    // Submit the form
    await page.getByRole('button', { name: /Fill in my data/i }).click();

    // Should redirect to applications list
    await expect(page).toHaveURL('/scholarship-applications');
  });

  test('should show newly created application in the table', async ({ page }) => {
    await page.goto('/scholarship-applications');

    // Wait for data to load
    await page.locator('table tbody tr').first().waitFor();

    // Navigate to add application
    await page.getByRole('button', { name: /Add New Application/i }).click();
    await expect(page).toHaveURL('/add-application');

    // Fill in a Performance / 2026-2027 / Semester II application
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Performance' }).click();

    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2026/2027' }).click();

    await page.locator('#semester').click();
    await page.getByRole('option', { name: 'II' }).click();

    await page.getByRole('button', { name: /Fill in my data/i }).click();
    await expect(page).toHaveURL('/scholarship-applications');

    // Wait for data to reload and verify the new application is in the table
    // Navigate to the last page where new entries appear
    await page.locator('table tbody tr').first().waitFor();
    while (await page.getByRole('button', { name: /Next/i }).isEnabled()) {
      await page.getByRole('button', { name: /Next/i }).click();
    }

    // Verify a row with the created application's data exists
    await expect(page.locator('table tbody tr', { hasText: 'Performance' }).filter({ hasText: '2026/2027' }).filter({ hasText: 'II' }).first()).toBeVisible();
  });

  test('should create applications with each scholarship type', async ({ page }) => {
    const types = ['Merit', 'Social', 'Performance'] as const;

    for (const type of types) {
      await page.goto('/add-application');

      await page.locator('#scholarship-type').click();
      await page.getByRole('option', { name: type }).click();

      await page.locator('#academic-year').click();
      await page.getByRole('option', { name: '2025/2026' }).click();

      await page.locator('#semester').click();
      await page.getByRole('option', { name: 'I', exact: true }).click();

      await page.getByRole('button', { name: /Fill in my data/i }).click();
      await expect(page).toHaveURL('/scholarship-applications');
    }
  });

  // ── Cancel Button ───────────────────────────────────────────────────

  test('should navigate back to applications list when Cancel is clicked', async ({ page }) => {
    await page.goto('/add-application');
    await page.getByRole('button', { name: /Cancel/i }).click();
    await expect(page).toHaveURL('/scholarship-applications');
  });

  // ── Application Detail Page ─────────────────────────────────────────

  test('should display application details on the detail page', async ({ page }) => {
    await page.goto('/scholarship-applications');
    await page.locator('table tbody tr').first().click();
    await expect(page).toHaveURL(/\/application\/\d+/);

    await expect(page.getByText('Application Details')).toBeVisible();
    await expect(page.getByText('Application ID')).toBeVisible();
    await expect(page.getByText('Scholarship Type', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Academic Year')).toBeVisible();
    await expect(page.getByLabel('Semester')).toBeVisible();
    await expect(page.getByText('Status').first()).toBeVisible();
  });

  test('should navigate back from detail page', async ({ page }) => {
    await page.goto('/application/1');
    await page.getByRole('button', { name: /Back to Applications/i }).click();
    await expect(page).toHaveURL('/scholarship-applications');
  });

  test('should show "Application Not Found" for non-existent id', async ({ page }) => {
    await page.goto('/application/9999');
    await expect(page.getByText('Application Not Found')).toBeVisible();
    await expect(page.getByRole('button', { name: /Back to Applications/i })).toBeVisible();
  });

  test('should allow editing academic year and saving changes', async ({ page }) => {
    // Create a fresh application to edit
    await page.goto('/add-application');
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Merit' }).click();
    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2025/2026' }).click();
    await page.locator('#semester').click();
    await page.getByRole('option', { name: 'I', exact: true }).click();
    await page.getByRole('button', { name: /Fill in my data/i }).click();
    await expect(page).toHaveURL('/scholarship-applications');

    // Wait for data to load, then navigate to last page
    await page.locator('table tbody tr').first().waitFor();
    while (await page.getByRole('button', { name: /Next/i }).isEnabled()) {
      await page.getByRole('button', { name: /Next/i }).click();
    }
    await page.locator('table tbody tr').last().click();
    await expect(page).toHaveURL(/\/application\/\d+/);

    // Change academic year
    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2026/2027' }).click();

    // Save
    await page.getByRole('button', { name: /Save Changes/i }).click();

    // Success message should appear
    await expect(page.getByText('Changes saved successfully!')).toBeVisible();
  });

  test('should delete application from detail page and redirect', async ({ page }) => {
    // Create a fresh application to delete
    await page.goto('/add-application');
    await page.locator('#scholarship-type').click();
    await page.getByRole('option', { name: 'Social' }).click();
    await page.locator('#academic-year').click();
    await page.getByRole('option', { name: '2025/2026' }).click();
    await page.locator('#semester').click();
    await page.getByRole('option', { name: 'II' }).click();
    await page.getByRole('button', { name: /Fill in my data/i }).click();
    await expect(page).toHaveURL('/scholarship-applications');

    // Wait for data to load, then navigate to last page
    await page.locator('table tbody tr').first().waitFor();
    while (await page.getByRole('button', { name: /Next/i }).isEnabled()) {
      await page.getByRole('button', { name: /Next/i }).click();
    }
    await page.locator('table tbody tr').last().click();
    await expect(page).toHaveURL(/\/application\/\d+/);

    await page.getByRole('button', { name: /Delete Application/i }).click();

    // Confirm deletion in the AlertDialog
    await page.getByRole('alertdialog').getByRole('button', { name: 'Delete' }).click();

    // Should redirect to applications list
    await expect(page).toHaveURL('/scholarship-applications');
  });
});
