import React, { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from '../lib/api';
import JobDetails from '../components/JobDetails';
import useIngestionRun from '../lib/useIngestionRun';
import { PageHeader, Notice, EmptyState, Panel } from '../components/Workspace';

const blank = { title: '', company: '', location: '', applyUrl: '', description: '' };
export default function JobsPage({ token }) {
  const [jobs, setJobs] = useState([]), [loading, setLoading] = useState(true), [saving, setSaving] = useState(false);
  const [filter, setFilter] = useState(''), [location, setLocation] = useState(''), [source, setSource] = useState('all'), [fresh, setFresh] = useState(false), [page, setPage] = useState(1);
  const [manual, setManual] = useState(blank), [showManual, setShowManual] = useState(false), [selected, setSelected] = useState(null), [error, setError] = useState('');
  const headers = { Authorization: `Bearer ${token}` };
  const load = async () => {
    setLoading(true); setError('');
    try { const result = await axios.get(`${API_BASE}/jobs`, { headers }); setJobs(result.data); }
    catch (e) { setError(apiError(e, 'Unable to load jobs. Please try again.')); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [token]);
  useEffect(() => { setPage(1); }, [filter, location, source, fresh]);
  const filtered = useMemo(() => jobs.filter(job => {
    const matchesText = [job.title, job.companyName, job.location].some(value => String(value || '').toLowerCase().includes(filter.toLowerCase()));
    const category = job.postingStatus === 'MANUAL_UNVERIFIED' ? 'manual' : job.postingStatus === 'PUBLIC_FEED_RECHECK_BEFORE_APPLYING' ? 'public' : 'unverified';
    return matchesText && String(job.location || '').toLowerCase().includes(location.toLowerCase()) && (source === 'all' || source === category) && (!fresh || (category === 'public' && !job.stale));
  }), [jobs, filter, location, source, fresh]);
  const pages = Math.max(1, Math.ceil(filtered.length / 20)), current = Math.min(page, pages);
  const discovery = useIngestionRun(token, load);
  const run = discovery.run;
  const report = run?.report;
  const saveManual = async event => {
    event.preventDefault(); setSaving(true); setError('');
    try { await axios.post(`${API_BASE}/jobs/manual`, manual, { headers }); setManual(blank); setShowManual(false); await load(); }
    catch (e) { setError(apiError(e, 'Unable to save this description.')); }
    finally { setSaving(false); }
  };
  return <div className="wp-page">
    <PageHeader eyebrow="Discover with direction" title="Jobs" description="Find the opportunities worth a closer look. Bring your own descriptions, or explore public employer listings."><button className="wp-button-primary" onClick={discovery.request} disabled={loading || discovery.loading || discovery.requesting || discovery.isActive}>{discovery.requesting ? 'Queuing…' : discovery.isActive ? 'Discovery in progress…' : 'Find jobs'}</button><button onClick={load} disabled={loading}>Refresh</button><button onClick={() => setShowManual(!showManual)}>{showManual ? 'Cancel pasted job' : 'Paste a job description'}</button></PageHeader>
    <Notice error>{error}</Notice>
    <Notice error>{discovery.error}</Notice>
    <Panel title="Discovery status" eyebrow="Public employer feeds"><p className="wp-muted">Feed refreshes run in the background. You can navigate away and return to the recorded status. Recent requests may reuse the last result to avoid checking employers too frequently.</p><button onClick={discovery.refresh} disabled={discovery.requesting}>Refresh discovery status</button>{discovery.loading ? <p role="status">Loading discovery status…</p> : !run ? <p>No discovery run recorded yet.</p> : <><p role="status"><strong>{{ QUEUED: 'Discovery queued', RUNNING: 'Discovery running', SUCCEEDED: 'Discovery completed', PARTIAL: 'Discovery completed with issues', FAILED: 'Discovery failed' }[run.status]}</strong> · Run #{run.id}</p>{run.finishedAt && <p className="wp-muted">Finished: {new Date(run.finishedAt).toLocaleString()}</p>}{run.error && <p>{run.error}</p>}{report && <><p>{report.checkedSources?.length || 0} feeds checked · {report.saved} listings saved · {report.archived} archived · {report.failed} storage errors.</p>{!report.checkedSources?.length && <p>No employer feed was checked successfully in this run. This does not verify any live vacancies.</p>}{Object.entries(report.sourceFailures || {}).map(([name, reason]) => <p key={name}>{name}: {reason}</p>)}</>}</>}</Panel>
    {showManual && <Panel title="Add a job for resume matching" eyebrow="Bring your own opportunity"><p className="wp-muted">Manually entered jobs are not independently verified.</p><form onSubmit={saveManual} className="wp-form"><div className="wp-form-grid">{[['title', 'Job title', true], ['company', 'Employer', true], ['location', 'Location', false], ['applyUrl', 'Official application URL (optional, HTTPS)', false]].map(([key, label, required]) => <label key={key}>{label}<input type={key === 'applyUrl' ? 'url' : 'text'} placeholder={label} required={required} maxLength={key === 'applyUrl' ? 500 : 255} value={manual[key]} onChange={e => setManual({ ...manual, [key]: e.target.value })} /></label>)}</div><label>Full description<textarea required minLength={40} maxLength={40000} rows={7} placeholder="Paste the full job description" value={manual.description} onChange={e => setManual({ ...manual, description: e.target.value })} /></label><div className="wp-actions"><button className="wp-button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save job for matching'}</button></div></form></Panel>}
    <div className="wp-stat-strip"><div><span>In your workspace</span><strong>{loading ? '—' : jobs.length}<small>opportunities</small></strong></div><div><span>After your filters</span><strong>{loading ? '—' : filtered.length}<small>to explore</small></strong></div><div><span>Always your decision</span><p>Review the source.<br />Confirm the current vacancy.</p></div></div>
    <Panel><div className="wp-filter-bar"><label>Search<input aria-label="Filter jobs" placeholder="Filter title, company or location" value={filter} onChange={e => setFilter(e.target.value)} /></label><label>Location<input aria-label="Filter location" placeholder="City, region or remote" value={location} onChange={e => setLocation(e.target.value)} /></label><label>Listing type<select aria-label="Listing type" value={source} onChange={e => setSource(e.target.value)}><option value="all">All listings</option><option value="public">Public employer feeds</option><option value="manual">Manually entered</option><option value="unverified">Other / unverified</option></select></label><label className="wp-checkbox-label"><input type="checkbox" checked={fresh} onChange={e => setFresh(e.target.checked)} /> Recently seen in a public feed</label></div>
    {loading ? <p role="status" className="wp-loading">Finding your opportunities…</p> : filtered.length === 0 ? <EmptyState title={jobs.length ? 'A little room to broaden the search.' : 'Your next opportunity starts here.'}>{jobs.length ? 'Try another location or clear a filter to see more roles.' : 'Choose Find jobs to refresh employer feeds, or paste a job description you would like to explore.'}</EmptyState> : <><div className="wp-table-scroll"><table className="wp-table jobs-table"><thead><tr><th>Opportunity</th><th>Employer</th><th>Location</th><th>Source & next step</th></tr></thead><tbody>{filtered.slice((current - 1) * 20, current * 20).map(job => <tr key={job.id}><td><button className="wp-title-link" onClick={() => setSelected(job.id)} aria-label={`Read job: ${job.title}`}>{job.title}</button></td><td>{job.companyName}</td><td>{job.location || 'Not specified'}</td><td><span className={`wp-badge ${job.postingStatus === 'PUBLIC_FEED_RECHECK_BEFORE_APPLYING' ? '' : 'wp-badge-neutral'}`}>{job.postingStatus === 'MANUAL_UNVERIFIED' ? 'Manually entered — unverified' : job.postingStatus === 'MOCK_NOT_A_VACANCY' ? 'Mock — not a vacancy' : job.postingStatus === 'SOURCE_UNVERIFIED' ? 'Unverified source' : job.source}</span>{job.stale && <small className="wp-table-note">Freshness needs review</small>}{/^https:\/\//i.test(job.applyUrl || '') && <a className="wp-inline-link" href={job.applyUrl} target="_blank" rel="noopener noreferrer">Official application</a>}</td></tr>)}</tbody></table></div><div className="wp-pagination"><p>{(current - 1) * 20 + 1}–{Math.min(current * 20, filtered.length)} of {filtered.length} opportunities</p><div className="wp-actions"><button aria-label="Previous jobs page" disabled={current === 1} onClick={() => setPage(current - 1)}>Previous</button><span>Page {current} / {pages}</span><button aria-label="Next jobs page" disabled={current === pages} onClick={() => setPage(current + 1)}>Next</button></div></div></>}
    </Panel>
    {selected && <JobDetails jobId={selected} token={token} onClose={() => setSelected(null)} />}
  </div>;
}
