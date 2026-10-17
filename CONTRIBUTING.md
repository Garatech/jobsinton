# Contributing to JobSinton

Build small, understandable, and verifiable changes. The initial commits establish the documentation baseline; subsequent increments follow the issue and pull request workflow.

## Language and documentation

Use English for code identifiers, comments, tests, developer-facing messages, repository documentation, commit messages, and issue/PR text. Localized interface copy and original provider content may use their respective languages.

Keep the [README](README.md) concise and product-focused. Maintain scope, decisions, milestones, and session notes in [the project plan](docs/PROJECT_PLAN.md). Our working conversation may remain in Spanish.

## External contributions

This public repository is owned by `Garatech`. External contributors should work in a fork and open a pull request. Opening a PR does not grant write access or guarantee acceptance. The owner decides what to merge; automatic merging is disabled.

Authorized tools may act on the owner's behalf. Accepting a contribution does not automatically add its author as a collaborator.

## Change workflow

1. Define an issue with the problem, scope, and observable acceptance criteria.
2. Create a focused branch, such as `feat/profile-preferences`, `fix/missing-salary`, or `docs/local-setup`.
3. Explain the design and build the solution together.
4. Run relevant checks and record their actual results.
5. Open a PR explaining the motivation, changes, evidence, and limitations.
6. Review the change and resolve blocking findings.
7. Merge and verify the resulting state of `main`.

Use descriptive commit messages with prefixes such as `feat`, `fix`, `docs`, `test`, and `chore`. Branch protection and automated checks have not been configured yet.

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
- Documentation reflects the actual state of the project.
- Sensitive data or configuration changes include a recovery plan.
- Review authorship and AI assistance are stated honestly; self-review is not presented as independent human review.

## Session close

Record the objective, decisions, relevant files, checks, limitations, and next step in the project plan. Explain an important decision and an alternative considered. Revisit anything that is not understood before increasing the scope.

## Data and external services

Use fictional data in tests and demonstrations. Keep credentials outside version control. Review provider access, quotas, attribution, and retention before integration. The agreed budget is EUR 0; discuss any proposed paid dependency with the project owner.
