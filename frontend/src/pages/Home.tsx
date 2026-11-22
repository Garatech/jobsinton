import { useEffect, useState } from 'react';
import { fetchAtsFeed, getCachedAtsFeed, type AtsFeedResponse } from '../api/ats';
import { evaluateAtsJob, formatFreshness, sortScoredJobs } from '../ats/scoring';
import { useLanguage } from '../i18n/Language';
import { opportunities } from '../catalog/data';
import { ROLES, WORK_MODES, type ProfessionalProfile } from '../profile/model';
import { JobList, type SavedActions } from '../ui';

export function profileSearch(profile: ProfessionalProfile): string {
  const role = { backend: 'Backend', frontend: 'Frontend', 'full-stack': 'Full stack', sap: 'SAP', data: 'Data', platform: 'Platform' }[profile.role];
  return '#opportunities?' + new URLSearchParams({ source: 'sample', role, mode: WORK_MODES[profile.workMode] }).toString();
}

export function Home({ profile, ...actions }: SavedActions & { profile: ProfessionalProfile | null }) {
  const { t, language } = useLanguage();
  const [feed, setFeed] = useState<AtsFeedResponse | null>(() => getCachedAtsFeed());
  const [loadingFeed, setLoadingFeed] = useState<boolean>(() => getCachedAtsFeed() === null);

  useEffect(() => {
    if (feed) return;
    const controller = new AbortController();
    fetchAtsFeed({ signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) {
          setFeed(data);
          setLoadingFeed(false);
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setLoadingFeed(false);
      });
    return () => controller.abort();
  }, [feed]);

  const previewJobs = feed
    ? sortScoredJobs(
        feed.jobs
          .map((job) => evaluateAtsJob(job, 'all-tech', []))
          .filter((item) => item.isMatch),
        'relevance',
      ).slice(0, 4)
    : [];

  return <>
    <section className="hero" aria-labelledby="hero-title"><div className="hero-copy"><p className="eyebrow">{t("YOUR NEXT CHAPTER, IN TUNE")}</p>
      <h1 id="hero-title">{t("Good work starts")}<br />{t("with a better")} <em>{t("fit.")}</em></h1><p className="intro">{t("Your skills. Your priorities. Your next move.")}</p>
      <div className="actions"><a className="button primary" href="#opportunities">{t("Explore opportunities")} <span aria-hidden="true">↗</span></a><a className="text-link" href="#profile">{t("Your profile")}</a></div>
    </div><aside className="connection-card"><p className="eyebrow">{t("YOUR DIRECTION")}</p><div className="signal-art" aria-hidden="true"><span /><span /><span /><span /><span /><span /><span /></div>
      <h2>{t("A next step that makes sense.")}</h2>
      <p className="card-description">{profile ? t(ROLES[profile.role]) + ' · ' + t(WORK_MODES[profile.workMode]) + (language === 'es' ? ' · Reside en ' : ' · Based in ') + profile.country : t('Bring your skills and working preferences together.')}</p>
      {profile && <ul className="dark-tags">{profile.technologies.slice(0, 3).map(tech => <li key={tech}>{tech}</li>)}</ul>}
      <a className="button lime" href={profile ? profileSearch(profile) : '#opportunities'}>{t(profile ? 'Explore this direction' : 'Open live ATS radar')} <span aria-hidden="true">↗</span></a>
      <a className="card-link" href={profile ? '#profile' : '#opportunities?source=sample'}>{t(profile ? 'Review your profile' : 'Browse the sample catalog')}</a>
    </aside></section>

    <section className="section" aria-labelledby="live-radar-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">{t('LIVE PUBLIC ATS BOARDS')}</p>
          <h2 id="live-radar-title">{t('Fresh directly from company career pages.')}</h2>
        </div>
        <a className="text-link" href="#opportunities">{t('Open full ATS board')} <span aria-hidden="true">↗</span></a>
      </div>

      <div className="home-preset-shortcuts" role="group" aria-label={t('Quick stack tracks')}>
        <a className="home-shortcut-pill" href="#opportunities?preset=all-tech">{t('All Tech / Software Roles')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=fullstack">{t('Software & Web Development')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=python-data">{t('Python, Data & Analytics')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=ai-ml">{t('AI & Machine Learning')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=cloud-devops">{t('Cloud, DevOps & Systems')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=qa-cyber">{t('QA Automation & Cybersecurity')}</a>
        <a className="home-shortcut-pill" href="#opportunities?preset=sap">{t('SAP, ERP & Enterprise CRM')}</a>
        <a className="home-shortcut-pill" href="#opportunities?filter=fresh">{t('Fresh (< 48h)')}</a>
      </div>

      {loadingFeed && (
        <p className="muted" role="status">{t('Fetching live postings from public company ATS boards…')}</p>
      )}

      {!loadingFeed && previewJobs.length > 0 && (
        <div className="jobs ats-preview-grid">
          {previewJobs.map((scored) => {
            const freshness = formatFreshness(scored.job.hoursSincePublished, scored.job.publishedAt, language);
            return (
              <article key={scored.job.id} className="job ats-card ats-preview-card">
                <div className="ats-card-body">
                  <div className="ats-card-header">
                    <span className="ats-company">{scored.job.companyName}</span>
                    <span className={'ats-provider-badge ats-' + scored.job.provider}>{scored.job.providerLabel}</span>
                    {freshness && (
                      <span className={'ats-freshness ats-freshness-' + freshness.tier}>{freshness.text}</span>
                    )}
                  </div>
                  <h3 className="ats-card-title">
                    <a href={scored.job.applyUrl} target="_blank" rel="noopener noreferrer">
                      {scored.job.title}
                    </a>
                  </h3>
                  <p className="job-meta">{scored.job.location ?? t('Location not specified')}</p>
                  {scored.matchedKeywords.length > 0 && (
                    <ul className="tags ats-matched-tags" aria-label={t('Matched stack keywords')}>
                      {scored.matchedKeywords.slice(0, 5).map((kw) => (
                        <li key={kw} className="ats-match-chip">{kw}</li>
                      ))}
                    </ul>
                  )}
                </div>
                <div className="ats-card-actions">
                  <a
                    className="button primary ats-apply-btn"
                    href={scored.job.applyUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {t('Apply on Company Site')} <span aria-hidden="true">↗</span>
                  </a>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {!loadingFeed && previewJobs.length === 0 && (
        <p className="muted">
          {t('Live ATS postings appear here automatically when the local Java service is running.')}{' '}
          <a href="#opportunities">{t('Open live ATS radar')}</a>
        </p>
      )}
    </section>

    <section className="section" aria-labelledby="latest-title"><div className="section-heading"><div><p className="eyebrow">{t("ROOM TO EXPLORE")}</p><h2 id="latest-title">{t("A few possibilities.")}</h2></div><a className="text-link" href="#opportunities?source=sample">{t("Browse the sample catalog")} <span aria-hidden="true">↗</span></a></div><JobList jobs={opportunities.slice(0, 3)} {...actions} /></section>
    <section className="direction"><div className="section-heading"><h2>{t("Built around what matters to you.")}</h2></div><div className="feature-grid">
      <article><span className="feature-number">{t("01 / DIRECT ATS SPEED")}</span><h3>{t("First-48h visibility")}</h3><p>{t("Query Greenhouse, Lever, SmartRecruiters, Workable, and Ashby directly without aggregator lag.")}</p></article>
      <article><span className="feature-number">{t("02 / CLEAR REASONS")}</span><h3>{t("Understand the fit")}</h3><p>{t("Highlight your exact stack keywords across Software, Python & Data, AI, Cloud, SAP/ERP, or custom profiles.")}</p></article>
      <article><span className="feature-number">{t("03 / DIRECT APPLY")}</span><h3>{t("Official company portals")}</h3><p>{t("Jump straight to the company’s official application form with zero intermediaries.")}</p></article>
    </div></section>
  </>;
}
