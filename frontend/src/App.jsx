import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

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
        <h1>JobPulse Auto Apply</h1>
        <div className="nav-links">
          <button onClick={() => setCurrentPage('jobs')} className={currentPage === 'jobs' ? 'active' : ''}>
            Jobs
          </button>
          <button onClick={() => setCurrentPage('resumes')} className={currentPage === 'resumes' ? 'active' : ''}>
            Resumes
          </button>
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
      const endpoint = isLogin ? '/api/auth/login' : '/api/auth/register';
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

