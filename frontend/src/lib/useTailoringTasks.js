import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from './api';

const ACTIVE = new Set(['QUEUED', 'RUNNING', 'CANCEL_REQUESTED']);

export default function useTailoringTasks(token, onReady) {
  const [tasks, setTasks] = useState([]), [error, setError] = useState(''), [loading, setLoading] = useState(true), [revision, setRevision] = useState(0);
  const ready = useRef(onReady);
  ready.current = onReady;
  const refresh = () => setRevision(value => value + 1);

  useEffect(() => {
    const controller = new AbortController();
    let timer;
    const load = async () => {
      try {
        const response = await axios.get(`${API_BASE}/tailoring/tasks`, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal });
        if (controller.signal.aborted) return;
        setTasks(response.data); setError(''); setLoading(false);
        if (response.data.some(task => ACTIVE.has(task.status))) timer = setTimeout(load, 2000);
      } catch (e) {
        if (controller.signal.aborted) return;
        setLoading(false); setError(apiError(e, 'Unable to refresh draft tasks. Your queued work remains on the server.'));
        timer = setTimeout(load, 5000);
      }
    };
    load();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [token, revision]);

  const enqueue = async jobId => {
    const result = await axios.post(`${API_BASE}/tailoring/tasks/${jobId}`, {}, { headers: { Authorization: `Bearer ${token}` } });
    setTasks(current => [result.data, ...current]);
    refresh();
  };
  const cancel = async id => {
    await axios.post(`${API_BASE}/tailoring/tasks/${id}/cancel`, {}, { headers: { Authorization: `Bearer ${token}` } });
    refresh();
  };
  const open = async id => {
    const result = await axios.get(`${API_BASE}/tailoring/tasks/${id}`, { headers: { Authorization: `Bearer ${token}` } });
    if (result.data.status === 'SUCCEEDED') ready.current(result.data);
  };
  return { tasks, error, loading, hasActive: tasks.some(task => ACTIVE.has(task.status)), enqueue, cancel, open, refresh };
}
