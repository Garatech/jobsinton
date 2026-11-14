import { number, translate, type Language } from '../i18n/locale';
import { useLanguage } from '../i18n/Language';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { searchAdzuna, type Listing, type SearchPage } from '../api/adzuna';

function payLabel(language: Language, salary: Listing['salary']): string {
  if (!salary || salary.origin !== 'ADVERTISED') return translate(language, 'Not indicated');
  const { minimum, maximum } = salary;
  const format = (n: number) => number(language, n);
  if (minimum !== null && maximum !== null) return format(minimum) + '–' + format(maximum);
  if (minimum !== null) return translate(language, 'From') + ' ' + format(minimum);
  if (maximum !== null) return translate(language, 'Up to') + ' ' + format(maximum);
  return translate(language, 'Not indicated');
}

function LiveListing({ job }: { job: Listing }) {
  const { t, language } = useLanguage();
  const pay = payLabel(language, job.salary);
  return <article className="job live-job"><div>
    <p className="company-name">{job.company || t('Company not indicated')}</p>
    <h3><a href={job.referral} target="_blank" rel="noopener noreferrer">{job.title}</a></h3>
    <p className="job-meta">{job.locationLabel || t('Location not indicated')} · {t('Work mode: not indicated')}</p>
    {job.excerpt && <details><summary>{t("Read excerpt")}</summary><p>{job.excerpt}</p></details>}
  </div><div className="live-job-aside">
    <p className="job-meta">{t("Salary:")} {pay}{pay !== t('Not indicated') && ' (' + t('currency and period unconfirmed') + ')'}</p>
    <a className="provider-credit" href="https://www.adzuna.es/" target="_blank" rel="noopener noreferrer">Adzuna</a>
  </div></article>;
}

type State = { kind: 'idle' | 'loading' } | { kind: 'error'; message: string }
  | { kind: 'done'; data: SearchPage; what: string; where: string };

export function AdzunaSearch() {
  const { t } = useLanguage();
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
  return <section className="live-search" aria-label={t("Adzuna Spain search")}>
    <form className="live-form" onSubmit={firstPage} role="search" aria-label={t("Adzuna search")}>
      <label>{t("Role or keyword")}<input required maxLength={100} value={what} onChange={e => setWhat(e.target.value)} /></label>
      <label>{t("Location in Spain")}<input required maxLength={100} value={where} onChange={e => setWhere(e.target.value)} /></label>
      <button className="primary" disabled={state.kind === 'loading' || !what.trim() || !where.trim()}>{t("Search Adzuna")}</button>
    </form>
    {state.kind === 'loading' && <p role="status">{t("Searching Adzuna…")}</p>}
    {state.kind === 'error' && <p role="alert" className="notice notice-error">{t(state.message)}</p>}
    {state.kind === 'done' && <>
      <p role="status">{state.data.listings.length ? state.data.listings.length + ' ' + t(state.data.listings.length === 1 ? 'opportunity' : 'opportunities') :
        t(state.data.received ? 'No usable opportunities on this page' : 'No opportunities found')} · {state.what} / {state.where} · {t('Page')} {state.data.page}</p>
      <div className="jobs">{state.data.listings.map(job => <LiveListing key={job.id} job={job} />)}</div>
      {state.what === what.trim() && state.where === where.trim() && <div className="actions live-pages">
        {state.data.page > 1 && <button className="secondary-button" onClick={() => void submit(state.what, state.where, state.data.page - 1)}>{t("Previous page")}</button>}
        {state.data.page < 5 && state.data.received === 10 && <button className="secondary-button" onClick={() => void submit(state.what, state.where, state.data.page + 1)}>{t("Next page")}</button>}
      </div>}
    </>}
  </section>;
}
