import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

function App() {
  const [currentPage, setCurrentPage] = useState('login');
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(localStorage.getItem('user'));

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setCurrentPage('login');
  };

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
          <button onClick={logout} className="logout">Logout</button>
        </div>
      </nav>

      <main className="container">
        {currentPage === 'jobs' && <JobsPage token={token} />}
        {currentPage === 'resumes' && <ResumesPage token={token} />}
        {currentPage === 'applications' && <ApplicationsPage token={token} />}
        {currentPage === 'matches' && <MatchesPage token={token} />}
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
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', response.data.username);
        setToken(response.data.token);
        setUser(response.data.username);
      }
    } catch (error) {
      alert('Auth failed: ' + (error.response?.data?.error || error.message));
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

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE}/jobs`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setJobs(response.data);
    } catch (error) {
      console.error('Failed to fetch jobs:', error);
    } finally {
      setLoading(false);
    }
  };

  const triggerIngest = async () => {
    try {
      await axios.post(`${API_BASE}/jobs/ingest`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      alert('Ingestion triggered!');
      fetchJobs();
    } catch (error) {
      alert('Ingestion failed: ' + error.message);
    }
  };

  const triggerApply = async () => {
    try {
      await axios.post(`${API_BASE}/jobs/autoapply`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      alert('Auto-apply triggered!');
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
        <button onClick={triggerIngest} className="btn-primary">Ingest Jobs</button>
        <button onClick={triggerApply} className="btn-success">Auto-Apply</button>
        <button onClick={fetchJobs} className="btn-secondary">Refresh</button>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : (
        <table className="jobs-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Company</th>
              <th>Location</th>
              <th>Applied</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>{job.title}</td>
                <td>{job.company}</td>
                <td>{job.location}</td>
                <td>{job.applied ? '✓' : '✗'}</td>
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

  const fetchResumes = async () => {
    try {
      const response = await axios.get(`${API_BASE}/resumes`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setResumes(response.data);
    } catch (error) {
      console.error('Failed to fetch resumes:', error);
    }
  };

  const uploadResume = async (e) => {
    e.preventDefault();
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      await axios.post(`${API_BASE}/resumes/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'multipart/form-data'
        }
      });
      alert('Resume uploaded!');
      setFile(null);
      fetchResumes();
    } catch (error) {
      alert('Upload failed: ' + error.message);
    }
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
        <button type="submit" className="btn-primary">Upload Resume</button>
      </form>

      <h3>Your Resumes</h3>
      <ul className="resume-list">
        {resumes.map((resume) => (
          <li key={resume.id}>
            {resume.filename} {resume.active && <span className="active-badge">Active</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}

function MatchesPage({ token }) {
  const [matches,setMatches] = useState([]);
  const [draft,setDraft] = useState(null);
  const [error,setError] = useState('');
  const [loading,setLoading] = useState(false);
  const load = async () => {
    setLoading(true); setError('');
    try { const result = await axios.get(`${API_BASE}/matches`,{headers:{Authorization:`Bearer ${token}`}}); setMatches(result.data); }
    catch (e) {setError(e.response?.data?.message || 'Upload a resume to begin matching');}
    finally {setLoading(false);}
  };
  const tailor = async (id) => {
    setLoading(true);setError('');
    try { const result=await axios.post(`${API_BASE}/tailoring/${id}`,{}, {headers:{Authorization:`Bearer ${token}`}});setDraft(result.data);}
    catch (e){setError(e.response?.data?.message || 'AI draft failed; check server configuration');}
    finally {setLoading(false);}
  };
  useEffect(()=>{load()},[]);
  return <div className="page"><h2>Resume matched opportunities</h2>
    <p>All matches require your review. No applications are submitted automatically.</p>
    <button onClick={load} disabled={loading}>Refresh matches</button>
    {error && <p role="alert">{error}</p>}
    {matches.map(m=><div key={m.jobId} className="auth-card" style={{margin:'12px 0'}}>
      <h3>{m.title} — {m.company}</h3><p>{m.score}% textual overlap · {m.source}</p>
      <button onClick={()=>tailor(m.jobId)} disabled={loading}>Generate truthful tailored draft</button>
      {' '}
      <a href={m.applyUrl} target="_blank" rel="noopener noreferrer">Review external application</a>
    </div>)}
    {draft && <section><h2>Review draft for job #{draft.jobId}</h2>
      <p>This draft has not been applied or submitted. Verify all claims before using it.</p>
      <textarea readOnly value={draft.draft} rows={20} style={{width:'100%'}} />
      <button onClick={()=>navigator.clipboard.writeText(draft.draft)}>Copy draft</button>
    </section>}
  </div>
}
function ApplicationsPage({ token }) {
  const [applications, setApplications] = useState([]);

  const fetchApplications = async () => {
    try {
      const response = await axios.get(`${API_BASE}/applications`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setApplications(response.data);
    } catch (error) {
      console.error('Failed to fetch applications:', error);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  return (
    <div className="page">
      <h2>Application History</h2>
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
              <td className={`status-${app.status.toLowerCase()}`}>{app.status}</td>
              <td>{new Date(app.appliedAt).toLocaleDateString()}</td>
              <td>{app.notes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default App;

