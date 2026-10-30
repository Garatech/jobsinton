# JobSinton — Project Plan

This plan brings together JobSinton's scope, decisions, milestones, open questions, and development notes. It is versioned in the public repository and excludes credentials and private personal information.

The [README](../README.md) provides a concise product introduction. Implementation planning and the development journal are maintained here.

## Current status

The runnable foundation connects a React interface to a Spring Boot status endpoint. A professional profile editor adds explicit browser-local saving, validation, recovery, and a summary. Build and integration checks run in GitHub Actions. There is no production deployment, database, matching, or live provider integration yet. The owner reported successful Windows foundation startup and verification on 2026-10-20; the owner has also confirmed successful completion of the profile acceptance checklist. The owner selected the Current preversion on 2026-10-30. Its application integration adds the fictional catalog, company and opportunity pages, and browser-local saved opportunities. [DESIGN.md](DESIGN.md) records the baseline and post-merge acceptance steps. The owner has verified the integrated application and closed the visual-design stage with a brief page-entry fade. The next planned work is the matching v1 contract and reviewed reference cases. Matching scores and live providers remain separate milestones. Checkout synchronization has not been independently inspected; Codespaces startup remains optional and unverified.

The public repository is [Garatech/jobsinton](https://github.com/Garatech/jobsinton). Initial documentation was published on 2026-10-17.

## Purpose

JobSinton helps software professionals prioritize job opportunities that match their experience, skills, and working preferences. As a professional portfolio project, it documents decisions, limitations, and verification results so they can be understood and reproduced.

## Decisions and constraints

| Area | Decision | Status |
| --- | --- | --- |
| Product name | JobSinton | Agreed on 2026-10-17 |
| Portfolio | Gara's Workshop | Agreed |
| Professional focus | Java backend / full stack development | Agreed |
| Backend | Java 21 and Spring Boot 4.0.8 | Foundation implemented |
| Frontend | React and strict TypeScript | Current interface integrated; versions pinned in package.json |
| Visual design | Current; Ledger list structure and Atlas card search | Owner-selected baseline on 2026-10-30 |
| Database | PostgreSQL | Technical proposal; not yet finalized |
| Editor | Visual Studio Code | Agreed |
| Weekly commitment | 6–10 hours | Agreed |
| Working method | Incremental development with ongoing documentation | Agreed |
| Budget | EUR 0; local development as the primary workflow, included cloud quota optional | Agreed; no paid resources created |
| Initial market | Spain, with a model prepared for other European countries | Agreed |
| Hosting | Evaluate a free option once a demo exists | Pending |
| Documentation structure | Product README; separate working plan | Agreed on 2026-10-17 |
| Code license | No open-source license declared yet | Pending |

The name was chosen after a preliminary web search found no exact matches. Trademark registration, domains, and hosting identifiers have not been verified.

## Initial release scope

Target journey: **configure a profile → browse opportunities → understand the match → save an opportunity**.

- An editable professional profile with priorities and constraints.
- A clearly labeled fictional job catalog for development and demonstrations.
- One live provider, subject to access availability and usage terms.
- Search, filters, opportunity details, and explainable scores.
- Saved opportunities; application tracking follows in a later milestone.

Initially out of scope: automatic job applications, automated CV extraction, multiple simultaneous providers, microservices, and paid services. Public user accounts require verified data isolation; earlier demonstrations use fictional data.

## Profile configuration

The first increment implements one target role, technologies, whole years of experience, residence, one preferred work mode, and conditional on-site days. See [the implemented contract and manual checklist](PROFILE.md). Other dimensions below remain proposals.

Profile configuration will offer a short setup flow with optional advanced controls. Proposed dimensions:

| Dimension | Examples |
| --- | --- |
| Career goals | Backend, frontend, full stack, SAP development, or consulting |
| Skills | Technology, self-assessed proficiency, recency, and context: employment, internship, or project |
| Experience | Responsibilities and relevant experience; mandatory versus desirable requirements |
| Location | Country of residence, countries accepted by the employer, cities, and relocation |
| Work mode | Remote, hybrid, on-site, and maximum on-site days |
| Conditions | Minimum and target pay, currency, pay period, working hours, contract, travel, and on-call duties |
| Languages and education | Language proficiency and relevant qualifications |
| Priorities | Mandatory, preferred, or indifferent; adjustable weights |

## Matching principles

1. Evaluate mandatory constraints before ranking by preferences.
2. Use deterministic rules and documented weights.
3. Distinguish missing information from a mismatch or a match.
4. Show the available information and the reasons behind each score.
5. Version the rules so results can be reproduced.

**A match score is not a probability of being hired.** Professional suitability must not be inferred from personal characteristics unrelated to the role.

Pending design work:

- Define the formula and treatment of unknown values.
- Define skill equivalences, tie-breaking rules, and score explanations.
- Build a small manually reviewed reference set, including incompatible requirements, missing salary information, and changed preference weights.
- Keep the initial scoring engine independent of language models.
- Consider model-based extraction later only if it can be evaluated against reviewed examples.

## European scope

Company location, job location, and countries eligible for remote work remain separate. A remote listing may still restrict the employee's country of residence.

Salary data retains its currency, pay period, and gross/net classification; incompatible amounts are not compared directly. The interface will support localization, with coverage determined by the selected providers.

## Job providers

Preliminary review dated 2026-10-17. Documentation has been consulted, but no authenticated integration has been validated.

| Provider | Evidence | Next step |
| --- | --- | --- |
| InfoJobs | Documented API requiring application credentials | Validate registration, search access, quotas, and usage terms |
| LinkedIn | Restricted job-posting API; not evidence of general job-search access | Do not commit to an initial integration |
| AIApply | Internal job-search documentation; no public API for this use located | Ask the provider if this becomes a priority |
| Adzuna | Job-search API with quotas and usage conditions | Assess geographic coverage and permitted use |

References:

- [InfoJobs operations](https://developer.infojobs.net/documentation/operation-list/index.xhtml)
- [LinkedIn Job Posting API](https://learn.microsoft.com/en-us/linkedin/talent/job-postings/api/overview)
- [AIApply Job Search](https://support.aiapply.co/en/articles/16889656-how-to-use-the-job-search-section)
- [Adzuna API](https://developer.adzuna.com/overview)
- [Adzuna terms](https://developer.adzuna.com/docs/terms_of_service)

Each connector must address pagination, normalization, attribution, rate limits, timeouts, failures, deduplication, and expiry. Demonstrations must remain usable with fictional data if a provider is unavailable.

## Proposed architecture

The frontend, backend, and documentation share one repository.

The backend will be a modular monolith organized around profiles, opportunities, connectors, matching, and applications. Connectors will translate provider responses into a common model so the matching engine does not depend on provider-specific formats.

The foundation implements the documented status API and GitHub Actions checks. A small system package owns the connectivity endpoint; feature modules will follow when needed. See [architecture](ARCHITECTURE.md).

Proposals still to finalize: PostgreSQL, versioned database migrations, and Docker Compose. No database is needed to run this increment.

## Roadmap

Milestones have exit criteria rather than fixed deadlines. Estimates follow once each milestone is broken down into small tasks.

| Milestone | Deliverable | Exit criteria | Status |
| --- | --- | --- | --- |
| 0. Definition | Name, scope, stack, and quality criteria | Main decisions recorded and open questions identified | In progress |
| 1. Foundation | Remote repository, reproducible environment, and automated checks | A clean clone starts using verified instructions | Complete for local workflow: owner reported Windows startup and verification on 2026-10-20; Codespaces remains optional and unverified |
| 2. First journey | Profile, fictional catalog, details, and saved opportunities | The journey works with loading, empty, and error states | Complete for the fictional catalog journey: browser-local profile and shortlist, with owner verification reported on 2026-10-30 |
| 3. Matching v1 | Constraints, weights, and explanations | Reviewed cases cover mismatches, missing data, and changed weights | Next: agree the rule contract and reference cases before implementing scores |
| 4. Live provider | One connector and normalized listings | Authorized access; limits, duplicates, and failures handled | Pending |
| 5. Personal use | Persistence, application tracking, and accounts | User isolation and data deletion verified | Pending |
| 6. Demonstration | Refined interface and a viable zero-cost deployment | Main journey works accessibly on mobile and desktop with fictional data | Pending |
| 7. Portfolio presentation | Screenshots, documentation, and technical explanation | Reproducible demonstration and defensible decisions | Pending |

## Local development

See [the development guide](DEVELOPMENT.md) for local and optional Codespaces setup, startup commands, verification, and switching devices without losing changes.

GitHub is the source of truth for shared work. Commit and push changes to a feature branch before switching machines. A Codespace can be reopened from either device, but its unpushed commits are not yet backed up in the repository.

The owner confirmed successful local foundation startup and verification on 2026-10-20. Local Git status has not been independently inspected because the remote development terminal cannot initialize. Preserve local changes and inspect Git status before synchronizing. Codespaces remains optional; its unverified startup does not block the local workflow.

No provider API keys or database are required for this stage.

## Development and evaluation

Each session follows: **small objective → design → implementation → verification → documentation**.

Each update records what changed, why, how it was checked, and what remains unresolved. An issue defines observable acceptance criteria, and a focused branch and pull request track the work. Review covers correctness, clarity, relevant security risks, user experience, and the effect on data or integrations before merging.

Verification is proportional to the change: reference cases for matching, integration checks for persistence and connectors, and end-to-end checks for critical journeys. Documentation changes require accurate content and valid internal links. Verification records include only tests and independent reviews that actually took place.

See [CONTRIBUTING](../CONTRIBUTING.md) for the detailed review process and [the pull request template](../.github/pull_request_template.md) for change records.

Each milestone closes with an explanation of the implementation, an important design decision, and an alternative considered. Open questions are revisited before expanding the scope.

## Repository access and data

The repository is public. Direct changes and merges are reserved for `Garatech` and tools explicitly authorized by the owner. Others can work in forks and propose pull requests; submitting a PR does not integrate it or grant write access.

See the [repository access review](repository-access.md). GitHub settings enforce permissions; documentation only records their reviewed state.

Credentials, real CVs, personal demonstration profiles, and job listings without redistribution rights are excluded from the public repository. The code license remains undecided.

## Session log

### 2026-10-17 — Project foundation

- Chose JobSinton as the product name.
- Recorded the agreed technologies, weekly commitment, budget, and initial market.
- Prepared the README, contribution process, pull request template, and VS Code workspace.
- Published the initial documentation in `Garatech/jobsinton`.
- Reviewed repository permissions and required workflow approval for all external contributors.
- No application, CI pipeline, deployment, or live integrations were implemented.
- Next technical step: inspect the development environment and build the first runnable increment.

### 2026-10-17 — Documentation and product README

- Separated the concise product introduction from this working plan.
- Preserved the detailed scope, provider notes, roadmap, review process, and session history.
- Updated the contribution guide, access review, pull request template, and configuration comments.
- Local terminal access was unavailable during this documentation update because the execution environment could not initialize its sandbox. Remote changes can be completed through GitHub; refresh the local checkout before the next coding session.

### 2026-10-18 — Runnable foundation and shared development

- Confirmed that owner-approved PR #2 is merged into main at `3a73c67`.
- Opened issue #3 and implemented the first real Java-to-browser connection on a focused branch.
- Added a bounded connection check, retry behavior, backend HTTP tests, and desktop/mobile browser integration tests.
- Added GitHub Actions, a development guide, an optional dev container, architecture notes, and CHANGELOG.md.
- Decision: keep this increment free of database/provider setup so its startup and connectivity behavior can be understood and checked independently.
- Alternative deferred: building profile, storage, and matching scaffolding together would make the first review much larger without validating user value.
- Local terminal initialization failed before checkout inspection. Local cleanliness, installed tools, and synchronization cannot be certified.
- Review CI evidence in the implementation PR; Codespaces and owner Windows startup remain separate acceptance checks.
- Next session: start this foundation, review the request path and visual direction, then define the smallest profile-preferences contract before implementing it.

### 2026-10-18 — Documentation update

- Opened `docs/documentation-update` to refine wording across the project documentation.
- Followed up in `docs/editorial-refinement` with a consistent, project-focused tone.
- Preserved the technical scope, decisions, instructions, and verification records.
- Checked command examples and link targets against the previous revision; application code and configuration are unchanged.
- Next step remains starting the foundation and reviewing the first profile-preferences contract.

### 2026-10-20 — Professional profile and local verification

- Recorded owner-reported Windows foundation startup and verification after selecting Java 21 and Node 24; Maven 3.10.0 was reported during setup.
- Opened issue #9 and branch `feat/profile-preferences` for the first profile editor.
- Added explicit browser-local saving, editing, a last-saved summary, confirmed clearing, and recovery from invalid or unavailable storage.
- Added validation and browser tests, including keyboard use, narrow viewports, conditional day limits, and backend outages.
- Decision: keep one profile in the current browser without introducing accounts or database setup. Profiles do not transfer between devices with Git.
- Alternative deferred: a profile API and database would require server-side validation, persistence, and isolation beyond this increment.
- Added `docs/PROFILE.md` with post-merge commands and a manual acceptance checklist. Updated Windows troubleshooting in `docs/DEVELOPMENT.md`.
- Verification: see the profile PR checks for the final tested commit. Local execution of this increment and human visual acceptance remain pending.
- Next step: complete the profile acceptance checklist, then define the fictional job catalog and first browsing journey.

### 2026-10-29 — Visual design studies

- Prepared a source-based Current reference and four standalone HTML/CSS/JavaScript alternatives on `design/visual-exploration`, tracked in issue #11.
- Added color and layout research, an offline comparison page, review-note export and browser verification.
- Resolved the interrupted study's mobile layout failure and completed the pending keyboard focus correction.
- Decision: compare fictional journeys in an isolated package before applying a design system to the application.
- Alternative deferred: selecting a palette and immediately restyling the production interface would make visual comparison and review harder.
- Verification evidence is supplied by the design workflow and the implementation PR. Package creation requires successful browser checks.
- Owner confirmation of the existing profile's manual checklist is recorded above; it is separate from visual acceptance of these new studies.
- Next step: review the alternatives, record the chosen combination in `docs/DESIGN.md`, then implement the selected system before the fictional catalog.

### 2026-10-30 — Current application integration

- The owner selected the Current preversion as the primary design, with no further visual exploration requested.
- Kept `design/current-preversion` separate and implemented its approved design in the existing React application on `feat/current-interface`.
- Added routed overview, search, company, detail, saved and profile pages; retained the real service check on a dedicated status page.
- Preserved existing profile records and draft/save separation. Added independently stored saved opportunities with explicit error recovery.
- Decision: retain the project's typed build and shared components while adopting the approved HTML structure and CSS. A separate static application would duplicate state and lose the tested profile behavior.
- The fictional catalog supports the first complete journey; descriptive profile facts do not implement matching v1 or live applications.
- Verification: the PR records build, integration, storage-recovery and responsive results. Local terminal access remains unavailable; owner acceptance follows the checklist in DESIGN.md after merge.
- Next step: verify the integrated journey, then define deterministic matching rules and a reviewed set of reference cases using this visual baseline.

### 2026-10-30 — Page transitions and design-stage close

- Recorded owner confirmation that the integrated Current application works as expected.
- Added a 200 ms incoming-page fade, preserving the shared shell, profile drafts and immediate interaction.
- Respected reduced-motion preferences, including changes during a transition, and retained immediate navigation when the animation API is unavailable.
- Decision: animate only page identity changes with a small native hook. Exit delays and remounting the page tree would add latency or risk losing local drafts.
- Added focused browser coverage for animation completion, keyboard navigation, history, rapid changes, reduced motion and fallback behavior. The PR records the actual results for its tested commit.
- The current visual-design stage is closed. Further work uses Current as the baseline; no additional redesign is planned.
- Next step: define deterministic matching constraints, missing-data treatment and a reviewed set of examples before implementing matching v1.
