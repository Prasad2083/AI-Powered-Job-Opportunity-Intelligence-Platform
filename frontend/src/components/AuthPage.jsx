import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from '../lib/api';
import { Brand, Arrow } from './HomePage';

export default function AuthPage({ mode, setToken, setUser }) {
  const isLogin = mode === 'login';
  const [formData, setFormData] = useState({ username: '', password: '', email: '', fullName: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    setError('');
    setFormData(previous => ({ ...previous, password: '' }));
  }, [mode]);

  const update = event => setFormData(previous => ({ ...previous, [event.target.name]: event.target.value }));
  const handleSubmit = async event => {
    event.preventDefault();
    if (busy) return;
    setError(''); setNotice(''); setBusy(true);
    try {
      const payload = isLogin ? { username: formData.username, password: formData.password } : formData;
      const response = await axios.post(`${API_BASE}/auth/${isLogin ? 'login' : 'register'}`, payload);
      if (!isLogin) {
        setFormData(previous => ({ ...previous, password: '' }));
        setNotice('Account created. Log in to begin your next chapter.');
        window.location.hash = 'login';
      } else if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', response.data.username);
        setUser(response.data.username); setToken(response.data.token);
      } else {
        setError('Login could not be confirmed. Please try again.');
      }
    } catch (failure) {
      setError(apiError(failure, 'Unable to connect. Check that JobPulse is running, then try again.'));
    } finally {
      setBusy(false);
    }
  };

  return <div className="jp-public jp-auth">
    <header className="jp-site-header"><Brand /><a className="jp-text-link" href="#">Back to home <Arrow /></a></header>
    <main className="jp-auth-layout">
      <section className="jp-auth-intro" aria-label="Your JobPulse workspace">
        <p className="jp-eyebrow">Your next chapter starts here</p>
        <h2>A little clarity.<br /><em>A new possibility.</em></h2>
        <p>Bring your experience, your preferences and your ambition. Give your next move the attention it deserves.</p>
        <ul><li>Discover opportunities with direction.</li><li>Understand the evidence behind a match.</li><li>Review drafts grounded in your real experience.</li></ul>
        <p className="jp-auth-note">Your original resume stays intact. Every application remains your decision.</p>
      </section>
      <section className="jp-auth-form-panel" aria-labelledby="public-page-title">
        <p className="jp-eyebrow">{isLogin ? 'Good to see you again' : 'Make space for what comes next'}</p>
        <h1 id="public-page-title" tabIndex={-1}>{isLogin ? 'Login' : 'Create your account'}</h1>
        <p className="jp-auth-description">{isLogin ? 'Your opportunities and progress, all in one place.' : 'Start a more thoughtful, organised job search.'}</p>
        {notice && <p role="status" className="jp-auth-notice">{notice}</p>}
        {error && <p role="alert" className="jp-auth-error">{error}</p>}
        <form onSubmit={handleSubmit} aria-label={isLogin ? 'Login' : 'Create account'}>
          <fieldset disabled={busy}>
            <label htmlFor="auth-username">Username</label>
            <input id="auth-username" name="username" placeholder="Username" autoComplete="username" value={formData.username} onChange={update} maxLength={100} required />
            {!isLogin && <><label htmlFor="auth-email">Email address</label><input id="auth-email" name="email" type="email" placeholder="Email" autoComplete="email" value={formData.email} onChange={update} maxLength={255} required /><label htmlFor="auth-fullname">Full name <span>(optional)</span></label><input id="auth-fullname" name="fullName" placeholder="Full Name" autoComplete="name" value={formData.fullName} onChange={update} maxLength={255} /></>}
            <label htmlFor="auth-password">Password</label>
            <input id="auth-password" name="password" type="password" placeholder="Password" autoComplete={isLogin ? 'current-password' : 'new-password'} value={formData.password} onChange={update} minLength={isLogin ? undefined : 12} maxLength={72} aria-describedby={!isLogin ? 'password-guidance' : undefined} required />
            {!isLogin && <p id="password-guidance" className="jp-field-help">Use 12–72 characters. Unicode passwords must also fit within 72 UTF-8 bytes.</p>}
            <button type="submit" className="jp-button" disabled={busy}>{busy ? (isLogin ? 'Logging in…' : 'Creating account…') : (isLogin ? 'Login' : 'Create account')} <Arrow /></button>
          </fieldset>
        </form>
        <p className="jp-auth-switch">{isLogin ? 'New to JobPulse?' : 'Already have an account?'} <a href={isLogin ? '#signup' : '#login'} aria-disabled={busy} onClick={event => { if (busy) event.preventDefault(); }}>{isLogin ? 'Create an account' : 'Log in'}</a></p>
      </section>
    </main>
    <footer className="jp-auth-footer">JobPulse <span aria-hidden="true">/</span> Discover with clarity. Prepare with confidence.</footer>
  </div>;
}
