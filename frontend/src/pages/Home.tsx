import { useLanguage } from '../i18n/Language';
import { opportunities } from '../catalog/data';
import { ROLES, WORK_MODES, type ProfessionalProfile } from '../profile/model';
import { JobList, type SavedActions } from '../ui';

export function profileSearch(profile: ProfessionalProfile): string {
  const role = { backend: 'Backend', frontend: 'Frontend', 'full-stack': 'Full stack', sap: 'SAP', data: 'Data', platform: 'Platform' }[profile.role];
  return '#opportunities?' + new URLSearchParams({ role, mode: WORK_MODES[profile.workMode] }).toString();
}
export function Home({ profile, ...actions }: SavedActions & { profile: ProfessionalProfile | null }) {
  const { t, language } = useLanguage();
  return <>
    <section className="hero" aria-labelledby="hero-title"><div className="hero-copy"><p className="eyebrow">{t("YOUR NEXT CHAPTER, IN TUNE")}</p>
      <h1 id="hero-title">{t("Good work starts")}<br />{t("with a better")} <em>{t("fit.")}</em></h1><p className="intro">{t("Your skills. Your priorities. Your next move.")}</p>
      <div className="actions"><a className="button primary" href="#opportunities">{t("Explore opportunities")} <span aria-hidden="true">↗</span></a><a className="text-link" href="#profile">{t("Your profile")}</a></div>
    </div><aside className="connection-card"><p className="eyebrow">{t("YOUR DIRECTION")}</p><div className="signal-art" aria-hidden="true"><span /><span /><span /><span /><span /><span /><span /></div>
      <h2>{t("A next step that makes sense.")}</h2>
      <p className="card-description">{profile ? t(ROLES[profile.role]) + ' · ' + t(WORK_MODES[profile.workMode]) + (language === 'es' ? ' · Reside en ' : ' · Based in ') + profile.country : t('Bring your skills and working preferences together.')}</p>
      {profile && <ul className="dark-tags">{profile.technologies.slice(0, 3).map(t => <li key={t}>{t}</li>)}</ul>}
      <a className="button lime" href={profile ? profileSearch(profile) : '#profile'}>{t(profile ? 'Explore this direction' : 'Set up your profile')} <span aria-hidden="true">↗</span></a>
      <a className="card-link" href={profile ? '#profile' : '#opportunities'}>{t(profile ? 'Review your profile' : 'Browse the sample catalog')}</a>
    </aside></section>
    <section className="section" aria-labelledby="latest-title"><div className="section-heading"><div><p className="eyebrow">{t("ROOM TO EXPLORE")}</p><h2 id="latest-title">{t("A few possibilities.")}</h2></div><a className="text-link" href="#opportunities">{t("View all opportunities")} <span aria-hidden="true">↗</span></a></div><JobList jobs={opportunities.slice(0, 3)} {...actions} /></section>
    <section className="direction"><div className="section-heading"><h2>{t("Built around what matters to you.")}</h2></div><div className="feature-grid">
      <article><span className="feature-number">{t("01 / YOUR PRIORITIES")}</span><h3>{t("A profile with purpose")}</h3><p>{t("Keep your skills and working preferences in one place.")}</p></article>
      <article><span className="feature-number">{t("02 / CLEAR REASONS")}</span><h3>{t("Understand the fit")}</h3><p>{t("See what lines up, what differs and what is still unknown.")}</p></article>
      <article><span className="feature-number">{t("03 / ROOM TO EXPLORE")}</span><h3>{t("Your own shortlist")}</h3><p>{t("Save the opportunities you want to come back to.")}</p></article>
    </div></section>
  </>;
}
