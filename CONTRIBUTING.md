# Contributing to JobSinton

I keep changes small enough to understand and verify. The initial commits establish the documentation baseline; subsequent increments follow the issue and pull request workflow.

## Language and documentation

I use English for code identifiers, comments, tests, developer-facing messages, repository documentation, commit messages, and issue/PR text. Localized interface copy and original provider content may use their respective languages.

I keep the [README](README.md) concise and product-focused, with scope, decisions, milestones, and session notes in [the project plan](docs/PROJECT_PLAN.md). Every implementation increment belongs in [CHANGELOG.md](CHANGELOG.md): purpose, behavior, verification, and limitations. I document non-obvious contracts and decisions alongside the code. Development discussions may remain in Spanish.

## External contributions

I maintain this public repository as `Garatech`. To propose a contribution, work in a fork and open a pull request. Opening a PR does not grant write access or guarantee acceptance. I decide what to merge; automatic merging is disabled.

Tools I authorize may act on my behalf. Accepting a contribution does not automatically add its author as a collaborator.

## Change workflow

This is the process I follow for each increment:

1. Define an issue with the problem, scope, and observable acceptance criteria.
2. Create a focused branch, such as `feat/profile-preferences`, `fix/missing-salary`, or `docs/local-setup`.
3. Work through the design and implementation together.
4. Run relevant checks and record their actual results.
5. Open a PR explaining the motivation, changes, evidence, and limitations.
6. Review the change and resolve blocking findings.
7. Merge and verify the resulting state of `main`.

Use descriptive commit messages with prefixes such as `feat`, `fix`, `docs`, `test`, and `chore`. GitHub Actions runs build and integration checks. Branch protection has not been configured yet; I check the results before merging.

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

I use tests to protect behavior and concrete risks rather than repeat implementation details, without imposing an arbitrary coverage percentage. Matching-rule changes are evaluated against a stable set of reviewed examples.

## Merge criteria

- Acceptance criteria are satisfied.
- Applicable build, static analysis, and tests pass once configured.
- No credentials, personal test data, or known unresolved regressions are introduced.
- Domain logic is understandable and separate from provider-specific formats.
- Relevant invalid inputs, failures, and incomplete data are handled.
- Documentation and CHANGELOG.md reflect the actual state of the project.
- Sensitive data or configuration changes include a recovery plan.
- Review authorship and AI assistance are stated honestly; self-review is not presented as independent human review.

Use [the development guide](docs/DEVELOPMENT.md) to synchronize work safely between devices. Push work branches before switching machines.

## Session close

At the end of a session, I record the objective, decisions, relevant files, checks, limitations, and next step in the project plan. I explain an important decision and an alternative I considered, and revisit anything I do not yet understand before increasing the scope.

## Data and external services

I use fictional data in tests and demonstrations and keep credentials outside version control. Before integrating a provider, I review access, quotas, attribution, and retention. My budget is EUR 0; discuss any proposed paid dependency with me first.
