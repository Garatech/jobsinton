import { useEffect, useState } from 'react';
import { fetchSystemStatus } from './api/status';
import { ProfileEditor } from './profile/ProfileEditor';

type ConnectionState = 'checking' | 'connected' | 'unavailable';

const connectionCopy: Record<ConnectionState, string> = {
  checking: 'Checking the connection…',
  connected: 'Connected to the JobSinton API',
  unavailable: 'The API is unavailable. Start the backend and try again.',
};

/** Product introduction, local profile editor, and independent API connectivity check. */
export function App() {
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

  return (
    <div className="page-shell">
      <a className="skip-link" href="#main">Skip to content</a>
      <header className="site-header">
        <a className="brand" href="/" aria-label="JobSinton home">
          <span className="brand-mark" aria-hidden="true">js</span>
          <span>JobSinton<span className="brand-dot">.</span></span>
        </a>
        <span className="release-label">Profile preview</span>
      </header>
      <main id="main">
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="eyebrow">YOUR NEXT CHAPTER, IN TUNE</p>
            <h1 id="hero-title">Good work starts<br />with a better <em>fit.</em></h1>
            <p className="intro">
              Your skills. Your priorities. Your next move.
              JobSinton is taking shape to help you find opportunities
              that make sense for you.
            </p>
            <a className="text-link" href="#profile">Set up your profile <span aria-hidden="true">↗</span></a>
          </div>
          <aside className="connection-card" aria-labelledby="connection-title">
            <p className="eyebrow">FIRST CONNECTION</p>
            <div className="signal-art" aria-hidden="true">
              <span /><span /><span /><span /><span /><span /><span />
            </div>
            <h2 id="connection-title">A foundation that connects.</h2>
            <p className="card-description">The interface checks the Java service in real time.</p>
            <p className={'connection-status ' + connection} role="status" aria-label="API connection" aria-live="polite" aria-atomic="true">
              <span className="status-dot" aria-hidden="true" />
              {connectionCopy[connection]}
            </p>
            <button type="button" onClick={retry} disabled={connection === 'checking'}>
              {connection === 'checking' ? 'Checking…' : 'Check connection again'}
            </button>
            <p className="card-note">This checks connectivity only. Job search is not available yet.</p>
          </aside>
        </section>
        <ProfileEditor />
        <section id="direction" className="direction" aria-labelledby="direction-title">
          <div className="section-heading">
            <h2 id="direction-title">Built around what matters to you.</h2>
            <span className="planned-label">Planned capabilities</span>
          </div>
          <div className="feature-grid">
            <article><span className="feature-number">01 / YOUR PRIORITIES</span><h3>A profile with purpose</h3><p>Use your saved preferences to shape future job searches.</p></article>
            <article><span className="feature-number">02 / CLEAR REASONS</span><h3>Understand the fit</h3><p>See why an opportunity matches, where it differs, and which details are still unknown.</p></article>
            <article><span className="feature-number">03 / ROOM TO EXPLORE</span><h3>A wider horizon</h3><p>Start with Spain, with future support for opportunities across Europe.</p></article>
          </div>
        </section>
      </main>
      <footer><span>A Gara’s Workshop project</span><span>Early development · Built step by step</span></footer>
    </div>
  );
}
