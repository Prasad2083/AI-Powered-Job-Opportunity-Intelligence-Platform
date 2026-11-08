import React from 'react';
import { it, expect, vi, afterEach } from 'vitest';
import { render, screen, cleanup, fireEvent, waitFor } from '@testing-library/react';
import axios from 'axios';
import MatchesPage from './MatchesPage';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn() } }));
afterEach(() => { cleanup(); vi.resetAllMocks(); });

const task = { id: 7, jobId: 1, resumeId: 2, status: 'SUCCEEDED', draft: 'Original truthful resume text' };
function fixtures(initial = []) {
  let history = initial;
  axios.get.mockImplementation(async url => {
    if (url.endsWith('/matches')) return { data: [{ jobId: 1, title: 'Quality Engineer', company: 'Explicit test fixture', score: 70 }] };
    if (url.endsWith('/tailoring/documents')) return { data: [] };
    if (url.endsWith('/tailoring/tasks')) return { data: history };
    if (url.endsWith('/tailoring/tasks/7')) return { data: task };
    throw new Error(`Unexpected GET ${url}`);
  });
  return value => { history = value; };
}

it('queues work and requires opening its result so completion cannot overwrite an editor', async () => {
  const setHistory = fixtures();
  axios.post.mockImplementation(async url => {
    expect(url.endsWith('/tailoring/tasks/1')).toBe(true);
    setHistory([task]); return { data: { ...task, status: 'QUEUED', draft: null } };
  });
  render(<MatchesPage token="test-token" />);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Generate truthful tailored draft' }).disabled).toBe(false));
  fireEvent.click(screen.getByRole('button', { name: 'Generate truthful tailored draft' }));
  await screen.findByRole('button', { name: 'Open draft task #7' });
  expect(screen.queryByRole('textbox', { name: 'Edit tailored resume' })).toBeNull();
  fireEvent.click(screen.getByRole('button', { name: 'Open draft task #7' }));
  expect((await screen.findByRole('textbox', { name: 'Edit tailored resume' })).value).toBe(task.draft);
});

it('restores a running task, prevents duplicate work and allows cancellation', async () => {
  const setHistory = fixtures([{ ...task, status: 'RUNNING', draft: null }]);
  axios.post.mockImplementation(async url => {
    expect(url.endsWith('/tailoring/tasks/7/cancel')).toBe(true);
    setHistory([{ ...task, status: 'CANCELLED', draft: null }]); return { data: {} };
  });
  render(<MatchesPage token="test-token" />);
  await screen.findByText('Preparing local AI draft');
  expect(screen.getByRole('button', { name: 'Generate truthful tailored draft' }).disabled).toBe(true);
  fireEvent.click(screen.getByRole('button', { name: 'Cancel draft task #7' }));
  await screen.findByText('Cancelled', { exact: true });
  expect(screen.queryByRole('button', { name: 'Open draft task #7' })).toBeNull();
});

it('saves the completed task reference and keeps approval explicit', async () => {
  fixtures([task]);
  axios.post.mockResolvedValue({ data: { id: 42 } });
  render(<MatchesPage token="test-token" />);
  fireEvent.click(await screen.findByRole('button', { name: 'Open draft task #7' }));
  await screen.findByRole('textbox', { name: 'Edit tailored resume' });
  fireEvent.click(screen.getByRole('button', { name: 'Save new version' }));
  await waitFor(() => expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/tailoring/documents'),
    { resumeId: 2, jobId: 1, content: task.draft, approved: false, taskId: 7, sourceDocumentId: null }, expect.anything()));
});

it('shows failed tasks and queues a separate task only on explicit retry', async () => {
  const setHistory = fixtures([{ ...task, status: 'FAILED', error: 'Local AI unavailable', draft: null }]);
  axios.post.mockImplementation(async () => { setHistory([{ ...task, id: 8, status: 'QUEUED', draft: null }]); return { data: { ...task, id: 8, status: 'QUEUED' } }; });
  render(<MatchesPage token="test-token" />);
  await screen.findByText('Local AI unavailable');
  expect(axios.post).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Retry draft for job #1' }));
  await screen.findByText('Queued', { exact: true });
  expect(axios.post).toHaveBeenCalledTimes(1);
});
