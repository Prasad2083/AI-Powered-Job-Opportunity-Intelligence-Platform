import { test, expect } from '@playwright/test';

test('resume save locks the editor and retains text on failure', async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem('token', 'editor-test-token'); localStorage.setItem('user', 'editor-fixture'); });
  let finishSave;
  const saveGate = new Promise(resolve => { finishSave = resolve; });
  let fail = true;
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/jobs/ingestion') return route.fulfill({ status: 204 });
    if (path === '/api/resumes') return route.fulfill({ json: [{ id: 1, filename: 'fixture.txt', active: true }] });
    if (path === '/api/resumes/1/text') {
      if (route.request().method() === 'GET') return route.fulfill({ json: { text: 'Explicit test resume fixture' } });
      if (fail) { await saveGate; fail = false; return route.fulfill({ status: 503, json: { message: 'Fixture save unavailable; retry' } }); }
      return route.fulfill({ json: {} });
    }
    return route.fulfill({ json: [] });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Resumes', exact: true }).click();
  await page.getByRole('button', { name: 'Preview / edit text' }).click();
  const editor = page.getByRole('textbox', { name: 'Resume text' });
  await editor.fill('My reviewed text');
  await expect(page.getByRole('button', { name: 'Preview / edit text' })).toBeDisabled();
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(editor).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Cancel', exact: true })).toBeDisabled();
  finishSave();
  await expect(page.getByRole('alert')).toContainText('Fixture save unavailable; retry');
  await expect(editor).toHaveValue('My reviewed text');
  await expect(editor).toBeEnabled();
  await page.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(page.getByText('Extracted text saved. Your uploaded original is unchanged.', { exact: true })).toBeVisible();
});
