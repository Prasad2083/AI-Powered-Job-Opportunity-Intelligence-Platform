import React from 'react';
import { it, expect, vi, afterEach, beforeEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from 'axios';
import App from './App';

vi.mock('axios', () => ({ default: { post: vi.fn(), get: vi.fn(), interceptors: { response: { use: vi.fn(), eject: vi.fn() } } } }));
beforeEach(() => {
  window.history.replaceState({}, '', '/');
  axios.get.mockImplementation(async url => url.endsWith('/jobs/ingestion') ? { status: 204 } : { data: [] });
});
afterEach(() => { cleanup(); localStorage.clear(); vi.clearAllMocks(); window.history.replaceState({}, '', '/'); });

it('shows the public home page without requesting private account data', () => {
  render(<App />);
  expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Your experience.');
  expect(screen.getByRole('link', { name: /Sign up/ }).getAttribute('href')).toBe('#signup');
  expect(axios.get).not.toHaveBeenCalled();
  expect(screen.getByText(/automatic submission is not currently enabled/i)).toBeTruthy();
});

it('opens signup from home and returns to login after successful registration', async () => {
  axios.post.mockResolvedValue({ data: { id: 1, username: 'alice' } });
  const user = userEvent.setup(); render(<App />);
  await user.click(screen.getByRole('link', { name: /Sign up/ }));
  await user.type(screen.getByLabelText('Username'), 'alice');
  await user.type(screen.getByLabelText('Password'), 'test-password-123');
  await user.type(screen.getByLabelText('Email address'), 'alice@example.com');
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(await screen.findByRole('heading', { name: 'Login', exact: true })).toBeTruthy();
  expect(screen.getByRole('status').textContent).toContain('Account created');
  expect(screen.getByLabelText('Password').value).toBe('');
  expect(localStorage.getItem('token')).toBeNull();
});

it('shows useful server validation messages inline and keeps the form available', async () => {
  window.history.replaceState({}, '', '/#signup');
  axios.post.mockRejectedValue({ response: { status: 400, data: { fieldErrors: { username: 'Username already exists' } } } });
  const user = userEvent.setup(); render(<App />);
  await user.type(screen.getByLabelText('Username'), 'alice');
  await user.type(screen.getByLabelText('Email address'), 'alice@example.com');
  await user.type(screen.getByLabelText('Password'), 'test-password-123');
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect((await screen.findByRole('alert')).textContent).toContain('Username already exists');
  expect(screen.getByRole('button', { name: 'Create account' }).disabled).toBe(false);
});

it('supports direct login and prevents duplicate requests while pending', async () => {
  window.history.replaceState({}, '', '/#login');
  let rejectLogin;
  axios.post.mockReturnValue(new Promise((_resolve, reject) => { rejectLogin = reject; }));
  const user = userEvent.setup(); render(<App />);
  await user.type(screen.getByLabelText('Username'), 'alice');
  await user.type(screen.getByLabelText('Password'), 'incorrect-password');
  await user.click(screen.getByRole('button', { name: 'Login', exact: true }));
  expect(screen.getByRole('button', { name: /Logging in/ }).disabled).toBe(true);
  expect(axios.post).toHaveBeenCalledTimes(1);
  rejectLogin({ response: { status: 401, data: { error: 'Invalid credentials' } } });
  expect((await screen.findByRole('alert')).textContent).toBe('Invalid credentials');
  expect(localStorage.getItem('token')).toBeNull();
});

it('can return home without authenticating', async () => {
  window.history.replaceState({}, '', '/#login');
  const user = userEvent.setup(); render(<App />);
  await user.click(screen.getByRole('link', { name: /Back to home/ }));
  expect((await screen.findByRole('heading', { level: 1 })).textContent).toContain('Your experience.');
  expect(axios.post).not.toHaveBeenCalled();
});
