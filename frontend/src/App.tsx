import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { findCompany, findOpportunity, opportunities } from './catalog/data';
import { clearSaved, loadSaved, SAVED_STORAGE_KEY, writeSaved } from './catalog/saved';
import { useRoute } from './navigation';
import { usePageFade } from './usePageFade';
import { Companies, CompanyDetail } from './pages/Companies';
import { Home } from './pages/Home';
import { OpportunityDetail } from './pages/Opportunity';
import { Search } from './pages/Search';
import { Status } from './pages/Status';
import { ProfileEditor } from './profile/ProfileEditor';
import { loadProfile } from './profile/storage';
import { type ProfessionalProfile } from './profile/model';
import { EmptyState, JobList, PageHeading } from './ui';

const nav = [['home', 'Overview'], ['opportunities', 'Opportunities'], ['companies', 'Companies'], ['saved', 'Saved'], ['profile', 'Profile']];

/** Current is the shared application shell; routes retain ordinary browser history. */
export function App() {
  const route = useRoute();
  const main = useRef<HTMLElement>(null);
  const [initial] = useState(loadSaved);
  const [saved, setSaved] = useState(initial.ids);
  const [storageError, setStorageError] = useState(initial.error);
  const [message, setMessage] = useState('');
  const [profile, setProfile] = useState<ProfessionalProfile | null>(() => {
    const current = loadProfile();
    return current.kind === 'loaded' ? current.profile : null;
  });
  const previousRoute = useRef(route.hash);
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(''), 4500);
    return () => window.clearTimeout(timer);
  }, [message]);
  useEffect(() => {
    function refresh(event: StorageEvent) {
      if (event.key !== SAVED_STORAGE_KEY && event.key !== null) return;
      const next = loadSaved(); setSaved(next.ids); setStorageError(next.error);
    }
    window.addEventListener('storage', refresh);
    return () => window.removeEventListener('storage', refresh);
  }, []);
  const job = route.page === 'opportunities' && route.id ? findOpportunity(route.id) : undefined;
  const company = route.page === 'companies' && route.id ? findCompany(route.id) : undefined;
  const valid = (['home', 'saved', 'profile', 'status'].includes(route.page) && !route.id)
    || (route.page === 'opportunities' && (!route.id || Boolean(job)))
    || (route.page === 'companies' && (!route.id || Boolean(company)));
  const title = !valid ? 'Page not found' : job?.title || company?.name || ({ home: 'Overview', opportunities: 'Opportunities', companies: 'Companies', saved: 'Saved opportunities', profile: 'Your profile', status: 'Service status' } as Record<string, string>)[route.page];
  useLayoutEffect(() => {
    document.title = title + ' · JobSinton';
    if (previousRoute.current !== route.hash) {
      document.getElementById('main')?.focus({ preventScroll: true });
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      previousRoute.current = route.hash;
    }
  }, [route.hash, title]);
  usePageFade(main, valid ? route.page + '/' + route.id : 'not-found');
  function toggle(id: string) {
    const exists = saved.includes(id);
    const next = exists ? saved.filter(value => value !== id) : [...saved, id];
    if (!writeSaved(next)) {
      setMessage(''); setStorageError('The shortlist could not be saved. Allow browser storage and try again. Your previous saved list is unchanged.'); return;
    }
    setSaved(next); setStorageError('');
    setMessage(exists ? 'Removed from your shortlist.' : 'Saved to your shortlist.');
    if (route.page === 'saved' && exists) window.requestAnimationFrame(() => {
      const nextControl = document.querySelector<HTMLElement>('.saved-jobs [data-save], .empty-state a');
      nextControl?.focus();
    });
  }
  function clearShortlist() {
    if (!window.confirm('Clear all saved opportunities from this browser? Your profile will be kept.')) return;
    if (!clearSaved()) { setMessage(''); setStorageError('The shortlist could not be cleared. Allow browser storage and try again.'); return; }
    setSaved([]); setStorageError(''); setMessage('Your shortlist is clear.');
    window.requestAnimationFrame(() => document.querySelector<HTMLElement>('.empty-state a')?.focus());
  }
  const actions = { saved, onToggle: toggle };
  return <>
    <a className="skip-link" href="#main" onClick={event => { event.preventDefault(); document.getElementById('main')?.focus(); }}>Skip to content</a>
    <div className="page-shell"><header className="site-header">
      <a className="brand" href="#home" aria-label="JobSinton home"><span className="brand-mark" aria-hidden="true">js</span><span>JobSinton<span className="brand-dot">.</span></span></a>
      <nav className="site-nav" aria-label="Main navigation">{nav.map(([page, label]) => <a href={'#' + page} key={page} aria-current={route.page === page ? 'page' : undefined}>
        {label}{page === 'saved' && <span className="saved-count">{saved.length}</span>}
      </a>)}</nav>
    </header>
    <main ref={main} id="main" tabIndex={-1}>
      {storageError && <p className="notice notice-error storage-notice" role="alert">{storageError} <a href="#saved">Review shortlist</a></p>}
      {!valid && <><PageHeading eyebrow="A DIFFERENT DIRECTION" title="Page not found." /><div className="app-empty"><a className="button primary" href="#opportunities">Explore opportunities</a></div></>}
      {valid && route.page === 'home' && <Home profile={profile} {...actions} />}
      {valid && route.page === 'opportunities' && !job && <Search key={route.hash + ':' + route.revision} query={route.query} {...actions} />}
      {job && <OpportunityDetail key={job.id} job={job} profile={profile} {...actions} />}
      {valid && route.page === 'companies' && !company && <Companies />}
      {company && <CompanyDetail key={company.id} company={company} {...actions} />}
      {valid && route.page === 'saved' && <>
        <PageHeading eyebrow="YOUR SHORTLIST" title="Worth coming back to." description="A little space for your next move." />
        <div className="section-heading saved-toolbar"><p role="status">{saved.length} saved {saved.length === 1 ? 'opportunity' : 'opportunities'}</p><div className="actions">
          <a className="text-link" href="#opportunities">Keep exploring <span aria-hidden="true">↗</span></a>{(saved.length > 0 || storageError) && <button className="text-button" type="button" onClick={clearShortlist}>Clear shortlist</button>}
        </div></div>
        {saved.length ? <JobList jobs={opportunities.filter(j => saved.includes(j.id))} className="saved-jobs" {...actions} /> :
          <EmptyState title="A clear space for your next move." description="Save an opportunity while you explore. It will be waiting here."><a className="button primary" href="#opportunities">Explore opportunities</a></EmptyState>}
      </>}
      {/* Keep the editor mounted so a local draft survives ordinary navigation. */}
      <div hidden={route.page !== 'profile' || !valid}>
        <PageHeading eyebrow="YOUR STARTING POINT" title="A profile with purpose." description="What you do. What matters. Where you want to go." />
        <ProfileEditor onSaved={setProfile} />
      </div>
      <div hidden={route.page !== 'status' || !valid}><Status /></div>
    </main>
    <footer className="page-footer"><span>A Gara’s Workshop project</span><span>Sample catalog · Fictional opportunities and companies</span><a href="#status">Service status</a></footer>
    </div><p className="toast" role="status" aria-live="polite">{message}</p>
  </>;
}
