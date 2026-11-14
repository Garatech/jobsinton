import { useLanguage } from '../i18n/Language';
import { findCompany, opportunities, salaryLabel, type Opportunity as Job } from '../catalog/data';
import type { ProfessionalProfile } from '../profile/model';
import { ProfileComparison } from '../matching/Comparison';
import { ApplicationDialog, CompanyMark, JobList, SaveButton, Tags, type SavedActions } from '../ui';

export function OpportunityDetail({ job, profile, ...actions }: SavedActions & { job: Job; profile: ProfessionalProfile | null }) {
  const { t, date, euros, language } = useLanguage();
  const company = findCompany(job.company)!;
  return <>
    <nav className="breadcrumb" aria-label={t("Breadcrumb")}><a href="#opportunities">{t("Opportunities")}</a><span aria-hidden="true">/</span><span>{t(job.title)}</span></nav>
    <section className="job-heading"><div className="company-lockup"><CompanyMark company={company} /><a href={'#companies/' + company.id}>{company.name} <span aria-hidden="true">↗</span></a></div>
      <h1>{t(job.title)}<span className="brand-dot">.</span></h1><p className="detail-subtitle">{t(job.location)} <span aria-hidden="true">·</span> {t(job.mode)} <span aria-hidden="true">·</span> {t("Full-time")}</p>
      <div className="job-heading-bottom"><Tags items={job.tags} /><span className="muted">{t("Published")} <time dateTime={job.published}>{date(job.published)}</time></span></div>
    </section>
    <div className="detail-layout"><article className="prose">
      <section><h2>{t("The opportunity")}</h2><p>{t(job.intro)}</p></section><section><h2>{t("What you’ll work on")}</h2><ul>{job.tasks.map(x => <li key={x}>{t(x)}</li>)}</ul></section>
      <section><h2>{t("What you’ll bring")}</h2><ul>{job.requirements.map(x => <li key={x}>{t(x)}</li>)}</ul></section><section><h2>{t("Beyond the role")}</h2><ul>{company.benefits.map(x => <li key={x}>{t(x)}</li>)}</ul></section>
      <ProfileComparison job={job} profile={profile} />
    </article><aside className="detail-sidebar"><div className="apply-card"><p className="eyebrow">{t("THE ESSENTIALS")}</p><p className="large-salary">{language === 'en' ? salaryLabel(job) : euros(job.salary) + '–' + euros(job.max)}</p><p className="muted">{t("EUR gross per year")}</p>
      <dl><dt>{t("Experience")}</dt><dd>{t(job.experience)}</dd><dt>{t("Contract")}</dt><dd>{t("Permanent · Full-time")}</dd><dt>{t("Work mode")}</dt><dd>{t(job.mode)}{job.officeDays ? ' · ' + job.officeDays + ' ' + t('office days / week') : ''}</dd></dl>
      <ApplicationDialog job={job} /><SaveButton job={job} {...actions} />
    </div><a className="company-mini" href={'#companies/' + company.id}><CompanyMark company={company} /><span><strong>{company.name}</strong><span>{t("Meet the team")} <span aria-hidden="true">↗</span></span></span></a></aside></div>
    <section className="section"><div className="section-heading"><h2>{t("Keep exploring.")}</h2><a className="text-link" href="#opportunities">{t("All opportunities")} <span aria-hidden="true">↗</span></a></div><JobList jobs={opportunities.filter(j => j.id !== job.id).slice(0, 2)} {...actions} /></section>
  </>;
}
