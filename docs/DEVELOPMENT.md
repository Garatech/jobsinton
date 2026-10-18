# Development guide

I use GitHub as the source of truth for shared code and history. A commit on a computer or in a Codespace is not backed up to GitHub until it is pushed.

## What works today

The React interface checks a real Java service through `GET /api/status`. It displays loading, connected, and unavailable states, allows retries, and stops waiting after five seconds. There is no profile, matching engine, job catalog, database, or live provider yet.

The services are development processes. This guide does not deploy a public application.

## Choose an environment

### Browser development with GitHub Codespaces (optional)

Codespaces gives me access to the same development environment from a desktop and a laptop.

1. In the repository, select the branch you want to work on.
2. Open **Code → Codespaces** and create an environment on that branch, or reopen your existing one.
3. The dev container installs Java 21, Maven 3.9.11, Node 24, and the locked frontend dependencies.
4. Open two terminals and run the startup commands below.
5. Open the forwarded frontend port **5173**. Keep forwarded ports **private**; the frontend proxies API requests internally to port 8080.
6. Commit and push work before finishing. Stop the Codespace when you are done and reopen the same one from your other computer.

Codespaces is optional and has limited included usage depending on your GitHub plan. Check remaining usage and billing settings before creating it. My budget for this project is EUR 0, so paid overages must stay disabled. This update does not create a Codespace or change billing settings. If the included quota is unavailable, use local development.

The dev container configuration is provided but has not yet been started and verified in Codespaces. Its first startup remains a milestone acceptance check.

### Local development on either computer

Install Git, VS Code, a Java 21 JDK, Maven 3.9.x, and Node.js 24 (including npm). Java 21 must be the JDK used by Maven. Docker is not required for the local workflow.

Check the tools:

```sh
git --version
java --version
mvn --version
node --version
npm --version
```

For a **new** checkout:

```sh
git clone https://github.com/Garatech/jobsinton.git
cd jobsinton
code JobSinton.code-workspace
npm --prefix frontend ci
```

An existing checkout must be inspected and synchronized using the next section; do not clone over it. Before the foundation PR is merged, check out `feat/runnable-foundation` to try this increment.

## Start the application

From the repository root, in terminal 1:

```sh
mvn -f backend/pom.xml spring-boot:run
```

From the repository root, in terminal 2:

```sh
npm --prefix frontend run dev
```

Locally, open [http://localhost:5173](http://localhost:5173). In Codespaces, open the forwarded **5173** URL instead. The interface should report **Connected to the JobSinton API**.

The API itself is available locally at [http://localhost:8080/api/status](http://localhost:8080/api/status) and responds:

```json
{"name":"JobSinton","status":"UP"}
```

Stop either process with `Ctrl+C`. To exercise recovery, stop the backend, click **Check connection again**, restart the backend, and retry.

No environment variables, API keys, real CVs, or database are needed.

## Move safely between devices

When moving between devices, I work on one shared branch at a time. I keep source files in Git; `node_modules`, build output, and the Git directory must not be synchronized through a general-purpose cloud folder.

Before changing machines, I inspect my changes, stage only intended files, commit, and push:

```sh
git status --short --branch
git diff
git add <intended-file-paths>
git commit -m "feat: describe the completed increment"
git push -u origin <your-branch>
```

Replace angle-bracket placeholders with your actual paths and branch. Temporary work can be committed on your feature branch with a clear work-in-progress message; do not merge incomplete work into `main`.

On the other machine:

1. Run `git status --short --branch` and `git remote -v`. Confirm that `origin` points to `Garatech/jobsinton`.
2. If there are uncommitted changes, review and commit them on their existing work branch before switching. Do not discard or overwrite them.
3. Run `git fetch origin --prune`.
4. Switch to the shared branch with `git switch <your-branch>`. If it is not present locally, use `git switch --track origin/<your-branch>`.
5. Run `git pull --ff-only`. If Git reports divergent history, stop and review the commits before proceeding; do not force push or reset.
6. Run `npm --prefix frontend ci` after dependency changes.

After a PR has been merged, update a **clean** checkout:

```sh
git switch main
git pull --ff-only
```

Create the next feature branch from this updated `main`.

## Verify an increment

From the repository root:

```sh
mvn -B -ntp -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run build
```

For browser tests, install Chromium once from the frontend directory:

```sh
cd frontend
npx playwright install chromium
npm run test:e2e
```

On a Linux machine missing browser system libraries, use `npx playwright install --with-deps chromium`. Browser tests start the API and frontend automatically. Locally they may reuse services already running on ports 8080 and 5173; stop existing servers to ensure a fresh startup.

GitHub Actions runs the backend HTTP tests, TypeScript checking, frontend build, and Playwright tests on desktop and mobile Chromium. Its job token is read-only, checkout credentials are not persisted, and failure reports are retained for seven days. A green run is evidence for that commit; it is not a production-readiness claim.

I check the workflow linked from the PR before merging. Branch protection has not yet been enabled, so passing checks are currently a review rule rather than a GitHub-enforced merge gate.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| API unavailable | Start Java, inspect its terminal, confirm port 8080 is free, then retry |
| Frontend port already used | Stop the previous frontend process; Vite intentionally fails rather than silently selecting another port |
| Java compilation/version error | Check `mvn --version`; it must show Java 21 |
| Node engine warning | Use Node 24, then reinstall with `npm ci` |
| Dependencies differ between computers | Pull the branch and use `npm ci`; do not copy dependency directories |
| Codespaces page cannot reach API | Open frontend port 5173; requests must use relative `/api` URLs through Vite |
| Pull cannot fast-forward | Preserve both sets of commits and inspect history before deciding how to integrate them |

## Verification limits

The first implementation is verified in GitHub Actions when its run passes. My Windows checkout and first Codespaces startup still need separate verification. During the 2026-10-18 session, the assistant's local terminal could not initialize its sandbox, so local changes, installed versions, and checkout cleanliness remain unverified.

## Platform references

- [Codespaces included usage and billing](https://docs.github.com/en/billing/concepts/product-billing/github-codespaces)
- [Codespaces default environment variables](https://docs.github.com/en/codespaces/developing-in-a-codespace/default-environment-variables-for-your-codespace)
- [Codespaces port forwarding](https://docs.github.com/en/codespaces/developing-in-a-codespace/forwarding-ports-in-your-codespace)
- [Vite host allowlists](https://vite.dev/config/server-options#server-allowedhosts)
