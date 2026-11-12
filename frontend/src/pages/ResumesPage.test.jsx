import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import ResumesPage from './ResumesPage';

vi.mock('axios', () => ({ default: { get: vi.fn(), put: vi.fn(), post: vi.fn() } }));
const rows = [{ id: 1, filename: 'first.txt', active: true }, { id: 2, filename: 'second.txt', active: false }];
const pending = () => { let resolve, reject; const promise = new Promise((ok, fail) => { resolve = ok; reject = fail; }); return { promise, resolve, reject }; };
beforeEach(() => axios.get.mockImplementation(async url => url.endsWith('/resumes') ? { data: rows } : { data: { text: 'Original extracted text' } }));
afterEach(() => { cleanup(); vi.resetAllMocks(); });

it('opens the latest selected resume even when an older request resolves last', async () => {
  const one = pending(), two = pending();
  let firstSignal;
  axios.get.mockImplementation((url, config) => {
    if (url.endsWith('/resumes')) return Promise.resolve({ data: rows });
    if (url.endsWith('/1/text')) { firstSignal = config.signal; return one.promise; }
    return two.promise;
  });
  render(<ResumesPage token="test-token" />);
  await screen.findByText('first.txt');
  const buttons = screen.getAllByRole('button', { name: 'Preview / edit text' });
  fireEvent.click(buttons[0]); fireEvent.click(buttons[1]);
  expect(firstSignal.aborted).toBe(true);
  await act(async () => two.resolve({ data: { text: 'Second resume text' } }));
  expect(screen.getByRole('textbox', { name: 'Resume text' }).value).toBe('Second resume text');
  await act(async () => one.resolve({ data: { text: 'First stale text' } }));
  expect(screen.getByRole('textbox', { name: 'Resume text' }).value).toBe('Second resume text');
  axios.put.mockResolvedValue({ data: {} });
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
  await waitFor(() => expect(axios.put).toHaveBeenCalledWith(expect.stringContaining('/resumes/2/text'), { text: 'Second resume text' }, expect.anything()));
});

it('protects an open editor and freezes mutations while the exact draft is saving', async () => {
  const saved = pending(); axios.put.mockReturnValue(saved.promise);
  render(<ResumesPage token="test-token" />);
  await screen.findByText('first.txt');
  fireEvent.click(screen.getAllByRole('button', { name: 'Preview / edit text' })[0]);
  const editor = await screen.findByRole('textbox', { name: 'Resume text' });
  fireEvent.change(editor, { target: { value: 'My reviewed edit' } });
  expect(screen.getAllByRole('button', { name: 'Preview / edit text' }).every(b => b.disabled)).toBe(true);
  expect(screen.getByRole('button', { name: 'Use for matching' }).disabled).toBe(true);
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
  expect(editor.disabled).toBe(true);
  expect(screen.getByRole('button', { name: 'Cancel', exact: true }).disabled).toBe(true);
  expect(screen.getByRole('button', { name: 'Saving…' }).disabled).toBe(true);
  expect(axios.put).toHaveBeenCalledTimes(1);
  expect(axios.put.mock.calls[0][1]).toEqual({ text: 'My reviewed edit' });
  await act(async () => saved.resolve({ data: {} }));
  expect(await screen.findByText('Extracted text saved. Your uploaded original is unchanged.')).toBeTruthy();
  expect(screen.queryByRole('textbox', { name: 'Resume text' })).toBeNull();
});

it('retains unsaved text after a failed save and allows an explicit retry', async () => {
  axios.put.mockRejectedValueOnce({ response: { status: 503, data: { message: 'Please retry' } } }).mockResolvedValueOnce({ data: {} });
  render(<ResumesPage token="test-token" />);
  await screen.findByText('first.txt');
  fireEvent.click(screen.getAllByRole('button', { name: 'Preview / edit text' })[0]);
  const editor = await screen.findByRole('textbox', { name: 'Resume text' });
  fireEvent.change(editor, { target: { value: 'Keep this unsaved draft' } });
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
  expect((await screen.findByRole('alert')).textContent).toContain('Please retry');
  expect(editor.value).toBe('Keep this unsaved draft'); expect(editor.disabled).toBe(false);
  fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
  await screen.findByText('Extracted text saved. Your uploaded original is unchanged.');
  expect(axios.put).toHaveBeenCalledTimes(2);
});

it('aborts an unfinished preview when leaving the page', async () => {
  const request = pending(); let signal;
  axios.get.mockImplementation((url, config) => url.endsWith('/resumes') ? Promise.resolve({ data: rows }) : (signal = config.signal, request.promise));
  const view = render(<ResumesPage token="test-token" />);
  await screen.findByText('first.txt');
  fireEvent.click(screen.getAllByRole('button', { name: 'Preview / edit text' })[0]);
  view.unmount(); expect(signal.aborted).toBe(true);
  await act(async () => request.reject(new Error('late error')));
});
