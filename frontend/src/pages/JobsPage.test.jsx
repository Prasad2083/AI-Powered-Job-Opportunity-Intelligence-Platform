import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from 'axios';
import JobsPage from './JobsPage';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn() } }));
const report = { checkedSources: [], saved: 0, archived: 0, failed: 0, sourceFailures: {} };
let latest;
beforeEach(() => {
  latest = { status: 204 };
  axios.get.mockImplementation(async url => url.endsWith('/jobs/ingestion') ? latest : { data: [] });
});
afterEach(() => { cleanup(); vi.clearAllMocks(); });

it('queues discovery without blocking manual entry and restores its completed report', async () => {
  const user = userEvent.setup();
  axios.post.mockResolvedValue({ status: 202, data: { id: 12, status: 'QUEUED' } });
  const view = render(<JobsPage token="test-token" />);
  await screen.findByText('No discovery run recorded yet.');
  latest = { data: { id: 12, status: 'RUNNING' } };
  await user.click(screen.getByRole('button', { name: 'Find jobs' }));
  expect(await screen.findByText('Discovery running')).toBeTruthy();
  expect(screen.getByRole('button', { name: 'Discovery in progress…' }).disabled).toBe(true);
  await user.click(screen.getByRole('button', { name: 'Paste a job description' }));
  await user.type(screen.getByLabelText('Job title'), 'My manual description');
  latest = { data: { id: 12, status: 'SUCCEEDED', report } };
  await user.click(screen.getByRole('button', { name: 'Refresh discovery status' }));
  expect(await screen.findByText('Discovery completed')).toBeTruthy();
  expect(screen.getByLabelText('Job title').value).toBe('My manual description');
  expect(screen.getByText(/This does not verify any live vacancies/)).toBeTruthy();
  await waitFor(() => expect(axios.get.mock.calls.filter(([url]) => url.endsWith('/jobs')).length).toBe(2));
  expect(axios.post).toHaveBeenCalledTimes(1);
  view.unmount();
  render(<JobsPage token="test-token" />);
  expect(await screen.findByText('Discovery completed')).toBeTruthy();
});

it('shows partial failure and provider messages without claiming success', async () => {
  latest = { data: { id: 13, status: 'PARTIAL', report: { ...report, checkedSources: ['public:fixture'], sourceFailures: { 'public:other': 'Retry later' } } } };
  render(<JobsPage token="test-token" />);
  expect(await screen.findByText('Discovery completed with issues')).toBeTruthy();
  expect(screen.getByText('public:other: Retry later')).toBeTruthy();
  expect(screen.queryByText('Discovery completed', { exact: true })).toBeNull();
});

it('keeps listings and manual entry available when queue admission fails', async () => {
  const user = userEvent.setup();
  axios.post.mockRejectedValue({ response: { status: 503, data: { message: 'Unavailable' } } });
  render(<JobsPage token="test-token" />);
  await screen.findByText('No discovery run recorded yet.');
  await user.click(screen.getByRole('button', { name: 'Find jobs' }));
  expect(await screen.findByRole('alert')).toBeTruthy();
  expect(screen.queryByText('Discovery completed')).toBeNull();
  expect(screen.getByRole('button', { name: 'Find jobs' }).disabled).toBe(false);
  await user.click(screen.getByRole('button', { name: 'Paste a job description' }));
  expect(screen.getByLabelText('Full description')).toBeTruthy();
});
