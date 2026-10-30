import { useEffect, useRef, useState } from 'react';
import { findCompany, opportunities } from '../catalog/data';
import { EmptyState, JobList, PageHeading, type SavedActions } from '../ui';

const roles = ['Backend', 'Frontend', 'Full stack', 'Data', 'Platform', 'SAP'];
const modes = ['Remote', 'Hybrid', 'On-site'];
export function Search({ query, ...actions }: SavedActions & { query: string }) {
  const initial = new URLSearchParams(query);
  const [text, setText] = useState((initial.get('q') || '').slice(0, 160));
  const [role, setRole] = useState(roles.includes(initial.get('role') || '') ? initial.get('role')! : '');
  const [workModes, setWorkModes] = useState((initial.get('mode') || '').split(',').filter(m => modes.includes(m)));
  const [salary, setSalary] = useState(['40000', '45000', '55000'].includes(initial.get('salary') || '') ? initial.get('salary')! : '0');
  const [sort, setSort] = useState(initial.get('sort') === 'salary' ? 'salary' : 'newest');
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
    const searchable = [job.title, findCompany(job.company)!.name, job.location, ...job.tags].join(' ').toLowerCase();
    return terms.every(t => searchable.includes(t)) && (!role || role === job.role)
      && (!workModes.length || workModes.includes(job.mode)) && job.salary >= Number(salary);
  }).sort((a, b) => sort === 'salary' ? b.salary - a.salary : b.published.localeCompare(a.published));
  function reset() { setText(''); setRole(''); setSalary('0'); setWorkModes([]); input.current?.focus(); }
  function chip(label: string, remove: () => void) {
    return <button className="filter-chip" key={label} type="button" aria-label={'Remove filter: ' + label}
      onClick={() => { remove(); input.current?.focus(); }}>{label} ×</button>;
  }
  return <>
    <PageHeading eyebrow="FIND YOUR NEXT CHAPTER" title="Opportunities." description="Good work, on your terms." />
    <form className="search-bar" role="search" onSubmit={event => event.preventDefault()}>
      <label className="sr-only" htmlFor="query">Search opportunities</label><span className="search-icon" aria-hidden="true">⌕</span>
      <input ref={input} id="query" type="search" maxLength={160} placeholder="Role, technology or company" value={text} onChange={e => setText(e.target.value)} />
      <button className="primary" type="submit">Search</button>
    </form>
    <div className="search-layout"><aside className="filters" aria-labelledby="filters-title">
      <div className="filter-heading"><h2 id="filters-title">Refine your search</h2><button className="text-button" type="button" onClick={reset}>Reset</button></div>
      <div className="filter-group"><label htmlFor="role-filter">Role</label><select id="role-filter" value={role} onChange={e => setRole(e.target.value)}><option value="">All roles</option>{roles.map(r => <option key={r}>{r}</option>)}</select></div>
      <fieldset className="filter-group"><legend>Work mode</legend>{modes.map(mode => <label className="check" key={mode}><input type="checkbox" checked={workModes.includes(mode)}
        onChange={e => setWorkModes(current => e.target.checked ? [...current, mode] : current.filter(m => m !== mode))} /><span>{mode}</span></label>)}</fieldset>
      <div className="filter-group"><label htmlFor="salary-filter">Minimum salary</label><select id="salary-filter" value={salary} onChange={e => setSalary(e.target.value)}>
        <option value="0">Any salary</option><option value="40000">€40,000 / year</option><option value="45000">€45,000 / year</option><option value="55000">€55,000 / year</option>
      </select><p className="field-hint">Based on the lower end of each range.</p></div>
      <div className="filter-note"><p>Looking for a better fit?</p><a className="text-link" href="#profile">Review your profile <span aria-hidden="true">↗</span></a></div>
    </aside>
    <section className="results" aria-label="Search results"><div className="results-toolbar">
      <p role="status">{filtered.length} {filtered.length === 1 ? 'opportunity' : 'opportunities'}</p>
      <div className="result-controls"><label className="sort-label" htmlFor="sort">Sort by</label><select id="sort" value={sort} onChange={e => setSort(e.target.value)}><option value="newest">Newest</option><option value="salary">Salary</option></select>
        <div className="view-toggle" role="group" aria-label="Result layout">{['list', 'cards'].map(view => <button key={view} type="button" aria-label={view === 'list' ? 'List view' : 'Card view'} aria-pressed={layout === view} onClick={() => setLayout(view)}>{view === 'list' ? '☰' : '▦'}</button>)}</div>
      </div></div>
      <div className="active-filters" aria-label="Active filters">
        {text.trim() && chip('“' + text.trim() + '”', () => setText(''))}
        {role && chip(role, () => setRole(''))}
        {workModes.map(mode => chip(mode, () => setWorkModes(current => current.filter(m => m !== mode))))}
        {salary !== '0' && chip('From €' + Number(salary).toLocaleString('en-GB'), () => setSalary('0'))}
      </div>
      {filtered.length ? <JobList jobs={filtered} className={layout === 'cards' ? 'cards' : ''} {...actions} /> :
        <EmptyState title="No opportunities found." description="Try a broader search or remove a filter."><button className="primary" type="button" onClick={reset}>Clear filters</button></EmptyState>}
      {filtered.length > 0 && <p className="end-note">You’re up to date.</p>}
    </section></div>
  </>;
}
