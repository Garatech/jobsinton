import { useEffect, useState } from 'react';
import { fetchSystemStatus } from '../api/status';
import { PageHeading } from '../ui';

type ConnectionState = 'checking' | 'connected' | 'unavailable';
const connectionCopy: Record<ConnectionState, string> = {
  checking: 'Checking the connection…',
  connected: 'Connected to the JobSinton API',
  unavailable: 'The API is unavailable. Start the backend and try again.',
};
/** Connectivity remains independent of the catalog and browser-local preferences. */
export function Status() {
  const [connection, setConnection] = useState<ConnectionState>('checking');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    // Bound the wait even when the development proxy or backend stops responding.
    const timeout = window.setTimeout(() => controller.abort(), 5000);

    void fetchSystemStatus(controller.signal)
      .then(() => { if (active) setConnection('connected'); })
      .catch(() => { if (active) setConnection('unavailable'); })
      .finally(() => window.clearTimeout(timeout));

    return () => {
      // Prevent a previous request from overwriting a newer attempt or an unmounted view.
      active = false;
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [attempt]);

  function retry() {
    setConnection('checking');
    setAttempt((current) => current + 1);
  }

  return <>
    <PageHeading eyebrow="SERVICE STATUS" title="A foundation that connects." />
    <div className="status-page"><aside className="connection-card" aria-label="Service connection">
      <p className="eyebrow">THE JOBSINTON API</p><div className="signal-art" aria-hidden="true"><span /><span /><span /><span /><span /><span /><span /></div>
      <h2>A live connection.</h2><p className="card-description">The interface checks the Java service in real time.</p>
      <p className={'connection-status ' + connection} role="status" aria-label="API connection" aria-live="polite" aria-atomic="true"><span className="status-dot" aria-hidden="true" />{connectionCopy[connection]}</p>
      <button type="button" onClick={retry} disabled={connection === 'checking'}>{connection === 'checking' ? 'Checking…' : 'Check connection again'}</button>
      <p className="card-note">The sample catalog and your browser-local preferences remain available during an outage.</p>
    </aside></div>
  </>;
}
