# Changelog

This is a record of notable changes to JobSinton: their purpose, behavior, verification, and known limits. Unreleased work becomes part of `main` only after its pull request is merged.

## [Unreleased]

### Design preview — Current preversion (2026-10-30)

- Added a [complete static preview](design-preversion/README.md) on `design/current-preversion`, an exploratory branch that will not be merged.
- Current remains the foundation; Ledger and Atlas inform search layouts. Folio and Nocturne are excluded from the new proposal.
- Fourteen connected pages include fictional offers, company profiles, search, saved opportunities and an editable example profile.
- Review steps and verification scope are recorded alongside the preview. Production application code is unchanged.

### Added — Visual design studies (2026-10-29)

**Purpose:** compare visual directions before extending the product frontend. Tracked in issue #11.

- A source-based Current reference and four independent static alternatives: Folio, Ledger, Atlas and Nocturne.
- Comparable fictional catalog, details, saved-opportunity and profile interactions with session-only state.
- An offline comparison page and downloadable notes for selecting elements across versions.
- [Research and sources](design-exploration/RESEARCH.md), a [review guide](design-exploration/REVIEW.md), and [verification scope and limits](design-exploration/VERIFICATION.md).
- Browser checks for five designs across four widths, with a ZIP package produced only after successful verification.
- Corrected Nocturne's narrow layout, Current's reference styling and focus recovery after removing a saved item.
- The design workflow runs on relevant branch pushes and pull requests. Review the PR checks for the tested revision and result.
- This study does not select or apply a redesign to the production application. The owner reviews the alternatives before the next frontend increment.

### Added — Professional profile (2026-10-20)

**Purpose:** turn professional goals and working preferences into an editable, browser-local profile. Tracked in issue #9.

- Target role, add/remove technologies, whole years of experience, country, city, work mode, and conditional on-site day limits.
- Explicit save, reload recovery, last-saved summary, unsaved-change feedback, and confirmed clear.
- Shared validation for form input and versioned saved data; keyboard-accessible errors and responsive controls.
- Storage failure handling that preserves edits and never reports a false save.
- Automated validation and desktop/mobile browser coverage for the main journeys and failure cases.
- [Profile verification guide](docs/PROFILE.md) with commands and a post-merge acceptance checklist.

### Updated

- README, architecture, and project plan reflect the profile increment.
- Development instructions clarify local-first setup, Node 24, Java 21, stopping servers before reinstalling dependencies, Windows file locks, and stale VS Code environments.
- Recorded owner-reported successful Windows foundation verification on 2026-10-20.

### Profile verification and limits

- Implementation commit `25d971b` passed [GitHub Actions run 39445633841](https://github.com/Garatech/jobsinton/actions/runs/39445633841): 2 backend HTTP tests, locked dependency installation, TypeScript checking, production build, and 40 Playwright executions (30 browser journeys plus 5 validation cases repeated in both project configurations). See [PR #10](https://github.com/Garatech/jobsinton/pull/10) for the final checked revision.
- The profile is stored only in this browser and origin; there is no device synchronization, account, backend profile endpoint, or database.
- Unsaved edits are lost on reload. Clearing browser data removes the profile; multiple tabs use last-save-wins behavior.
- Automated viewport and keyboard checks do not replace human visual acceptance. The owner has reported completing the profile manual checklist successfully, including browser-local persistence.
- The development terminal is unavailable; remote changes do not automatically update the local checkout.

### Documentation update — 2026-10-18

- Opened `docs/documentation-update` for a wording update across the README, development notes, project plan, contribution guide, access review, and PR template.
- Refined the wording in `docs/editorial-refinement` for a consistent, project-focused tone.
- Preserved the technical scope, instructions, and verification records; no application code or configuration changes.

### Added — Runnable foundation (2026-10-18)

**Purpose:** establish a small, testable connection between the Java and React stacks, with a shared workflow for desktop, laptop, and optional browser development. Tracked in issue #3.

- Java 21 / Spring Boot service with `GET /api/status`, a deliberately narrow connectivity contract.
- React / TypeScript introduction with live connection status, loading feedback, bounded wait, failure handling, and manual retry.
- Responsive layout, keyboard skip link, visible focus, and clearly labeled planned capabilities.
- Backend HTTP integration tests and browser checks for a real connection, outage recovery, invalid responses, and timeouts.
- GitHub Actions build/test workflow with a read-only token and pinned action revisions.
- Optional dev container and a development guide covering startup, verification, and safe changes between devices.
- Architecture notes explaining the request path, runtime validation, cancellation, and deliberately limited scope.
- This changelog as a required record for subsequent updates.

### Changed

- README reflects the runnable foundation while keeping future job-discovery capabilities separate.
- Project plan records the implementation increment and remaining environment acceptance checks.
- Contribution guide and PR template require changelog updates and actual verification evidence.

### Verification and limits

- Initial cloud verification passed: 2 backend HTTP tests, strict TypeScript checking, a frontend production build, and 8 browser checks across desktop/mobile Chromium ([run 39141434566](https://github.com/Garatech/jobsinton/actions/runs/39141434566)).
- Committed the generated npm lockfile, removed the one-time bootstrap step, and updated GitHub Actions from deprecated runtime versions to current pinned releases. The final PR check verifies these changes using only locked installs.
- Review [PR #4](https://github.com/Garatech/jobsinton/pull/4) for the final checked commit and validation result.
- The local Windows checkout could not be inspected in this session because the development terminal failed to initialize its sandbox. Local synchronization and local test results remain unverified.
- Codespaces configuration has not yet been exercised; no cloud environment or paid resource was created.
- There is no database, profile editor, job search, matching, live provider, or production deployment yet.
- No independent human review or full accessibility audit has been performed.

## Documentation baseline — 2026-10-17

- Established the public repository, VS Code workspace, review process, and access-review notes.
- Separated the product README from the working project plan in PR #2, subsequently merged by the owner.
