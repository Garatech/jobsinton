# Changelog

All notable changes to JobSinton are recorded here in English. Entries describe intent, behavior, verification, and known limits. Unreleased work becomes part of `main` only after its pull request is merged.

## [Unreleased]

### Added — Runnable foundation (2026-10-18)

**Purpose:** establish a small, testable connection between the agreed Java and React stacks, with a shared workflow for desktop, laptop, and optional browser development. Tracked in issue #3.

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
- The local Windows checkout could not be inspected in this session because the assistant terminal failed to initialize its sandbox. No claim of local synchronization or local test success is made.
- Codespaces configuration has not yet been exercised; no cloud environment or paid resource was created.
- There is no database, profile editor, job search, matching, live provider, or production deployment yet.
- No independent human review or full accessibility audit has been performed.

## Documentation baseline — 2026-10-17

- Established the public repository, VS Code workspace, review process, and access-review notes.
- Adopted English for repository content.
- Separated the product README from the working project plan in PR #2, subsequently merged by the owner.
