# JobSinton — Project Plan

This is the working document for the project owner and their development assistant. It contains scope, decisions, milestones, open questions, and session notes. It is versioned in the public repository; do not add credentials or private personal information.

The [README](../README.md) is the concise product introduction. Keep implementation planning and the development journal here.

## Current status

Documentation and repository preparation are underway. There is no runnable application, production deployment, CI pipeline, or live provider integration yet.

The public repository is [Garatech/jobsinton](https://github.com/Garatech/jobsinton). Initial documentation was published on 2026-10-17.

## Purpose

Help software professionals prioritize job opportunities that match their experience, skills, and working preferences. The project also serves as a professional portfolio: decisions, limitations, and verification results must be understandable and reproducible.

## Decisions and constraints

| Area | Decision | Status |
| --- | --- | --- |
| Product name | JobSinton | Agreed on 2026-10-17 |
| Portfolio | Gara's Workshop | Agreed |
| Professional focus | Java backend / full stack development | Agreed |
| Backend | Java and Spring Boot | Agreed; versions pending |
| Frontend | React and TypeScript | Agreed; versions pending |
| Database | PostgreSQL | Technical proposal; not yet finalized |
| Editor | Visual Studio Code | Agreed |
| Weekly commitment | 6–10 hours | Agreed |
| Working method | Build together, step by step, with ongoing documentation | Agreed |
| Budget | EUR 0; local development | Agreed |
| Initial market | Spain, with a model prepared for other European countries | Agreed |
| Hosting | Evaluate a free option once a demo exists | Pending |
| Project language | English for code and repository documentation | Agreed on 2026-10-17 |
| Documentation structure | Product README; separate working plan | Agreed on 2026-10-17 |
| Code license | No open-source license declared yet | Pending |

The name was chosen after a preliminary web search found no exact matches. Trademark registration, domains, and hosting identifiers have not been verified.

English applies to identifiers, comments, tests, developer-facing messages, documentation, commit messages, and issue/PR text. Our working conversation may remain in Spanish. Future localized interface text and original provider content may use other languages.

## Initial release scope

Target journey: **configure a profile → browse opportunities → understand the match → save an opportunity**.

- An editable professional profile with priorities and constraints.
- A clearly labeled fictional job catalog for development and demonstrations.
- One live provider, subject to access availability and usage terms.
- Search, filters, opportunity details, and explainable scores.
- Saved opportunities; application tracking follows in a later milestone.

Initially out of scope: automatic job applications, AI-based CV extraction, multiple simultaneous providers, microservices, and paid services. Public user accounts require verified data isolation; earlier demonstrations use fictional data.

## Profile configuration

Provide a short setup flow and optional advanced controls. Proposed dimensions:

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

**A match score is not a probability of being hired.** Do not infer professional suitability from personal characteristics unrelated to the role.

Pending design work:

- Define the formula and treatment of unknown values.
- Define skill equivalences, tie-breaking rules, and score explanations.
- Build a small manually reviewed reference set, including incompatible requirements, missing salary information, and changed preference weights.
- Keep the initial scoring engine free of AI dependencies.
- Consider AI-assisted extraction later only if it can be evaluated against reviewed examples.

## European scope

Keep company location, job location, and countries eligible for remote work separate. A remote listing may still restrict the employee's country of residence.

Preserve currency, pay period, and gross/net classification. Do not compare incompatible salary amounts directly. Prepare the interface for localization without promising coverage that the selected providers cannot deliver.

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

Use one repository for the frontend, backend, and documentation.

The backend will be a modular monolith organized around profiles, opportunities, connectors, matching, and applications. Connectors will translate provider responses into a common model so the matching engine does not depend on provider-specific formats.

Proposals still to finalize: PostgreSQL, versioned database migrations, a documented REST API, Docker Compose, and GitHub Actions. Pin versions after checking the development environment and compatibility.

## Roadmap

Milestones have exit criteria rather than fixed deadlines. Estimate them when they are broken down into small tasks.

| Milestone | Deliverable | Exit criteria | Status |
| --- | --- | --- | --- |
| 0. Definition | Name, scope, stack, and quality criteria | Main decisions recorded and open questions identified | In progress |
| 1. Foundation | Remote repository, reproducible environment, and automated checks | A clean clone starts using verified instructions | In progress: repository published; runtime setup pending |
| 2. First journey | Profile, fictional catalog, details, and saved opportunities | The journey works with loading, empty, and error states | Pending |
| 3. Matching v1 | Constraints, weights, and explanations | Reviewed cases cover mismatches, missing data, and changed weights | Pending |
| 4. Live provider | One connector and normalized listings | Authorized access; limits, duplicates, and failures handled | Pending |
| 5. Personal use | Persistence, application tracking, and accounts | User isolation and data deletion verified | Pending |
| 6. Demonstration | Refined interface and a viable zero-cost deployment | Main journey works accessibly on mobile and desktop with fictional data | Pending |
| 7. Portfolio presentation | Screenshots, documentation, and technical explanation | Reproducible demonstration and defensible decisions | Pending |

## Local development

The repository currently contains documentation and workspace configuration only. There are no application startup commands, dependencies, or application tests yet.

Open `JobSinton.code-workspace` in VS Code, or open the repository folder directly. Use `Ctrl+Shift+V` to preview Markdown.

Before implementing the first feature:

1. Check the installed versions and functionality of Java, Node, Git, and Docker.
2. Choose compatible runtime and framework versions.
3. Finalize the database choice.
4. Record exact installation, startup, and verification steps.
5. Build the smallest backend/frontend startup increment together.

No provider API keys are required for this stage.

## Development and evaluation

Each session follows: **small objective → explanation → joint implementation → verification → documentation**.

Each update records what changed, why, how it was checked, and what remains unresolved. Use an issue with observable acceptance criteria, a focused branch, and a pull request. Review correctness, clarity, relevant security risks, user experience, and the effect on data or integrations before merging.

Verification is proportional to the change: reference cases for matching, integration checks for persistence and connectors, and end-to-end checks for critical journeys. Documentation changes require accurate content and valid internal links. Do not claim tests or independent reviews that did not take place.

See [CONTRIBUTING](../CONTRIBUTING.md) for the detailed review process and [the pull request template](../.github/pull_request_template.md) for change records.

At the end of each milestone, explain the implementation, one important design decision, and an alternative considered. Revisit anything that is not understood before expanding the scope.

## Repository access and data

The repository is public. Direct changes and merges are reserved for `Garatech` and tools explicitly authorized by the owner. Others can work in forks and propose pull requests; submitting a PR does not integrate it or grant write access.

See the [repository access review](repository-access.md). GitHub settings enforce permissions; documentation only records their reviewed state.

Do not publish credentials, real CVs, personal demonstration profiles, or job listings we cannot redistribute. The code license remains undecided.

## Session log

### 2026-10-17 — Project foundation

- Chose JobSinton as the product name.
- Recorded the agreed technologies, weekly commitment, budget, and initial market.
- Prepared the README, contribution process, pull request template, and VS Code workspace.
- Published the initial documentation in `Garatech/jobsinton`.
- Reviewed repository permissions and required workflow approval for all external contributors.
- No application, CI pipeline, deployment, or live integrations were implemented.
- Next technical step: inspect the development environment and build the first runnable increment together.

### 2026-10-17 — English documentation and product README

- Adopted English for code and repository documentation.
- Separated the concise product introduction from this working plan.
- Preserved the detailed scope, provider notes, roadmap, review process, and session history.
- Translated the contribution guide, access review, pull request template, and configuration comments.
- Local terminal access was unavailable during this documentation update because the execution environment could not initialize its sandbox. Remote changes can be completed through GitHub; refresh the local checkout before the next coding session.
