import { test, expect } from '@playwright/test';

for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }, { width: 320, height: 740 }]) {
  test(`workspace navigation, filters and notes at ${viewport.width}px`, async ({ page }, testInfo) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.addInitScript(() => { localStorage.setItem('token', 'visual-test-token'); localStorage.setItem('user', 'sample_candidate'); });
    const jobs = Array.from({ length: 26 }, (_, i) => ({ id: i + 1, title: `Quality Engineer ${String(i + 1).padStart(2, '0')}`, companyName: 'Example Studio', location: i === 1 ? 'Remote Canada' : 'Toronto, Ontario', postingStatus: 'MANUAL_UNVERIFIED', source: 'MANUAL', stale: true }));
    let record = { id: 1, job: { id: 1, title: 'Quality Engineer 01' }, applicationStatus: 'USER_REPORTED_SUBMITTED', appliedDate: '2026-10-28T12:00:00', notes: 'Sample record only; no application was sent.', providerConfirmed: false };
    await page.route('**/api/**', async route => {
      const path = new URL(route.request().url()).pathname;
      let data = [];
      if (path === '/api/jobs') data = jobs;
      if (path === '/api/resumes') data = [{ id: 1, filename: 'sample-resume.txt', active: true }];
      if (path === '/api/matches') data = [{ jobId: 1, title: 'Quality Engineer 01', company: 'Example Studio', score: 74.2, source: 'MANUAL', matchedSkills: ['java', 'selenium', 'junit'], skillsNotEvidenced: ['playwright'], explanation: 'Illustrative skill evidence. Review requirements before using this ranking.', locationPreferenceMatched: true }];
      if (path === '/api/tailoring/documents') data = [{ id: 1, jobId: 1, resumeId: 1, content: 'Sample candidate — demonstration text only.', approved: false }];
      if (path === '/api/applications') data = [record];
      if (path === '/api/applications/1') { record = { ...record, ...route.request().postDataJSON(), applicationStatus: route.request().postDataJSON().status }; data = record; }
      if (path === '/api/profile') data = { username: 'sample_candidate', fullName: 'Sample Candidate', email: 'sample@example.com', targetRoles: 'Quality Engineer, SDET', targetLocations: 'Toronto, Remote Canada' };
      await route.fulfill({ json: data });
    });
    const noOverflow = async () => expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Jobs', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Jobs', exact: true })).toHaveAttribute('aria-current', 'page');
    await expect(page.getByRole('button', { name: 'Read job: Quality Engineer 01', exact: true })).toBeVisible();
    await noOverflow();
    await page.screenshot({ path: testInfo.outputPath('jobs.png'), fullPage: viewport.width < 500 });
    await page.getByRole('button', { name: 'Next jobs page' }).click();
    await expect(page.getByRole('button', { name: 'Read job: Quality Engineer 21', exact: true })).toBeVisible();
    await page.getByRole('textbox', { name: 'Filter location' }).fill('Remote');
    await expect(page.getByRole('button', { name: 'Read job: Quality Engineer 02', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Next jobs page' })).toBeDisabled();
    await page.getByRole('checkbox', { name: 'Recently seen in a public feed' }).check();
    await expect(page.getByRole('heading', { name: 'A little room to broaden the search.' })).toBeVisible();
    await page.getByRole('button', { name: 'Resumes', exact: true }).click();
    await expect(page.getByText('sample-resume.txt', { exact: true })).toBeVisible();
    await noOverflow(); await page.screenshot({ path: testInfo.outputPath('resumes.png'), fullPage: viewport.width < 500 });
    await page.getByRole('button', { name: 'Matches', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Quality Engineer 01', exact: true })).toBeVisible();
    await noOverflow(); await page.screenshot({ path: testInfo.outputPath('matches.png'), fullPage: viewport.width < 500 });
    await page.getByRole('button', { name: 'Applications', exact: true }).click();
    await expect(page.getByRole('combobox', { name: 'Update status for Quality Engineer 01' })).toHaveValue('USER_REPORTED_SUBMITTED');
    await noOverflow(); await page.screenshot({ path: testInfo.outputPath('applications.png'), fullPage: viewport.width < 500 });
    await page.getByRole('button', { name: 'Edit notes', exact: true }).click();
    await page.getByRole('textbox', { name: 'Notes for Quality Engineer 01' }).fill('Sample follow-up reminder.');
    await page.getByRole('button', { name: 'Save notes', exact: true }).click();
    await expect(page.getByText('Sample follow-up reminder.', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Profile & preferences', exact: true }).click();
    await expect(page.getByLabel('Full name')).toHaveValue('Sample Candidate');
    await noOverflow(); await page.screenshot({ path: testInfo.outputPath('preferences.png'), fullPage: viewport.width < 500 });
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Preferences saved');
  });
}
