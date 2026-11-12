import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { API_BASE, apiError } from './api';

const active = run => ['QUEUED', 'RUNNING'].includes(run?.status);
const valid = run => run && Number.isInteger(run.id) && ['QUEUED', 'RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED'].includes(run.status);

export default function useIngestionRun(token, onFinished) {
  const [run, setRun] = useState(null), [error, setError] = useState(''), [loading, setLoading] = useState(true), [requesting, setRequesting] = useState(false), [revision, setRevision] = useState(0);
  const watched = useRef(null), finished = useRef(onFinished);
  finished.current = onFinished;
  const refresh = () => setRevision(value => value + 1);
  useEffect(() => {
    const controller = new AbortController(); let timer;
    const load = async () => {
      try {
        const response = await axios.get(`${API_BASE}/jobs/ingestion`, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal });
        if (controller.signal.aborted) return;
        if (response.status === 204) { setRun(null); setLoading(false); setError(''); return; }
        if (!valid(response.data)) throw new Error('Restart the running backend after updating to use background discovery.');
        const next = response.data;
        setRun(next); setError(''); setLoading(false);
        if (active(next)) { watched.current = next.id; timer = setTimeout(load, 2000); }
        else if (watched.current === next.id) { watched.current = null; finished.current(); }
      } catch (e) {
        if (controller.signal.aborted) return;
        setLoading(false);
        setError(e.message?.includes('running backend') ? e.message : apiError(e, 'Unable to read discovery status. Previously saved jobs remain available.'));
        if (!e.response || e.response.status >= 500) timer = setTimeout(load, 5000);
      }
    };
    load();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [token, revision]);

  const request = async () => {
    setRequesting(true); setError('');
    try {
      const response = await axios.post(`${API_BASE}/jobs/ingest`, {}, { headers: { Authorization: `Bearer ${token}` } });
      if (!valid(response.data)) throw new Error('Restart the running backend after updating to use background discovery.');
      setRun(response.data);
      if (active(response.data)) watched.current = response.data.id;
      else finished.current();
      refresh();
    } catch (e) {
      setError(e.message?.includes('running backend') ? e.message : apiError(e, 'Unable to queue discovery. Please try again later.'));
    } finally { setRequesting(false); }
  };
  return { run, error, loading, requesting, isActive: active(run), request, refresh };
}
