import { translate } from '../i18n/locale';
import { useLanguage } from '../i18n/Language';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AdzunaSearch } from './AdzunaSearch';
import { localSearchAvailable } from '../api/adzuna';
import { findCompany, opportunities } from '../catalog/data';
import { EmptyState, JobList, PageHeading, type SavedActions } from '../ui';
import { useRankedOrder } from '../matching/useRankedOrder';
import type { ProfessionalProfile } from '../profile/model';

type Sort = 'newest' | 'salary' | 'compatibility';
const roles = ['Backend', 'Frontend', 'Full stack', 'Data', 'Platform', 'SAP'];
const modes = ['Remote', 'Hybrid', 'On-site'];
type SearchProps = SavedActions & { query: string; profile: ProfessionalProfile | null };

export function Search(props: SearchProps) {
  const { t } = useLanguage();
  const [available, setAvailable] = useState(false);
  const [live, setLive] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    localSearchAvailable(controller.signal).then(value => { if (!controller.signal.aborted) setAvailable(value); });
    return () => controller.abort();
  }, []);
  return <>
    <PageHeading eyebrow="FIND YOUR NEXT CHAPTER" title="Opportunities." description="Good work, on your terms." />
    {available && <div className="source-selector" role="group" aria-label={t("Opportunity source")}>
      <button type="button" aria-pressed={!live} onClick={() => setLive(false)}>{t("Sample catalog")}</button>
      <button type="button" aria-pressed={live} onClick={() => setLive(true)}>{t("Adzuna · Spain")}</button>
    </div>}
    <div hidden={live}><CatalogSearch {...props} /></div>
    {live && <AdzunaSearch />}
  </>;
}

function CatalogSearch({ query, profile, ...actions }: SearchProps) {
  const { t, euros } = useLanguage();
  const initial = new URLSearchParams(query);
  const [text, setText] = useState((initial.get('q') || '').slice(0, 160));
  const [role, setRole] = useState(roles.includes(initial.get('role') || '') ? initial.get('role')! : '');
  const [workModes, setWorkModes] = useState((initial.get('mode') || '').split(',').filter(m => modes.includes(m)));
  const [salary, setSalary] = useState(['40000', '45000', '55000'].includes(initial.get('salary') || '') ? initial.get('salary')! : '0');
  const requestedSort = initial.get('sort');
  const [sort, setSort] = useState<Sort>(requestedSort === 'compatibility' && profile ? 'compatibility'
    : requestedSort === 'salary' ? 'salary' : 'newest');
  const [notice, setNotice] = useState<'profile' | 'failure' | null>(
    requestedSort === 'compatibility' && !profile ? 'profile' : null);
  const onFailure = useCallback(() => { setNotice('failure'); setSort('newest'); }, []);
  function chooseSort(value: Sort) {
    if (value === 'compatibility' && !profile) { setNotice('profile'); setSort('newest'); return; }
    setNotice(null); setSort(value);
  }
  const [layout, setLayout] = useState(initial.get('view') === 'cards' ? 'cards' : 'list');
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const params = new URLSearchParams();
    const entries = { q: text.trim(), role, mode: workModes.join(','), salary: salary === '0' ? '' : salary, sort: sort === 'newest' ? '' : sort, view: layout === 'list' ? '' : layout };
    for (const [key, value] of Object.entries(entries)) if (value) params.set(key, value);
    history.replaceState(null, '', '#opportunities' + (params.size ? '?' + params.toString() : ''));
  }, [text, role, workModes, salary, sort, layout]);
  const terms = text.trim().toLowerCase().split(/\s+/).filter(Boolean);
  const filtered = opportunities.filter(job => {
    const searchable = [job.title, translate('es', job.title), findCompany(job.company)!.name, job.location, translate('es', job.location), ...job.tags].join(' ').toLowerCase();
    return terms.every(t => searchable.includes(t)) && (!role || role === job.role)
      && (!workModes.length || workModes.includes(job.mode)) && job.salary >= Number(salary);
  }).sort((a, b) => sort === 'salary' ? b.salary - a.salary : b.published.localeCompare(a.published));
  const idsKey = sort === 'compatibility' && profile && filtered.length
    ? JSON.stringify(filtered.map(job => job.id)) : null;
  const order = useRankedOrder(profile, idsKey, onFailure);
  const byId = new Map(filtered.map(job => [job.id, job]));
  const shown = order ? [...order.ranked, ...order.unavailable].map(id => byId.get(id)!) : filtered;
  function reset() { setText(''); setRole(''); setSalary('0'); setWorkModes([]); input.current?.focus(); }
  function chip(label: string, remove: () => void) {
    return <button className="filter-chip" key={label} type="button" aria-label={t('Remove filter:') + ' ' + label}
      onClick={() => { remove(); input.current?.focus(); }}>{label} ×</button>;
  }
  return <>
    <form className="search-bar" role="search" onSubmit={event => event.preventDefault()}>
      <label className="sr-only" htmlFor="query">{t("Search opportunities")}</label><span className="search-icon" aria-hidden="true">⌕</span>
      <input ref={input} id="query" type="search" maxLength={160} placeholder={t("Role, technology or company")} value={text} onChange={e => setText(e.target.value)} />
      <button className="primary" type="submit">{t("Search")}</button>
    </form>
    <div className="search-layout"><aside className="filters" aria-labelledby="filters-title">
      <div className="filter-heading"><h2 id="filters-title">{t("Refine your search")}</h2><button className="text-button" type="button" onClick={reset}>{t("Reset")}</button></div>
      <div className="filter-group"><label htmlFor="role-filter">{t("Role")}</label><select id="role-filter" value={role} onChange={e => setRole(e.target.value)}><option value="">{t("All roles")}</option>{roles.map(r => <option key={r} value={r}>{t(r)}</option>)}</select></div>
      <fieldset className="filter-group"><legend>{t("Work mode")}</legend>{modes.map(mode => <label className="check" key={mode}><input type="checkbox" checked={workModes.includes(mode)}
        onChange={e => setWorkModes(current => e.target.checked ? [...current, mode] : current.filter(m => m !== mode))} /><span>{t(mode)}</span></label>)}</fieldset>
      <div className="filter-group"><label htmlFor="salary-filter">{t("Minimum salary")}</label><select id="salary-filter" value={salary} onChange={e => setSalary(e.target.value)}>
        <option value="0">{t("Any salary")}</option><option value="40000">{euros(40000)} / {t('year')}</option><option value="45000">{euros(45000)} / {t('year')}</option><option value="55000">{euros(55000)} / {t('year')}</option>
      </select><p className="field-hint">{t("Based on the lower end of each range.")}</p></div>
      <div className="filter-note"><p>{t("Looking for a better fit?")}</p><a className="text-link" href="#profile">{t("Review your profile")} <span aria-hidden="true">↗</span></a></div>
    </aside>
    <section className="results" aria-label={t("Search results")}><div className="results-toolbar">
      <p role="status">{filtered.length} {t(filtered.length === 1 ? 'opportunity' : 'opportunities')}</p>
      <div className="result-controls"><label className="sort-label" htmlFor="sort">{t("Sort by")}</label><select id="sort" value={sort} onChange={e => chooseSort(e.target.value as Sort)}><option value="newest">{t("Newest")}</option><option value="salary">{t("Salary")}</option><option value="compatibility">{t("Compatibility")}</option></select>
        <div className="view-toggle" role="group" aria-label={t("Result layout")}>{['list', 'cards'].map(view => <button key={view} type="button" aria-label={t(view === 'list' ? 'List view' : 'Card view')} aria-pressed={layout === view} onClick={() => setLayout(view)}>{view === 'list' ? '☰' : '▦'}</button>)}</div>
      </div></div>
      <div className="active-filters" aria-label={t("Active filters")}>
        {text.trim() && chip('“' + text.trim() + '”', () => setText(''))}
        {role && chip(t(role), () => setRole(''))}
        {workModes.map(mode => chip(t(mode), () => setWorkModes(current => current.filter(m => m !== mode))))}
        {salary !== '0' && chip(t('From') + ' ' + euros(Number(salary)), () => setSalary('0'))}
      </div>
      {notice === 'profile' && <p className="field-hint" role="status">{t("Showing Newest.")} <a href="#profile">{t("Save a profile")}</a> {t("to sort by compatibility.")}</p>}
      {notice === 'failure' && <p className="field-hint" role="status">{t("Compatibility unavailable. Showing Newest.")}{' '}
        <button type="button" className="text-button" onClick={() => chooseSort('compatibility')}>{t("Retry ranking")}</button></p>}
      {idsKey && !order && <p className="field-hint" role="status">{t("Ranking opportunities… Showing Newest for now.")}</p>}
      {order && <p className="sr-only" role="status">{t("Ordered by compatibility.")}</p>}
      {filtered.length ? <JobList jobs={shown} unavailableIds={order?.unavailable} className={layout === 'cards' ? 'cards' : ''} {...actions} /> :
        <EmptyState title="No opportunities found." description="Try a broader search or remove a filter."><button className="primary" type="button" onClick={reset}>{t("Clear filters")}</button></EmptyState>}
      {filtered.length > 0 && <p className="end-note">{t("You’re up to date.")}</p>}
    </section></div>
  </>;
}
