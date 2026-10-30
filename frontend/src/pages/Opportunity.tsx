import { findCompany, opportunities, salaryLabel, type Opportunity as Job } from '../catalog/data';
import { ROLES, WORK_MODES, type ProfessionalProfile } from '../profile/model';
import { ApplicationDialog, CompanyMark, JobList, SaveButton, Tags, type SavedActions } from '../ui';

function ProfileFacts({ job, profile }: { job: Job; profile: ProfessionalProfile | null }) {
  const shared = profile ? job.tags.filter(tag => profile.technologies.some(t => t.toLowerCase() === tag.toLowerCase())) : [];
  return <section className="fit-section"><p className="eyebrow">A CLOSER LOOK</p><h2>How it lines up</h2>
    {profile ? <ul className="fit-list">
      <li>Your target: {ROLES[profile.role]}. This role: {job.title}.</li>
      <li>{shared.length ? 'Technologies in common: ' + shared.join(', ') + '.' : 'No exact technology names in common with your saved profile.'}</li>
      <li>Your experience: {profile.experienceYears} years. Listed minimum: {job.minimumYears} years.</li>
      <li>Your preference: {WORK_MODES[profile.workMode]}. This role: {job.mode}{job.officeDays ? ', ' + job.officeDays + ' office days per week' : ''}.</li>
      {profile.maxOnsiteDays !== null && <li>Your limit: {profile.maxOnsiteDays} office days per week.</li>}
      <li>You are based in {profile.city}, {profile.country}. This role is based in {job.location}.</li>
    </ul> : <p><a href="#profile">Set up your profile</a> to compare your preferences with the listed requirements.</p>}
    <p className="field-hint">A factual comparison, without ranking or a suitability score. Technology names must match exactly, ignoring case. Location does not establish work eligibility.</p>
    <details><summary>What’s still unknown</summary><p>{job.unknown}</p></details>
  </section>;
}
export function OpportunityDetail({ job, profile, ...actions }: SavedActions & { job: Job; profile: ProfessionalProfile | null }) {
  const company = findCompany(job.company)!;
  return <>
    <nav className="breadcrumb" aria-label="Breadcrumb"><a href="#opportunities">Opportunities</a><span aria-hidden="true">/</span><span>{job.title}</span></nav>
    <section className="job-heading"><div className="company-lockup"><CompanyMark company={company} /><a href={'#companies/' + company.id}>{company.name} <span aria-hidden="true">↗</span></a></div>
      <h1>{job.title}<span className="brand-dot">.</span></h1><p className="detail-subtitle">{job.location} <span aria-hidden="true">·</span> {job.mode} <span aria-hidden="true">·</span> Full-time</p>
      <div className="job-heading-bottom"><Tags items={job.tags} /><span className="muted">Published <time dateTime={job.published}>{job.published}</time></span></div>
    </section>
    <div className="detail-layout"><article className="prose">
      <section><h2>The opportunity</h2><p>{job.intro}</p></section><section><h2>What you’ll work on</h2><ul>{job.tasks.map(x => <li key={x}>{x}</li>)}</ul></section>
      <section><h2>What you’ll bring</h2><ul>{job.requirements.map(x => <li key={x}>{x}</li>)}</ul></section><section><h2>Beyond the role</h2><ul>{company.benefits.map(x => <li key={x}>{x}</li>)}</ul></section>
      <ProfileFacts job={job} profile={profile} />
    </article><aside className="detail-sidebar"><div className="apply-card"><p className="eyebrow">THE ESSENTIALS</p><p className="large-salary">{salaryLabel(job)}</p><p className="muted">EUR gross per year</p>
      <dl><dt>Experience</dt><dd>{job.experience}</dd><dt>Contract</dt><dd>Permanent · Full-time</dd><dt>Work mode</dt><dd>{job.mode}{job.officeDays ? ' · ' + job.officeDays + ' office days / week' : ''}</dd></dl>
      <ApplicationDialog job={job} /><SaveButton job={job} {...actions} />
    </div><a className="company-mini" href={'#companies/' + company.id}><CompanyMark company={company} /><span><strong>{company.name}</strong><span>Meet the team <span aria-hidden="true">↗</span></span></span></a></aside></div>
    <section className="section"><div className="section-heading"><h2>Keep exploring.</h2><a className="text-link" href="#opportunities">All opportunities <span aria-hidden="true">↗</span></a></div><JobList jobs={opportunities.filter(j => j.id !== job.id).slice(0, 2)} {...actions} /></section>
  </>;
}
