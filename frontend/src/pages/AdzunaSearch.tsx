import { useEffect, useRef, useState, type FormEvent } from 'react';
import { searchAdzuna, type Listing, type SearchPage } from '../api/adzuna';

function payLabel(salary: Listing['salary']): string {
  if (!salary || salary.origin !== 'ADVERTISED') return 'Not indicated';
  const { minimum, maximum } = salary;
  const format = (n: number) => n.toLocaleString('en-GB');
  if (minimum !== null && maximum !== null) return format(minimum) + '–' + format(maximum);
  if (minimum !== null) return 'From ' + format(minimum);
  if (maximum !== null) return 'Up to ' + format(maximum);
  return 'Not indicated';
}

function LiveListing({ job }: { job: Listing }) {
  const pay = payLabel(job.salary);
  return <article className="job live-job"><div>
    <p className="company-name">{job.company || 'Company not indicated'}</p>
    <h3><a href={job.referral} target="_blank" rel="noopener noreferrer">{job.title}</a></h3>
    <p className="job-meta">{job.locationLabel || 'Location not indicated'} · Work mode: not indicated</p>
    {job.excerpt && <details><summary>Read excerpt</summary><p>{job.excerpt}</p></details>}
  </div><div className="live-job-aside">
    <p className="job-meta">Salary: {pay}{pay !== 'Not indicated' && ' (currency and period unconfirmed)'}</p>
    <a className="provider-credit" href="https://www.adzuna.es/" target="_blank" rel="noopener noreferrer">Adzuna</a>
  </div></article>;
}

type State = { kind: 'idle' | 'loading' } | { kind: 'error'; message: string }
  | { kind: 'done'; data: SearchPage; what: string; where: string };

export function AdzunaSearch() {
  const [what, setWhat] = useState('Java'), [where, setWhere] = useState('Madrid');
  const [state, setState] = useState<State>({ kind: 'idle' });
  const active = useRef<AbortController | null>(null);
  useEffect(() => () => { active.current?.abort(); active.current = null; }, []);
  async function submit(keyword: string, location: string, page: number) {
    if (active.current || !keyword || !location) return;
    const controller = new AbortController();
    active.current = controller;
    setState({ kind: 'loading' });
    const timer = window.setTimeout(() => controller.abort(), 15000);
    try {
      const data = await searchAdzuna(keyword, location, page, controller.signal);
      if (active.current === controller) setState({ kind: 'done', data, what: keyword, where: location });
    } catch (e) {
      if (active.current === controller) setState({ kind: 'error', message: controller.signal.aborted
        ? 'The local search timed out.' : e instanceof TypeError ? 'Could not reach the local backend.'
        : e instanceof Error ? e.message : 'Adzuna search is unavailable.' });
    } finally { window.clearTimeout(timer); if (active.current === controller) active.current = null; }
  }
  function firstPage(event: FormEvent) {
    event.preventDefault();
    void submit(what.trim(), where.trim(), 1);
  }
  return <section className="live-search" aria-label="Adzuna Spain search">
    <form className="live-form" onSubmit={firstPage} role="search" aria-label="Adzuna search">
      <label>Role or keyword<input required maxLength={100} value={what} onChange={e => setWhat(e.target.value)} /></label>
      <label>Location in Spain<input required maxLength={100} value={where} onChange={e => setWhere(e.target.value)} /></label>
      <button className="primary" disabled={state.kind === 'loading' || !what.trim() || !where.trim()}>Search Adzuna</button>
    </form>
    {state.kind === 'loading' && <p role="status">Searching Adzuna…</p>}
    {state.kind === 'error' && <p role="alert" className="notice notice-error">{state.message}</p>}
    {state.kind === 'done' && <>
      <p role="status">{state.data.listings.length ? state.data.listings.length + ' opportunities' :
        state.data.received ? 'No usable opportunities on this page' : 'No opportunities found'} · {state.what} / {state.where} · Page {state.data.page}</p>
      <div className="jobs">{state.data.listings.map(job => <LiveListing key={job.id} job={job} />)}</div>
      {state.what === what.trim() && state.where === where.trim() && <div className="actions live-pages">
        {state.data.page > 1 && <button className="secondary-button" onClick={() => void submit(state.what, state.where, state.data.page - 1)}>Previous page</button>}
        {state.data.page < 5 && state.data.received === 10 && <button className="secondary-button" onClick={() => void submit(state.what, state.where, state.data.page + 1)}>Next page</button>}
      </div>}
    </>}
  </section>;
}
