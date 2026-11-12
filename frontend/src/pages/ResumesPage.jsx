import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from '../lib/api';
import { downloadBlob } from '../lib/download';
import { PageHeader, Notice, EmptyState, Panel } from '../components/Workspace';

export default function ResumesPage({ token }) {
  const [resumes, setResumes] = useState([]), [file, setFile] = useState(null), [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [uploading, setUploading] = useState(false);
  const [editing, setEditing] = useState(null), [text, setText] = useState(''), [error, setError] = useState(''), [notice, setNotice] = useState('');
  const input = useRef(null), editor = useRef(null), preview = useRef(null);
  const [opening, setOpening] = useState(null);
  const headers = { Authorization: `Bearer ${token}` };
  const load = async () => { setLoading(true); try { const result = await axios.get(`${API_BASE}/resumes`, { headers }); setResumes(result.data); } catch (e) { setError(apiError(e, 'Unable to load your resumes.')); } finally { setLoading(false); } };
  useEffect(() => { load(); return () => preview.current?.abort(); }, [token]);
  useEffect(() => { if (editing) editor.current?.focus(); }, [editing]);
  const upload = async event => {
    event.preventDefault(); if (!file || busy || opening !== null || editing !== null) return;
    setUploading(true); setBusy(true); setError(''); setNotice('');
    try { const data = new FormData(); data.append('file', file); await axios.post(`${API_BASE}/resumes/upload`, data, { headers }); setFile(null); if (input.current) input.current.value = ''; setNotice('Resume uploaded. Your original is preserved and this version is selected for matching.'); await load(); }
    catch (e) { setError(apiError(e, 'Unable to upload this resume.')); } finally { setUploading(false); setBusy(false); }
  };
  const open = async id => {
    if (busy || editing !== null) return;
    preview.current?.abort();
    const controller = new AbortController(); preview.current = controller;
    setOpening(id); setError(''); setNotice('');
    try {
      const result = await axios.get(`${API_BASE}/resumes/${id}/text`, { headers, signal: controller.signal });
      if (controller.signal.aborted) return;
      setEditing(id); setText(result.data.text);
    } catch (e) { if (!controller.signal.aborted) setError(apiError(e, 'Unable to open this resume.')); }
    finally { if (preview.current === controller) { preview.current = null; setOpening(null); } }
  };
  const save = async () => {
    if (busy || editing === null || opening !== null || !text.trim()) return;
    setBusy(true); setError(''); setNotice('');
    try { await axios.put(`${API_BASE}/resumes/${editing}/text`, { text }, { headers }); setEditing(null); setNotice('Extracted text saved. Your uploaded original is unchanged.'); await load(); }
    catch (e) { setError(apiError(e, 'Unable to save the resume text.')); } finally { setBusy(false); }
  };
  const primary = async id => {
    if (busy || opening !== null || editing !== null) return;
    setBusy(true); setError(''); setNotice('');
    try { await axios.put(`${API_BASE}/resumes/${id}/primary`, {}, { headers }); setNotice('Primary resume updated for future matches.'); await load(); }
    catch (e) { setError(apiError(e, 'Unable to select this resume.')); } finally { setBusy(false); }
  };
  const original = async resume => { setError(''); try { const result = await axios.get(`${API_BASE}/resumes/${resume.id}/original`, { headers, responseType: 'blob' }); downloadBlob(result.data, resume.filename); } catch (e) { setError(e.response?.status === 404 ? 'Original file unavailable for this historical upload. Upload the original again to retain a downloadable copy.' : 'Unable to download the original. Please try again.'); } };
  return <div className="wp-page">
    <PageHeader eyebrow="Your experience, preserved" title="Resumes" description="A strong next step starts with your own story. Keep your original intact, and choose the experience you want to build on." />
    <Notice error>{error}</Notice><Notice>{notice}</Notice>{opening !== null && <Notice>Opening resume text… You can select another resume while this loads.</Notice>}
    <div className="wp-two-column"><Panel eyebrow="Begin with the original" title="Your source of truth"><form onSubmit={upload} className="wp-form"><label className="wp-upload-label" htmlFor="resume-file"><span className="wp-upload-symbol" aria-hidden="true">↑</span><strong>Choose your resume</strong><span>PDF, DOCX or UTF-8 TXT · Up to 10 MB</span></label><input ref={input} id="resume-file" type="file" accept=".pdf,.docx,.txt" onChange={e => { setFile(e.target.files[0] || null); setError(''); }} /><button className="wp-button-primary" disabled={!file || busy || opening !== null || editing !== null}>{uploading ? 'Uploading…' : 'Upload Resume'}</button></form></Panel><aside className="wp-guidance"><p className="wp-eyebrow">Room to evolve</p><h2>One original.<br /><em>Thoughtful versions.</em></h2><p>Uploading a replacement retains your previous originals and selects the new resume for matching.</p><p>Editing extracted text changes future matches. It never replaces the file you uploaded.</p></aside></div>
    <Panel eyebrow="Your experience library" title="Your Resumes">{loading ? <p role="status" className="wp-loading">Loading your documents…</p> : resumes.length === 0 ? <EmptyState title="Start with your story.">Upload your current resume to begin matching and draft preparation.</EmptyState> : <ul className="wp-document-list">{resumes.map(resume => <li key={resume.id}><span className="wp-document-symbol" aria-hidden="true">R</span><div className="wp-document-info"><h3>{resume.filename}</h3><p>{resume.active ? 'Selected for future matches' : 'Original retained in your library'}</p></div>{resume.active && <span className="wp-badge">Primary resume</span>}<div className="wp-actions"><button disabled={busy || editing !== null} onClick={() => open(resume.id)}>Preview / edit text</button><button onClick={() => original(resume)}>Download original</button>{!resume.active && <button disabled={busy || opening !== null || editing !== null} onClick={() => primary(resume.id)}>Use for matching</button>}</div></li>)}</ul>}</Panel>
    {editing && <Panel title="Edit resume text" eyebrow="Review before saving"><p className="wp-muted">Check accuracy before saving. Changes affect future matches; the uploaded original remains unchanged. Save or cancel before opening another resume, uploading or changing the primary selection.</p><textarea disabled={busy} ref={editor} className="wp-resume-editor" aria-label="Resume text" rows={18} maxLength={100000} value={text} onChange={e => setText(e.target.value)} /><div className="wp-actions"><button className="wp-button-primary" onClick={save} disabled={busy || !text.trim()}>{busy ? 'Saving…' : 'Save changes'}</button><button disabled={busy} onClick={() => setEditing(null)}>Cancel</button></div></Panel>}
  </div>;
}
