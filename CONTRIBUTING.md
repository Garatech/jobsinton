# Contributing to JobSinton

JobSinton develops through small, focused changes that can be understood and verified. The initial commits establish the documentation baseline; subsequent increments follow the issue and pull request workflow.

## Documentation

The [README](README.md) provides a concise product introduction. Scope, decisions, milestones, and session notes belong in [the project plan](docs/PROJECT_PLAN.md). [CHANGELOG.md](CHANGELOG.md) records each implementation increment: purpose, behavior, verification, and limitations. Non-obvious contracts and design decisions are documented alongside the code.

## External contributions

This public repository is owned by `Garatech`. External contributors should work in a fork and open a pull request. Opening a PR does not grant write access or guarantee acceptance. The owner decides what to merge; automatic merging is disabled.

Authorized tools may act on the owner's behalf. Accepting a contribution does not automatically add its author as a collaborator.

## Change workflow

1. Define an issue with the problem, scope, and observable acceptance criteria.
2. Create a focused branch, such as `feat/profile-preferences`, `fix/missing-salary`, or `docs/local-setup`.
3. Define the design and implement the change.
4. Run relevant checks and record their actual results.
5. Open a PR explaining the motivation, changes, evidence, and limitations.
6. Review the change and resolve blocking findings.
7. Merge and verify the resulting state of `main`.

Use descriptive commit messages with prefixes such as `feat`, `fix`, `docs`, `test`, and `chore`. GitHub Actions runs build and integration checks. Branch protection has not been configured yet; the owner must check the results before merging.

## Verification by risk

| Change | Expected evidence |
| --- | --- |
| Documentation | Accurate content, valid internal links, and current instructions |
| Interface | Affected journey, keyboard use, relevant screen sizes, and empty/error states |
| Matching | Reference cases, constraints, missing data, weights, and explanations |
| Persistence | Database integration and migration impact |
| Connector | Contract, pagination, failures, timeouts, and limits without requiring credentials in CI |
| Accounts | Authentication, authorization, and isolation between two users |
| Critical journey | An end-to-end check of the affected behavior |

Tests should protect behavior and concrete risks rather than repeat implementation details. Do not impose an arbitrary coverage percentage. Evaluate matching-rule changes against a stable set of reviewed examples.

## Merge criteria

- Acceptance criteria are satisfied.
- Applicable build, static analysis, and tests pass once configured.
- No credentials, personal test data, or known unresolved regressions are introduced.
- Domain logic is understandable and separate from provider-specific formats.
- Relevant invalid inputs, failures, and incomplete data are handled.
- Documentation and CHANGELOG.md reflect the actual state of the project.
- Sensitive data or configuration changes include a recovery plan.
- Review records identify the reviewer; self-review is not presented as independent human review.

Use [the development guide](docs/DEVELOPMENT.md) to synchronize work safely between devices. Push work branches before switching machines.

## Session close

Session notes cover the objective, decisions, relevant files, checks, limitations, and next step, including an important design decision and an alternative considered. Unresolved questions are revisited before the scope expands.

## Data and external services

Use fictional data in tests and demonstrations. Keep credentials outside version control. Review provider access, quotas, attribution, and retention before integration. The agreed budget is EUR 0; discuss any proposed paid dependency with the project owner.
