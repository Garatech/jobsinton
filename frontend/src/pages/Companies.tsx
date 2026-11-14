import { translate } from '../i18n/locale';
import { useLanguage } from '../i18n/Language';
import { useRef, useState } from 'react';
import { companies, opportunities, type Company } from '../catalog/data';
import { CompanyMark, EmptyState, JobList, PageHeading, type SavedActions } from '../ui';

export function Companies() {
  const { t } = useLanguage();
  const [query, setQuery] = useState('');
  const input = useRef<HTMLInputElement>(null);
  const terms = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
  const filtered = companies.filter(c => terms.every(term => [c.name, c.sector, c.location, translate('es', c.sector), translate('es', c.location)].join(' ').toLowerCase().includes(term)));
  return <>
    <PageHeading eyebrow="THE PEOPLE BEHIND THE WORK" title="Find your kind of team." description="A closer look at where you could work." />
    <form className="search-bar" role="search" onSubmit={e => e.preventDefault()}><label className="sr-only" htmlFor="company-query">{t("Search companies")}</label><span className="search-icon" aria-hidden="true">⌕</span>
      <input ref={input} type="search" id="company-query" maxLength={160} value={query} onChange={e => setQuery(e.target.value)} placeholder={t("Company, sector or city")} /><button className="primary" type="submit">{t("Search")}</button></form>
    <p className="count-line" role="status">{filtered.length} {t(filtered.length === 1 ? 'company' : 'companies')}</p>
    <div className="company-grid">{filtered.map(c => <article key={c.id} className="company-card">
      <div className="company-card-top"><CompanyMark company={c} /><span className="pill">{t(c.model)}</span></div><p className="eyebrow">{t(c.sector)}</p>
      <h2><a href={'#companies/' + c.id}>{c.name}</a></h2><p>{t(c.desc)}</p>
      <div className="company-card-bottom"><span>{t(c.location)}</span><a href={'#companies/' + c.id}>{opportunities.filter(j => j.company === c.id).length} {t("open roles")} <span aria-hidden="true">↗</span></a></div>
    </article>)}</div>
    {!filtered.length && <EmptyState title="No companies found." description="Try another name, sector or city."><button type="button" className="primary" onClick={() => { setQuery(''); input.current?.focus(); }}>{t("Clear search")}</button></EmptyState>}
  </>;
}
export function CompanyDetail({ company: c, ...actions }: SavedActions & { company: Company }) {
  const { t } = useLanguage();
  const jobs = opportunities.filter(j => j.company === c.id);
  return <>
    <nav className="breadcrumb" aria-label={t("Breadcrumb")}><a href="#companies">{t("Companies")}</a><span aria-hidden="true">/</span><span>{c.name}</span></nav>
    <section className="company-hero"><div><CompanyMark company={c} /><p className="eyebrow">{t(c.sector)}</p><h1>{c.name}<span className="brand-dot">.</span></h1><p className="intro">{t(c.desc)}</p>
      <a className="button primary" href="#open-roles" onClick={e => { e.preventDefault(); document.getElementById('open-roles')?.focus(); document.getElementById('open-roles')?.scrollIntoView(); }}>{t("Explore open roles")} <span aria-hidden="true">↓</span></a>
    </div><aside className="company-facts"><p className="eyebrow">{t("AT A GLANCE")}</p><dl><dt>{t("Based in")}</dt><dd>{t(c.location)}</dd><dt>{t("Team")}</dt><dd>{t(c.size)}</dd><dt>{t("Working model")}</dt><dd>{t(c.model)}</dd><dt>{t("Open roles")}</dt><dd>{jobs.length} {t("opportunities")}</dd></dl></aside></section>
    <div className="detail-layout"><div className="prose"><section><h2>{t("About the team")}</h2><p>{t(c.about)}</p></section><section><h2>{t("How work happens")}</h2><ul>{c.values.map(v => <li key={v}>{t(v)}</li>)}</ul></section></div>
      <aside className="soft-card"><p className="eyebrow">{t("BEYOND THE ROLE")}</p><h2>{t("A little more room.")}</h2><ul className="benefits">{c.benefits.map(v => <li key={v}>{t(v)}</li>)}</ul></aside></div>
    <section className="section" id="open-roles" tabIndex={-1}><div className="section-heading"><h2>{t("Open roles")}</h2><span className="muted">{jobs.length} {t("opportunities")}</span></div><JobList jobs={jobs} {...actions} /></section>
  </>;
}
