import { useRef, useState } from 'react';
import { companies, opportunities, type Company } from '../catalog/data';
import { CompanyMark, EmptyState, JobList, PageHeading, type SavedActions } from '../ui';

export function Companies() {
  const [query, setQuery] = useState('');
  const input = useRef<HTMLInputElement>(null);
  const terms = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
  const filtered = companies.filter(c => terms.every(t => [c.name, c.sector, c.location].join(' ').toLowerCase().includes(t)));
  return <>
    <PageHeading eyebrow="THE PEOPLE BEHIND THE WORK" title="Find your kind of team." description="A closer look at where you could work." />
    <form className="search-bar" role="search" onSubmit={e => e.preventDefault()}><label className="sr-only" htmlFor="company-query">Search companies</label><span className="search-icon" aria-hidden="true">⌕</span>
      <input ref={input} type="search" id="company-query" maxLength={160} value={query} onChange={e => setQuery(e.target.value)} placeholder="Company, sector or city" /><button className="primary" type="submit">Search</button></form>
    <p className="count-line" role="status">{filtered.length} {filtered.length === 1 ? 'company' : 'companies'}</p>
    <div className="company-grid">{filtered.map(c => <article key={c.id} className="company-card">
      <div className="company-card-top"><CompanyMark company={c} /><span className="pill">{c.model}</span></div><p className="eyebrow">{c.sector}</p>
      <h2><a href={'#companies/' + c.id}>{c.name}</a></h2><p>{c.desc}</p>
      <div className="company-card-bottom"><span>{c.location}</span><a href={'#companies/' + c.id}>{opportunities.filter(j => j.company === c.id).length} open roles <span aria-hidden="true">↗</span></a></div>
    </article>)}</div>
    {!filtered.length && <EmptyState title="No companies found." description="Try another name, sector or city."><button type="button" className="primary" onClick={() => { setQuery(''); input.current?.focus(); }}>Clear search</button></EmptyState>}
  </>;
}
export function CompanyDetail({ company: c, ...actions }: SavedActions & { company: Company }) {
  const jobs = opportunities.filter(j => j.company === c.id);
  return <>
    <nav className="breadcrumb" aria-label="Breadcrumb"><a href="#companies">Companies</a><span aria-hidden="true">/</span><span>{c.name}</span></nav>
    <section className="company-hero"><div><CompanyMark company={c} /><p className="eyebrow">{c.sector}</p><h1>{c.name}<span className="brand-dot">.</span></h1><p className="intro">{c.desc}</p>
      <a className="button primary" href="#open-roles" onClick={e => { e.preventDefault(); document.getElementById('open-roles')?.focus(); document.getElementById('open-roles')?.scrollIntoView(); }}>Explore open roles <span aria-hidden="true">↓</span></a>
    </div><aside className="company-facts"><p className="eyebrow">AT A GLANCE</p><dl><dt>Based in</dt><dd>{c.location}</dd><dt>Team</dt><dd>{c.size}</dd><dt>Working model</dt><dd>{c.model}</dd><dt>Open roles</dt><dd>{jobs.length} opportunities</dd></dl></aside></section>
    <div className="detail-layout"><div className="prose"><section><h2>About the team</h2><p>{c.about}</p></section><section><h2>How work happens</h2><ul>{c.values.map(v => <li key={v}>{v}</li>)}</ul></section></div>
      <aside className="soft-card"><p className="eyebrow">BEYOND THE ROLE</p><h2>A little more room.</h2><ul className="benefits">{c.benefits.map(v => <li key={v}>{v}</li>)}</ul></aside></div>
    <section className="section" id="open-roles" tabIndex={-1}><div className="section-heading"><h2>Open roles</h2><span className="muted">{jobs.length} opportunities</span></div><JobList jobs={jobs} {...actions} /></section>
  </>;
}
