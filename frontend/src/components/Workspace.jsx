import React, { useEffect } from 'react';
import { Brand } from './HomePage';
import './Workspace.css';

const sections = [['jobs', 'Jobs', 'Discover'], ['resumes', 'Resumes', 'Your experience'], ['matches', 'Matches', 'Prepare'], ['applications', 'Applications', 'Your progress'], ['settings', 'Profile & preferences', 'Make it yours']];

export default function Workspace({ page, onNavigate, user, onLogout, loggingOut, children }) {
  useEffect(() => { document.getElementById('workspace-heading')?.focus({ preventScroll: false }); }, [page]);
  return <div className="jp-workspace">
    <a className="wp-skip" href="#workspace-main">Skip to workspace</a>
    <aside className="wp-sidebar">
      <Brand onClick={() => onNavigate('jobs')} />
      <div className="wp-sidebar-intro"><span className="wp-eyebrow">Your next chapter</span><p>A little structure.<br />A clearer direction.</p></div>
      <nav aria-label="Workspace navigation">{sections.map(([key, name, hint], index) => <button key={key} type="button" aria-current={page === key ? 'page' : undefined} onClick={() => onNavigate(key)}><span className="wp-nav-number" aria-hidden="true">0{index + 1}</span><span>{name}<small aria-hidden="true">{hint}</small></span><span aria-hidden="true" className="wp-nav-dot" /></button>)}</nav>
      <div className="wp-sidebar-bottom"><span className="wp-account-avatar" aria-hidden="true">{(user || 'J').slice(0, 1).toUpperCase()}</span><div className="wp-account-name"><strong>{user || 'Your account'}</strong><small>Your personal workspace</small></div><button type="button" onClick={onLogout} disabled={loggingOut}>{loggingOut ? 'Leaving…' : 'Logout'}</button></div>
    </aside>
    <div className="wp-body"><header className="wp-topbar"><p><span className="wp-status-dot" aria-hidden="true" /> Your opportunity workspace</p><span>Discover with clarity. Prepare with confidence.</span></header><main id="workspace-main" tabIndex={-1}>{children}</main><footer className="wp-footer">Your experience at the centre. <span>Thoughtful preparation. Human decisions.</span></footer></div>
  </div>;
}

export function PageHeader({ eyebrow, title, description, children }) {
  return <header className="wp-page-header"><div><p className="wp-eyebrow">{eyebrow}</p><h1 id="workspace-heading" tabIndex={-1}>{title}</h1><p className="wp-page-description">{description}</p></div>{children && <div className="wp-actions">{children}</div>}</header>;
}
export function Notice({ error, children }) {
  return children ? <div className={`wp-notice ${error ? 'wp-notice-error' : ''}`} role={error ? 'alert' : 'status'}>{children}</div> : null;
}
export function EmptyState({ title, children, action }) {
  return <div className="wp-empty"><span className="wp-empty-mark" aria-hidden="true">◇</span><h3>{title}</h3><p>{children}</p>{action}</div>;
}
export function Panel({ title, eyebrow, children, className = '' }) {
  return <section className={`wp-panel ${className}`}>{(eyebrow || title) && <header className="wp-panel-heading">{eyebrow && <p className="wp-eyebrow">{eyebrow}</p>}{title && <h2>{title}</h2>}</header>}{children}</section>;
}
