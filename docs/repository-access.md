# Initial Repository Access Review

Review date: 2026-10-17. Repository: [Garatech/jobsinton](https://github.com/Garatech/jobsinton).

This is a historical record of the initial settings review. Translating this document does not change GitHub settings.

## Goal

Keep the project publicly visible while reserving changes to the original repository for the owner and their authorized tools. External contributions are proposed through forks and merged only at the owner's discretion.

## Reviewed configuration

| Control | Observed or applied state |
| --- | --- |
| Visibility | Public |
| Owner | Garatech, personal account |
| Collaborators | 0; GitHub indicates that only the owner can contribute directly |
| Deploy keys | None |
| Installed GitHub Apps | ChatGPT Codex Connector; existing owner-authorized integration |
| Automatic PR merging | Disabled |
| Wiki editing | Restricted to collaborators; with no collaborators, to the owner |
| Default GITHUB_TOKEN permissions | Read repository contents and packages |
| GitHub Actions creating or approving PRs | Disabled |
| Fork PR workflow execution | Changed from approval for first-time contributors to approval for all external contributors |

Issues and pull requests remain open to external proposals. This does not grant permission to write to branches or merge changes.

## Scope and limitations

- This review covers only the repository controls listed above. It does not audit account sessions, personal tokens, local credentials, or account security.
- The authorized connector can act within the permissions granted by the owner. Its access was neither expanded nor revoked.
- Default Actions permissions are not an absolute limit: workflows can declare additional permissions. Review future automation files before merging them.
- No branch protection rule was added during this review. Having no collaborators already prevents strangers from pushing or merging. Protections against maintainer mistakes and required checks will be designed alongside the first CI workflow.
- There were no application workflows or automated tests at the time of this review.

## References

- [Permission levels for personal repositories](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/permission-levels-for-a-personal-account-repository)
- [Forks and permissions](https://docs.github.com/en/pull-requests/reference/forks)
