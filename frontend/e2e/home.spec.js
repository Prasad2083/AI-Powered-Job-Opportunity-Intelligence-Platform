import { test, expect } from '@playwright/test';

for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }, { width: 320, height: 740 }]) {
  test(`public home and account navigation at ${viewport.width}px`, async ({ page }, testInfo) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: 'reduce' });
    const privateRequests = [];
    await page.route('**/api/**', route => { privateRequests.push(route.request().url()); return route.fulfill({ status: 401, json: { error: 'Invalid credentials' } }); });
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Your experience.');
    await expect(page.getByRole('link', { name: /Sign up/ })).toBeVisible();
    await page.screenshot({path:testInfo.outputPath('home.png'),fullPage:true});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.getByRole('link', { name: /Explore the workflow/ }).click();
    await expect(page).toHaveURL(/#workflow$/);
    await expect(page.getByRole('heading', { name: 'One workflow. Your next chapter.' })).toBeInViewport();
    expect(privateRequests).toEqual([]);
    await page.getByRole('link', { name: /Get started/ }).click();
    await expect(page).toHaveURL(/#signup$/);
    await expect(page.getByRole('heading', { name: 'Create your account', exact: true })).toBeFocused();
    await expect(page.getByLabel('Email address')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Create your account', exact: true })).toBeVisible();
    await page.getByRole('link', { name: 'Log in', exact: true }).click();
    await expect(page).toHaveURL(/#login$/);
    await page.goBack();
    await expect(page.getByRole('heading', { name: 'Create your account', exact: true })).toBeVisible();
    await page.getByRole('link', { name: /Back to home/ }).click();
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Your experience.');
    expect(privateRequests).toEqual([]);
    await page.getByRole('link', { name: 'Log in', exact: true }).first().click();
    await page.getByLabel('Username').fill('sample');
    await page.getByLabel('Password').fill('incorrect-password');
    await page.getByRole('button', { name: 'Login', exact: true }).click();
    await expect(page.getByRole('alert')).toHaveText('Invalid credentials');
    await expect(page.getByRole('button', { name: 'Login', exact: true })).toBeEnabled();
  });
}
