import { test, expect } from '@playwright/test';

test.describe('Feature 1: Navigation & Responsive Layout', () => {
  // ── Desktop Navigation ──────────────────────────────────────────────

  test.describe('Desktop', () => {
    test.use({ viewport: { width: 1280, height: 720 } });

    test('should display all navbar links on desktop', async ({ page }) => {
      await page.goto('/');
      const nav = page.locator('nav');

      await expect(nav.getByRole('link', { name: 'Applications' })).toBeVisible();
      await expect(nav.getByRole('link', { name: 'Dashboard' })).toBeVisible();
      await expect(nav.getByRole('link', { name: 'Personal Info' })).toBeVisible();
      await expect(nav.getByRole('link', { name: 'Documents' })).toBeVisible();
      await expect(nav.getByRole('link', { name: 'Login' })).toBeVisible();
      await expect(nav.getByRole('link', { name: 'Register' })).toBeVisible();
    });

    test('should hide hamburger menu on desktop', async ({ page }) => {
      await page.goto('/');
      await expect(page.getByLabel('Open navigation menu')).not.toBeVisible();
    });

    test('should navigate to Applications page via navbar', async ({ page }) => {
      await page.goto('/');
      await page.locator('nav').getByRole('link', { name: 'Applications' }).click();
      await expect(page).toHaveURL('/scholarship-applications');
      await expect(page.getByRole('heading', { name: 'Scholarship Applications' })).toBeVisible();
    });

    test('should navigate to Login page via navbar', async ({ page }) => {
      await page.goto('/');
      await page.locator('nav').getByRole('link', { name: 'Login' }).click();
      await expect(page).toHaveURL('/login');
      await expect(page.getByRole('heading', { name: 'Welcome Back' })).toBeVisible();
    });

    test('should navigate to Register page via navbar', async ({ page }) => {
      await page.goto('/');
      await page.locator('nav').getByRole('link', { name: 'Register' }).first().click();
      await expect(page).toHaveURL('/register');
      await expect(page.getByRole('heading', { name: 'Create Account' })).toBeVisible();
    });

    test('should navigate home when clicking the logo', async ({ page }) => {
      await page.goto('/scholarship-applications');
      await page.locator('nav a[href="/"]').click();
      await expect(page).toHaveURL('/');
      await expect(page.getByRole('heading', { name: 'Helping students apply smarter' })).toBeVisible();
    });
  });

  // ── Mobile Navigation / Hamburger Menu ──────────────────────────────

  test.describe('Mobile', () => {
    test.use({ viewport: { width: 375, height: 812 } });

    test('should hide desktop nav links on mobile', async ({ page }) => {
      await page.goto('/');
      // Desktop-only links should be hidden
      const desktopNav = page.locator('.hidden.md\\:flex');
      await expect(desktopNav).not.toBeVisible();
    });

    test('should show hamburger button on mobile', async ({ page }) => {
      await page.goto('/');
      await expect(page.getByLabel('Open navigation menu')).toBeVisible();
    });

    test('should open mobile menu when hamburger is clicked', async ({ page }) => {
      await page.goto('/');
      await page.getByLabel('Open navigation menu').click();

      const mobileMenu = page.locator('#mobile-nav-menu');
      await expect(mobileMenu).toBeVisible();
      await expect(mobileMenu.getByRole('link', { name: 'Applications' })).toBeVisible();
      await expect(mobileMenu.getByRole('link', { name: 'Dashboard' })).toBeVisible();
      await expect(mobileMenu.getByRole('link', { name: 'Register' })).toBeVisible();
    });

    test('should close mobile menu when close button is clicked', async ({ page }) => {
      await page.goto('/');
      await page.getByLabel('Open navigation menu').click();
      await expect(page.locator('#mobile-nav-menu')).toBeVisible();

      await page.getByLabel('Close navigation menu').click();
      await expect(page.locator('#mobile-nav-menu')).not.toBeVisible();
    });

    test('should navigate via mobile menu and auto-close it', async ({ page }) => {
      await page.goto('/');
      await page.getByLabel('Open navigation menu').click();
      await page.locator('#mobile-nav-menu').getByRole('link', { name: 'Applications' }).click();

      await expect(page).toHaveURL('/scholarship-applications');
      await expect(page.locator('#mobile-nav-menu')).not.toBeVisible();
    });

    test('should keep Login visible in mobile header', async ({ page }) => {
      await page.goto('/');
      // Login sits outside the mobile menu, in the header
      const headerLogin = page.locator('nav > div > div').first().locator('..').getByRole('link', { name: 'Login' });
      await expect(page.locator('nav').getByRole('link', { name: 'Login' })).toBeVisible();
    });
  });

  // ── Home Page Content ───────────────────────────────────────────────

  test('should display hero section with tagline, description, and CTA buttons', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Helping students apply smarter' })).toBeVisible();
    await expect(page.getByText('FormForge helps you complete')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Get Started' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Learn More' })).toBeVisible();
  });

  test('should display hero image', async ({ page }) => {
    await page.goto('/');
    const heroImg = page.getByAltText('Stack of documents');
    await expect(heroImg).toBeVisible();
  });

  test('Get Started button should link to register', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: 'Get Started' }).click();
    await expect(page).toHaveURL('/register');
  });
});
