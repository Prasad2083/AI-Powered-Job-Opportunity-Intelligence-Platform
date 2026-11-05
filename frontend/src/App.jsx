import React, { useState, useEffect } from 'react';
import axios from 'axios';

import HomePage from './components/HomePage';
import AuthPage from './components/AuthPage';
import Workspace from './components/Workspace';
import JobsPage from './pages/JobsPage';
import ResumesPage from './pages/ResumesPage';
import MatchesPage from './pages/MatchesPage';
import ApplicationsPage from './pages/ApplicationsPage';
import SettingsPage from './pages/SettingsPage';
import { API_BASE } from './lib/api';

function publicViewFromHash() {
  return window.location.hash === '#login' ? 'login' : window.location.hash === '#signup' ? 'signup' : 'home';
}

function App() {
  const [currentPage, setCurrentPage] = useState('jobs');
  const [publicView, setPublicView] = useState(publicViewFromHash);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(localStorage.getItem('user'));
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    const onHashChange = () => setPublicView(publicViewFromHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  useEffect(() => {
    if (!token) document.getElementById('public-page-title')?.focus({ preventScroll: true });
  }, [publicView, token]);

  const clearSession = (destination = 'login') => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setCurrentPage('jobs');
    setPublicView(destination);
    window.location.hash = destination === 'home' ? '' : 'login';
  };

  const logout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {await axios.post(`${API_BASE}/auth/logout`,{}, {headers:{Authorization:`Bearer ${token}`}});}
    catch(e) {window.alert("Local session cleared. Server logout could not be confirmed; existing tokens expire within the configured lifetime.");}
    finally {setLoggingOut(false);clearSession('home');}
  };

  useEffect(() => {
    const interceptor=axios.interceptors.response.use(response=>response,error=>{
      if (error.response?.status === 401 && token) clearSession();
      return Promise.reject(error);
    });
    return () => axios.interceptors.response.eject(interceptor);
  },[token]);

  if (!token) {
    return publicView === 'home' ? <HomePage /> : <AuthPage mode={publicView} setToken={setToken} setUser={setUser} />;
  }

  return <Workspace page={currentPage} onNavigate={setCurrentPage} user={user} onLogout={logout} loggingOut={loggingOut}>
    {currentPage === 'jobs' && <JobsPage token={token} />}
    {currentPage === 'resumes' && <ResumesPage token={token} />}
    {currentPage === 'matches' && <MatchesPage token={token} />}
    {currentPage === 'applications' && <ApplicationsPage token={token} />}
    {currentPage === 'settings' && <SettingsPage token={token} />}
  </Workspace>;
}

export default App;
