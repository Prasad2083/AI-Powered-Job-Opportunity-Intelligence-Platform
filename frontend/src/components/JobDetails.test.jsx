import React from 'react';
import { it, expect, vi, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import axios from 'axios';
import JobDetails, { plainDescription } from './JobDetails';
vi.mock('axios', () => ({ default: { get: vi.fn() } }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

it('renders readable provider text without executing HTML or hiding verification limits', async () => {
  axios.get.mockResolvedValue({ data: { id: 1, title: 'Quality Engineer', companyName: 'Example', description: '<p>Java &amp; Selenium</p><script>window.jobInjected = true</script><img src=x onerror="window.jobInjected=true"><p>Review requirements.</p>', source: 'MANUAL', postingStatus: 'MANUAL_UNVERIFIED', stale: true } });
  render(<JobDetails jobId={1} token="test-token" onClose={() => {}} />);
  expect(await screen.findByText('Quality Engineer')).toBeTruthy();
  expect(screen.getByText(/Java & Selenium/).textContent).toContain('Review requirements.');
  expect(screen.getByText('Manually entered — not independently verified.')).toBeTruthy();
  expect(screen.getByText('Not supplied')).toBeTruthy();
  expect(document.querySelector('img')).toBeNull();
  expect(window.jobInjected).toBeUndefined();
  expect(axios.get.mock.calls[0][1].headers.Authorization).toBe('Bearer test-token');
});

it('explains unavailable or inaccessible jobs without displaying their content', async () => {
  axios.get.mockRejectedValue({ response: { status: 404 } });
  render(<JobDetails jobId={99} token="test-token" onClose={() => {}} />);
  expect((await screen.findByRole('alert')).textContent).toContain('no longer available');
  expect(screen.queryByText('Open official application')).toBeNull();
});

it('preserves plain-text comparisons and safely decodes numeric entities', () => {
  expect(plainDescription('Experience <3 years; API testing &#38; SQL &#x26; Java')).toBe('Experience <3 years; API testing & SQL & Java');
});
