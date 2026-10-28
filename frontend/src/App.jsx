import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';
function apiError(error,fallback='Request failed') {
  const data=error.response?.data;
  if(data?.fieldErrors) {
    const fields=Object.entries(data.fieldErrors).map(([name,message])=>`${name}: ${message}`);
    if(fields.length) return fields.join('; ');
  }
  return data?.message||data?.error||fallback;
}

function App() {
  const [currentPage, setCurrentPage] = useState('jobs');
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(localStorage.getItem('user'));

  const clearSession = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setCurrentPage('login');
  };

  const logout = async () => {
    try {await axios.post(`${API_BASE}/auth/logout`,{}, {headers:{Authorization:`Bearer ${token}`}});}
    catch(e) {window.alert("Local session cleared. Server logout could not be confirmed; existing tokens expire within the configured lifetime.");}
    finally {clearSession();}
  };

  useEffect(() => {
    const interceptor=axios.interceptors.response.use(response=>response,error=>{
      if (error.response?.status === 401 && token) clearSession();
      return Promise.reject(error);
    });
    return () => axios.interceptors.response.eject(interceptor);
  },[token]);

  if (!token) {
    return <AuthPage setToken={setToken} setUser={setUser} />;
  }

  return (
    <div className="app">
      <nav className="navbar">
        <h1>JobPulse</h1>
        <div className="nav-links">
          <button onClick={() => setCurrentPage('jobs')} className={currentPage === 'jobs' ? 'active' : ''}>
            Jobs
          </button>
          <button onClick={() => setCurrentPage('resumes')} className={currentPage === 'resumes' ? 'active' : ''}>
            Resumes
          </button>
          <button onClick={() => setCurrentPage('matches')}>Matches</button>
          <button onClick={() => setCurrentPage('applications')} className={currentPage === 'applications' ? 'active' : ''}>
            Applications
          </button>
          <button onClick={() => setCurrentPage('settings')}>Profile & preferences</button>
          <button onClick={logout} className="logout">Logout</button>
        </div>
      </nav>

      <main className="container">
        {currentPage === 'jobs' && <JobsPage token={token} />}
        {currentPage === 'resumes' && <ResumesPage token={token} />}
        {currentPage === 'applications' && <ApplicationsPage token={token} />}
        {currentPage === 'matches' && <MatchesPage token={token} />}
        {currentPage === 'settings' && <SettingsPage token={token} />}
      </main>
    </div>
  );
}

function AuthPage({ setToken, setUser }) {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    email: '',
    fullName: ''
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const endpoint = isLogin ? '/auth/login' : '/auth/register';
      const response = await axios.post(`${API_BASE}${endpoint}`, formData);
      if (!isLogin && !response.data.token) { setIsLogin(true); alert('Account created. Please log in.'); return; }
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', response.data.username);
        setToken(response.data.token);
        setUser(response.data.username);
      }
    } catch (error) {
      alert('Auth failed: ' + apiError(error,error.message));
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>{isLogin ? 'Login' : 'Register'}</h2>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Username"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
            required
          />
          <input
            type="password"
            placeholder="Password"
            minLength={isLogin?undefined:12}
            maxLength={72}
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            required
          />
          {!isLogin && (
            <>
              <input
                type="email"
                placeholder="Email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
              <input
                type="text"
                placeholder="Full Name"
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
              />
            </>
          )}
          <button type="submit">{isLogin ? 'Login' : 'Register'}</button>
        </form>
        <p>
          {isLogin ? "Don't have an account? " : 'Already have an account? '}
          <button
            type="button"
            onClick={() => setIsLogin(!isLogin)}
            className="toggle-auth"
          >
            {isLogin ? 'Register' : 'Login'}
          </button>
        </p>
      </div>
    </div>
  );
}

function JobsPage({ token }) {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filter,setFilter] = useState('');
  const [showManual,setShowManual]=useState(false);
  const [ingestion,setIngestion]=useState(null);
  const [manual,setManual]=useState({title:'',company:'',location:'',applyUrl:'',description:''});
  const [error,setError] = useState('');

  const fetchJobs = async () => {
    setLoading(true); setError('');
    try {
      const response = await axios.get(`${API_BASE}/jobs`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setJobs(response.data);
    } catch (error) {
      setError(error.response?.data?.error || 'Unable to load jobs. Log out and sign back in if your login expired.');
    } finally {
      setLoading(false);
    }
  };

  const triggerIngest = async () => {
    setLoading(true);setError('');
    try {
      const result=await axios.post(`${API_BASE}/jobs/ingest`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setIngestion(result.data);
      await fetchJobs();
    } catch (error) {
      setError('Job search failed: ' + (error.response?.data?.message || error.message));
    } finally {setLoading(false);}
  };

  const saveManual = async (e) => {
    e.preventDefault(); setError('');
    try {
      await axios.post(`${API_BASE}/jobs/manual`,manual,{headers:{Authorization:`Bearer ${token}`}});
      setManual({title:'',company:'',location:'',applyUrl:'',description:''});
      setShowManual(false);
      await fetchJobs();
    } catch(err) {
      setError('Unable to save pasted job: '+(err.response?.data?.error||err.response?.data?.message||err.message));
    }
  };
  const triggerApply = async () => {
    try {
      await axios.post(`${API_BASE}/jobs/autoapply`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      alert('Application submission is not available; review official job postings instead.');
      fetchJobs();
    } catch (error) {
      alert('Auto-apply failed: ' + error.message);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  return (
    <div className="page">
      <h2>Jobs</h2>
      <div className="actions">
        <button onClick={triggerIngest} disabled={loading} className="btn-primary">{loading?'Searching…':'Find jobs'}</button>

        <button onClick={fetchJobs} className="btn-secondary">Refresh</button>
        <button onClick={()=>setShowManual(!showManual)} type="button">{showManual ? "Cancel pasted job" : "Paste a job description"}</button>
      </div>
      {showManual && <form onSubmit={saveManual} className="auth-card" style={{display:'grid',gap:10,maxWidth:680,marginBottom:18}}>
        <h3>Add a job for resume matching</h3>
        <p>Manually entered jobs are not independently verified.</p>
        <input required maxLength={255} placeholder="Job title" value={manual.title} onChange={e=>setManual({...manual,title:e.target.value})} />
        <input required maxLength={255} placeholder="Employer" value={manual.company} onChange={e=>setManual({...manual,company:e.target.value})} />
        <input placeholder="Location" value={manual.location} onChange={e=>setManual({...manual,location:e.target.value})} />
        <input type="url" placeholder="Official application URL (optional, HTTPS)" value={manual.applyUrl} onChange={e=>setManual({...manual,applyUrl:e.target.value})} />
        <textarea required minLength={40} rows={10} placeholder="Paste the full job description" value={manual.description} onChange={e=>setManual({...manual,description:e.target.value})} />
        <button type="submit">Save job for matching</button>
      </form>}
      {ingestion && <div role="status"><p>Checked {ingestion.checkedSources?.length||0} feeds · retrieved {ingestion.fetched} · saved {ingestion.saved} · archived {ingestion.archived} · storage errors {ingestion.failed}.</p>{Object.entries(ingestion.sourceFailures||{}).map(([source,reason])=><p key={source}>{source}: {reason}</p>)}</div>}
      <p>Find jobs from configured public employer feeds. An empty list means no jobs were returned or configured.</p>
      <input aria-label="Filter jobs" placeholder="Filter title, company or location" value={filter} onChange={e=>setFilter(e.target.value)} style={{width:'100%',maxWidth:420,padding:10,marginBottom:12}} />
      {error && <p role="alert">{error}</p>}

      {loading ? (
        <p>Loading...</p>
      ) : (
        <table className="jobs-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Company</th>
              <th>Location</th>
              <th>Source / application</th>
            </tr>
          </thead>
          <tbody>
            {jobs.filter(job=>[job.title,job.companyName,job.location].some(value=>String(value||'').toLowerCase().includes(filter.toLowerCase()))).map((job) => (
              <tr key={job.id}>
                <td>{job.title}</td>
                <td>{job.companyName}</td>
                <td>{job.location}</td>
                <td>{job.postingStatus === 'MANUAL_UNVERIFIED' ? 'Manually entered — unverified' : job.postingStatus === 'MOCK_NOT_A_VACANCY' ? 'Mock — not a vacancy' : job.source} {job.applyUrl && <a href={job.applyUrl} target="_blank" rel="noopener noreferrer">Official application</a>} {job.stale && <span> · Freshness unverified</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ResumesPage({ token }) {
  const [resumes, setResumes] = useState([]);
  const [file, setFile] = useState(null);
  const [error,setError] = useState('');
  const [editing,setEditing] = useState(null);
  const [resumeText,setResumeText] = useState('');
  const [uploading,setUploading] = useState(false);

  const fetchResumes = async () => {
    try {
      const response = await axios.get(`${API_BASE}/resumes`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setResumes(response.data);
    } catch (error) {
      setError(error.response?.data?.error || 'Unable to load resumes. Please sign in again if your session has expired.');
    }
  };

  const uploadResume = async (e) => {
    e.preventDefault();
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    setError(''); setUploading(true);
    try {
      await axios.post(`${API_BASE}/resumes/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,

        }
      });
      alert('Resume uploaded!');
      setFile(null);
      fetchResumes();
    } catch (error) {
      setError('Upload failed: ' + (error.response?.data?.error || error.response?.data?.message || error.message));
    } finally { setUploading(false); }
  };

  const selectPrimary = async (id) => {
    try { await axios.put(`${API_BASE}/resumes/${id}/primary`,{}, {headers:{Authorization:`Bearer ${token}`}}); await fetchResumes(); }
    catch(e) { setError('Unable to select resume'); }
  };
  const downloadOriginal = async (id, filename) => {
    try {
      const result=await axios.get(`${API_BASE}/resumes/${id}/original`,{headers:{Authorization:`Bearer ${token}`},responseType:'blob'});
      const url=URL.createObjectURL(result.data);const a=document.createElement('a');a.href=url;a.download=filename;a.click();URL.revokeObjectURL(url);
    } catch(e) {setError('Original file unavailable. Historical uploads retained only extracted text; upload the original again.');}
  };
  const editResume = async (id) => {
    setError('');
    try { const response=await axios.get(`${API_BASE}/resumes/${id}/text`,{headers:{Authorization:`Bearer ${token}`}});setEditing(id);setResumeText(response.data.text); }
    catch(error){setError('Unable to open resume: '+(error.response?.data?.message||error.message));}
  };
  const saveResume = async () => {
    setError('');
    try {await axios.put(`${API_BASE}/resumes/${editing}/text`,{text:resumeText},{headers:{Authorization:`Bearer ${token}`}});setEditing(null);await fetchResumes();}
    catch(error){setError('Unable to save resume: '+(error.response?.data?.message||error.message));}
  };

  useEffect(() => {
    fetchResumes();
  }, []);

  return (
    <div className="page">
      <h2>Resumes</h2>
      <form onSubmit={uploadResume} className="upload-form">
        <input
          type="file"
          onChange={(e) => setFile(e.target.files[0])}
          accept=".pdf,.docx,.txt"
        />
        <button type="submit" disabled={!file || uploading} className="btn-primary">{uploading ? "Uploading..." : "Upload Resume"}</button><p>Uploading a replacement retains previous originals and selects the new resume for matching.</p>
      </form>

      {error && <p role="alert">{error}</p>}
      <h3>Your Resumes</h3>
      <ul className="resume-list">
        {resumes.map((resume) => (
          <li key={resume.id}>
            {resume.filename} {resume.active && <span className="active-badge">Active</span>} <button type="button" onClick={()=>editResume(resume.id)}>Preview / edit text</button> <button type="button" onClick={()=>downloadOriginal(resume.id,resume.filename)}>Download original</button> {!resume.active && <button type="button" onClick={()=>selectPrimary(resume.id)}>Use for matching</button>}
          </li>
        ))}
      </ul>
      {editing && <section><h3>Edit resume text</h3><p>Changes to this master resume affect future matches. Verify accuracy before saving.</p><textarea aria-label="Resume text" value={resumeText} onChange={e=>setResumeText(e.target.value)} rows={18} style={{width:"100%"}} /><button type="button" onClick={saveResume}>Save changes</button> <button type="button" onClick={()=>setEditing(null)}>Cancel</button></section>}
    </div>
  );
}

function MatchesPage({ token }) {
  const [matches,setMatches] = useState([]);
  const [draft,setDraft] = useState(null);
  const [editedDraft,setEditedDraft] = useState('');
  const [saved,setSaved]=useState([]);
  const [approved,setApproved]=useState(false);
  const [notice,setNotice]=useState('');
  const [error,setError] = useState('');
  const [loading,setLoading] = useState(false);
  const load = async () => {
    setLoading(true); setError('');
    try { const result = await axios.get(`${API_BASE}/matches`,{headers:{Authorization:`Bearer ${token}`}}); setMatches(result.data); }
    catch (e) {setError(e.response?.data?.message || 'Upload a resume to begin matching');}
    finally {setLoading(false);}
  };
  const loadSaved = async () => {
    try {const result=await axios.get(`${API_BASE}/tailoring/documents`,{headers:{Authorization:`Bearer ${token}`}});setSaved(result.data);}
    catch(e) {setError('Unable to load saved documents');}
  };
  const saveVersion = async () => {
    try {await axios.post(`${API_BASE}/tailoring/documents`,{resumeId:draft.resumeId,jobId:draft.jobId,content:editedDraft,approved},{headers:{Authorization:`Bearer ${token}`}});setNotice('Saved a new version. Your original resume is unchanged.');await loadSaved();}
    catch(e) {setError('Unable to save document');}
  };
  const tailor = async (id) => {
    setLoading(true);setError('');
    try { const result=await axios.post(`${API_BASE}/tailoring/${id}`,{}, {headers:{Authorization:`Bearer ${token}`}});setDraft(result.data);setEditedDraft(result.data.draft);setApproved(false);setNotice('');}
    catch (e){setError(e.response?.data?.message || 'AI draft failed; check server configuration');}
    finally {setLoading(false);}
  };
  useEffect(()=>{load();loadSaved()},[]);
  return <div className="page"><h2>Resume matched opportunities</h2>
    <p>All matches require your review. No applications are submitted automatically.</p>
    <button onClick={load} disabled={loading}>Refresh matches</button>
    {error && <p role="alert">{error}</p>}
    {notice && <p role="status">{notice}</p>}
    {matches.map(m=><div key={m.jobId} className="auth-card" style={{margin:'12px 0'}}>
      <h3>{m.title} — {m.company}</h3><p>{m.score}% ranking score · {m.source}</p>
      <details><summary>Why this match?</summary><p>{m.explanation}</p><p>Skills evidenced: {(m.matchedSkills||[]).join(', ')||'None detected'}</p><p>Skills to review: {(m.skillsNotEvidenced||[]).join(', ')||'None detected'}</p><p>Location preference: {m.locationPreferenceMatched?'Matched':'Review required'}</p>{m.seniorityReviewRequired && <p>Seniority evidence needs review.</p>}<p>Explicit years mentioned: job {m.requiredYearsMentioned??'unspecified'} · resume {m.resumeYearsMentioned??'unspecified'} (not verified totals)</p>{m.experienceReviewRequired && <p>Experience requirements need review.</p>}</details>
      <button onClick={()=>tailor(m.jobId)} disabled={loading}>Generate truthful tailored draft</button>
      {' '}
      <button onClick={async()=>{
        if(!window.confirm('Have you completed the application on the official employer website? This records your report, not provider confirmation.')) return;
        try {await axios.post(`${API_BASE}/applications/manual`,{jobId:m.jobId,completed:true,notes:'Reported completed by user; no provider receipt verified.'},{headers:{Authorization:`Bearer ${token}`}});setNotice('Recorded as user-reported submission. See Application History.');}
        catch(e) {setError(e.response?.data?.message||'Unable to record application');}
      }}>Record completed manual application</button>
      <a href={m.applyUrl} target="_blank" rel="noopener noreferrer">Review external application</a>
    </div>)}
    {draft && <section><h2>Review draft for job #{draft.jobId}</h2>
<p>Local AI selects verbatim source excerpts; the complete original is preserved below them. Edit wording only where supported by your actual experience. Review every claim before approval.</p>
      <textarea aria-label="Edit tailored resume" value={editedDraft} onChange={e=>{setEditedDraft(e.target.value);setApproved(false);}} rows={20} style={{width:'100%'}} />
      <label><input type="checkbox" checked={approved} onChange={e=>setApproved(e.target.checked)} /> I reviewed every claim against my original resume and approve this document</label>
      <div className="actions"><button onClick={saveVersion} disabled={!editedDraft.trim()}>Save new version</button><button onClick={()=>navigator.clipboard.writeText(editedDraft)}>Copy edited draft</button>
      <button onClick={()=>{const blob=new Blob([editedDraft],{type:'text/plain;charset=utf-8'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`jobpulse-tailored-${draft.jobId}.txt`;a.click();URL.revokeObjectURL(url);}}>Download edited draft</button></div>
      <p>Save new version retains a separate copy. Approval applies only to this document; no application is submitted.</p>
    </section>}
    <h3>Saved tailored versions</h3>
    {saved.map(document=><div key={document.id}><button onClick={()=>{setDraft({resumeId:document.resumeId,jobId:document.jobId});setEditedDraft(document.content);setApproved(false);setNotice('');}}>Open version #{document.id} · job #{document.jobId} · {document.approved?'Reviewed':'Draft'}</button></div>)}
  </div>
}
function SettingsPage({token}) {
  const [profile,setProfile]=useState(null);const [message,setMessage]=useState('');
  useEffect(()=>{axios.get(`${API_BASE}/profile`,{headers:{Authorization:`Bearer ${token}`}}).then(r=>setProfile(r.data)).catch(()=>setMessage('Unable to load profile'));},[]);
  const save=async e=>{e.preventDefault();try{await axios.put(`${API_BASE}/profile`,profile,{headers:{Authorization:`Bearer ${token}`}});setMessage('Preferences saved');}catch(e){setMessage(e.response?.data?.message||'Unable to save preferences');}};
  return <section className="page"><h2>Profile & job preferences</h2>{message && <p role="status">{message}</p>}{profile && <form onSubmit={save} className="auth-card"><p>{profile.username} · {profile.email}</p><label>Full name<input value={profile.fullName} maxLength={255} onChange={e=>setProfile({...profile,fullName:e.target.value})}/></label><label>Target roles (comma separated)<textarea rows={4} value={profile.targetRoles} maxLength={1000} onChange={e=>setProfile({...profile,targetRoles:e.target.value})}/></label><label>Preferred locations (comma separated)<textarea rows={3} value={profile.targetLocations} maxLength={255} onChange={e=>setProfile({...profile,targetLocations:e.target.value})}/></label><p>Preferences influence ranking. They do not enable automatic submissions.</p><button>Save preferences</button></form>}</section>;
}
function ApplicationsPage({ token }) {
  const [applications, setApplications] = useState([]);
  const [error,setError]=useState('');

  const fetchApplications = async () => {
    try {
      const response = await axios.get(`${API_BASE}/applications`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setApplications(response.data);
    } catch (error) {
      setError('Unable to load application history. Please retry.');
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  return (
    <div className="page">
      <h2>Application History</h2><p>Manual records are user-reported. JobPulse has not independently verified provider confirmation.</p>{error && <p role="alert">{error}</p>}
      <table className="apps-table">
        <thead>
          <tr>
            <th>Job Title</th>
            <th>Status</th>
            <th>Date Applied</th>
            <th>Notes</th>
          </tr>
        </thead>
        <tbody>
          {applications.map((app) => (
            <tr key={app.id}>
              <td>{app.job?.title}</td>
              <td><select aria-label={`Update status for ${app.job?.title}`} value={app.applicationStatus} onChange={async e=>{
                try {await axios.patch(`${API_BASE}/applications/${app.id}`,{status:e.target.value,notes:app.notes},{headers:{Authorization:`Bearer ${token}`}});await fetchApplications();}
                catch(error) {setError(apiError(error,'Unable to update status'));}
              }}>{['USER_REPORTED_SUBMITTED','USER_REPORTED_INTERVIEW','USER_REPORTED_OFFER','USER_REPORTED_REJECTED','USER_REPORTED_WITHDRAWN'].map(status=><option key={status} value={status}>{status}</option>)}</select></td>
              <td>{new Date(app.appliedDate).toLocaleDateString()}</td>
              <td>{app.notes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default App;

