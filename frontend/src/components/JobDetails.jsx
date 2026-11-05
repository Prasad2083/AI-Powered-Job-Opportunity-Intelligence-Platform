import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from '../lib/api';
import { Arrow } from './HomePage';

// Provider descriptions are display-only text; no provider HTML enters the DOM.
export function plainDescription(value = '') {
  const entities = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ' };
  return value.replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, '')
    .replace(/<br\s*\/?\s*>|<\/(?:p|li|div|h[1-6])\s*>/gi, '\n')
    .replace(/<\/?[a-z][^>]*>/gi, '')
    .replace(/&(#x[0-9a-f]+|#\d+|amp|lt|gt|quot|apos|nbsp);/gi, (entity, name) => {
      if (!name.startsWith('#')) return entities[name.toLowerCase()];
      const number = name.toLowerCase().startsWith('#x') ? parseInt(name.slice(2), 16) : parseInt(name.slice(1), 10);
      return number > 0 && number <= 0x10ffff ? String.fromCodePoint(number) : entity;
    }).trim();
}

export default function JobDetails({ jobId, token, onClose }) {
  const [details, setDetails] = useState(null);
  const [error, setError] = useState('');
  const heading = useRef(null);
  useEffect(() => {
    const controller = new AbortController();
    const previousFocus = document.activeElement;
    setDetails(null); setError('');
    heading.current?.focus();
    axios.get(`${API_BASE}/jobs/${jobId}`, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal })
      .then(response => { if (!controller.signal.aborted) setDetails(response.data); })
      .catch(failure => {
        if (!controller.signal.aborted) setError(failure.response?.status === 404 ? 'This job is no longer available in your workspace.' : apiError(failure, 'Unable to load this job. Please try again.'));
      });
    return () => { controller.abort(); if (previousFocus?.isConnected) previousFocus.focus({ preventScroll: true }); };
  }, [jobId, token]);

  return <section className="job-detail-panel" aria-labelledby="job-detail-heading">
    <div className="job-detail-heading"><h3 ref={heading} id="job-detail-heading" tabIndex={-1}>Job description</h3><button type="button" onClick={onClose}>Close job details</button></div>
    {error && <p role="alert">{error}</p>}
    {!details && !error && <p role="status">Loading job description…</p>}
    {details && <><h4>{details.title}</h4><p>{details.companyName} · {details.location || 'Location unspecified'}</p>
      <p className="job-detail-provenance">{details.postingStatus === 'MANUAL_UNVERIFIED' ? 'Manually entered — not independently verified.' : details.postingStatus === 'MOCK_NOT_A_VACANCY' ? 'Mock data — not a live vacancy.' : details.postingStatus === 'SOURCE_UNVERIFIED' ? 'Source and vacancy status are unverified.' : 'Public employer listing — confirm availability before applying.'}</p>
      <dl><div><dt>Source</dt><dd>{details.source}</dd></div><div><dt>{details.postingStatus === 'PUBLIC_FEED_RECHECK_BEFORE_APPLYING' ? 'Publication date' : 'Recorded date'}</dt><dd>{details.postedDate ? new Date(details.postedDate).toLocaleDateString() : 'Not supplied'}</dd></div><div><dt>Last seen in a feed</dt><dd>{details.lastSeenAt ? new Date(details.lastSeenAt).toLocaleString() : 'Not verified'}</dd></div></dl>
      {details.stale && <p>Freshness needs review. Confirm the current details on the employer’s website.</p>}
      <div className="job-detail-description">{plainDescription(details.description) || 'No description supplied.'}</div>
      {/^https:\/\//i.test(details.applyUrl || '') && <a href={details.applyUrl} target="_blank" rel="noopener noreferrer">Open official application <Arrow /></a>}
    </>}
  </section>;
}
