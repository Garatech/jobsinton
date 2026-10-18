# JobSinton — Project Plan

I use this document to track JobSinton's scope, decisions, milestones, open questions, and session notes as I develop the project with assistance. It is versioned in the public repository, so credentials and private personal information do not belong here.

The [README](../README.md) is the concise product introduction. I keep implementation planning and my development journal here.

## Current status

The first runnable foundation connects a React interface to a Spring Boot status endpoint. Build and integration checks are defined in GitHub Actions. There is no production deployment, database, matching, or live provider integration yet. See the PR checks for actual validation results; Windows checkout synchronization and the first Codespaces startup remain unverified.

The public repository is [Garatech/jobsinton](https://github.com/Garatech/jobsinton). Initial documentation was published on 2026-10-17.

## Purpose

I'm building JobSinton to help software professionals prioritize job opportunities that match their experience, skills, and working preferences. It is also part of my professional portfolio, so I want decisions, limitations, and verification results to be understandable and reproducible.

## Decisions and constraints

| Area | Decision | Status |
| --- | --- | --- |
| Product name | JobSinton | Agreed on 2026-10-17 |
| Portfolio | Gara's Workshop | Agreed |
| Professional focus | Java backend / full stack development | Agreed |
| Backend | Java 21 and Spring Boot 4.0.8 | Foundation implemented |
| Frontend | React and strict TypeScript | Foundation implemented; versions pinned in package.json |
| Database | PostgreSQL | Technical proposal; not yet finalized |
| Editor | Visual Studio Code | Agreed |
| Weekly commitment | 6–10 hours | Agreed |
| Working method | Develop step by step with assistance and ongoing documentation | Agreed |
| Budget | EUR 0; local development or available included cloud quota | Agreed; no paid resources created |
| Initial market | Spain, with a model prepared for other European countries | Agreed |
| Hosting | Evaluate a free option once a demo exists | Pending |
| Project language | English for code and repository documentation | Agreed on 2026-10-17 |
| Documentation structure | Product README; separate working plan | Agreed on 2026-10-17 |
| Code license | No open-source license declared yet | Pending |

I chose the name after a preliminary web search found no exact matches. Trademark registration, domains, and hosting identifiers have not been verified.

English applies to identifiers, comments, tests, developer-facing messages, documentation, commit messages, and issue/PR text. Development discussions may remain in Spanish. Future localized interface text and original provider content may use other languages.

## Initial release scope

Target journey: **configure a profile → browse opportunities → understand the match → save an opportunity**.

- An editable professional profile with priorities and constraints.
- A clearly labeled fictional job catalog for development and demonstrations.
- One live provider, subject to access availability and usage terms.
- Search, filters, opportunity details, and explainable scores.
- Saved opportunities; application tracking follows in a later milestone.

Initially out of scope: automatic job applications, AI-based CV extraction, multiple simultaneous providers, microservices, and paid services. Public user accounts require verified data isolation; earlier demonstrations use fictional data.

## Profile configuration

I plan to offer a short setup flow with optional advanced controls. These are the proposed dimensions:

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

**A match score is not a probability of being hired.** The matching rules must not infer professional suitability from personal characteristics unrelated to the role.

Pending design work:

- Define the formula and treatment of unknown values.
- Define skill equivalences, tie-breaking rules, and score explanations.
- Build a small manually reviewed reference set, including incompatible requirements, missing salary information, and changed preference weights.
- Keep the initial scoring engine free of AI dependencies.
- Consider AI-assisted extraction later only if it can be evaluated against reviewed examples.

## European scope

I will keep company location, job location, and countries eligible for remote work separate. A remote listing may still restrict the employee's country of residence.

I will preserve currency, pay period, and gross/net classification so incompatible salary amounts are not compared directly. I will prepare the interface for localization, with coverage limited to what the selected providers can deliver.

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

I keep the frontend, backend, and documentation in one repository.

The backend will be a modular monolith organized around profiles, opportunities, connectors, matching, and applications. Connectors will translate provider responses into a common model so the matching engine does not depend on provider-specific formats.

The foundation implements the documented status API and GitHub Actions checks. A small system package owns the connectivity endpoint; feature modules will follow when needed. See [architecture](ARCHITECTURE.md).

Proposals still to finalize: PostgreSQL, versioned database migrations, and Docker Compose. No database is needed to run this increment.

## Roadmap

I track milestones by their exit criteria rather than fixed deadlines, and estimate them once I have broken them down into small tasks.

| Milestone | Deliverable | Exit criteria | Status |
| --- | --- | --- | --- |
| 0. Definition | Name, scope, stack, and quality criteria | Main decisions recorded and open questions identified | In progress |
| 1. Foundation | Remote repository, reproducible environment, and automated checks | A clean clone starts using verified instructions | In progress: runnable foundation and CI added; startup in my development environment pending |
| 2. First journey | Profile, fictional catalog, details, and saved opportunities | The journey works with loading, empty, and error states | Pending |
| 3. Matching v1 | Constraints, weights, and explanations | Reviewed cases cover mismatches, missing data, and changed weights | Pending |
| 4. Live provider | One connector and normalized listings | Authorized access; limits, duplicates, and failures handled | Pending |
| 5. Personal use | Persistence, application tracking, and accounts | User isolation and data deletion verified | Pending |
| 6. Demonstration | Refined interface and a viable zero-cost deployment | Main journey works accessibly on mobile and desktop with fictional data | Pending |
| 7. Portfolio presentation | Screenshots, documentation, and technical explanation | Reproducible demonstration and defensible decisions | Pending |

## Local development

See [the development guide](DEVELOPMENT.md) for local and optional Codespaces setup, startup commands, verification, and switching devices without losing changes.

GitHub is the source of truth for shared work. Commit and push changes to a feature branch before switching machines. A Codespace can be reopened from either device, but its unpushed commits are not yet backed up in the repository.

Verification of my current Windows checkout was blocked because the assistant terminal could not initialize. Before synchronizing, I need to preserve any local changes and inspect Git status. Codespaces startup also remains to be verified; milestone 1 stays open until one of my development environments starts successfully.

No provider API keys or database are required for this stage.

## Development and evaluation

I work through each session in this order: **small objective → explanation → joint implementation → verification → documentation**.

For each update, I record what changed, why, how it was checked, and what remains unresolved. I use an issue with observable acceptance criteria, a focused branch, and a pull request. Before merging, I review correctness, clarity, relevant security risks, user experience, and the effect on data or integrations.

Verification is proportional to the change: reference cases for matching, integration checks for persistence and connectors, and end-to-end checks for critical journeys. Documentation changes require accurate content and valid internal links. I only record tests and independent reviews that actually took place.

See [CONTRIBUTING](../CONTRIBUTING.md) for the detailed review process and [the pull request template](../.github/pull_request_template.md) for change records.

At the end of each milestone, I explain the implementation, one important design decision, and an alternative I considered. I revisit anything I do not yet understand before expanding the scope.

## Repository access and data

The repository is public. I manage direct changes and merges through `Garatech` and tools I explicitly authorize. Others can work in forks and propose pull requests; submitting a PR does not integrate it or grant write access.

See the [repository access review](repository-access.md). GitHub settings enforce permissions; documentation only records their reviewed state.

I keep credentials, real CVs, personal demonstration profiles, and job listings that cannot be redistributed out of the public repository. I have not yet decided on a code license.

## Session log

### 2026-10-17 — Project foundation

- Chose JobSinton as the product name.
- Recorded the agreed technologies, weekly commitment, budget, and initial market.
- Prepared the README, contribution process, pull request template, and VS Code workspace.
- Published the initial documentation in `Garatech/jobsinton`.
- Reviewed repository permissions and required workflow approval for all external contributors.
- No application, CI pipeline, deployment, or live integrations were implemented.
- Next technical step: inspect my development environment and build the first runnable increment with assistance.

### 2026-10-17 — English documentation and product README

- Adopted English for code and repository documentation.
- Separated the concise product introduction from this working plan.
- Preserved the detailed scope, provider notes, roadmap, review process, and session history.
- Translated the contribution guide, access review, pull request template, and configuration comments.
- Local terminal access was unavailable during this documentation update because the execution environment could not initialize its sandbox. Remote changes can be completed through GitHub; refresh the local checkout before the next coding session.

### 2026-10-18 — Runnable foundation and shared development

- Confirmed that PR #2, which I approved, is merged into main at `3a73c67`.
- Opened issue #3 and implemented the first real Java-to-browser connection on a focused branch.
- Added a bounded connection check, retry behavior, backend HTTP tests, and desktop/mobile browser integration tests.
- Added GitHub Actions, a development guide, an optional dev container, architecture notes, and CHANGELOG.md.
- Decision: keep this increment free of database/provider setup so its startup and connectivity behavior can be understood and checked independently.
- Alternative deferred: building profile, storage, and matching scaffolding together would make the first review much larger without validating user value.
- Local terminal initialization failed before checkout inspection. Local cleanliness, installed tools, and synchronization cannot be certified.
- Review CI evidence in the implementation PR; Codespaces startup and startup on my Windows machine remain separate acceptance checks.
- Next session: start this foundation with assistance, review the request path and visual direction, then define the smallest profile-preferences contract before implementing it.

### 2026-10-18 — Documentation update

- Opened `docs/documentation-update` to refine wording across the project documentation.
- Kept the existing scope, decisions, technical instructions, and verification records.
- Checked command examples and link targets against the previous revision; application code and configuration are unchanged.
- Next step remains starting the foundation and reviewing the first profile-preferences contract.
