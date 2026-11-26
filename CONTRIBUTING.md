# Contributing to JobSinton

Thank you for your interest in contributing to **JobSinton**! This repository is developed through focused, well-tested increments that keep the domain logic clean, observable, and easy to verify offline.

---

## Development Setup

### Prerequisites

- **Java 21** (Eclipse Temurin or equivalent JDK)
- **Maven 3.9+**
- **Node.js 24** (including `npm`)
- **Docker & Docker Compose** *(optional, for single-command containerized execution)*

See [INSTRUCTIONS.md](INSTRUCTIONS.md) for quick Docker startup or [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for native multi-terminal development.

---

## Architectural Guardrails

When proposing changes, please preserve these core boundaries:

1. **Zero-Config & EUR 0 Runtime:** The application must boot and aggregate live openings out of the box without API keys, `.env` secrets, or paid third-party services.
2. **Pure Matching & Ranking Domain:** `io.github.garatech.jobsinton.matching` and `io.github.garatech.jobsinton.ranking` remain pure Java (`record`/`enum`) modules with zero Spring, HTTP, clock, or provider coupling.
3. **Fault Isolation & Safe External Data:** Every external ATS adapter in `providers/ats/` must enforce bounded timeouts, payload caps, strict `https://` URL validation, and board-level fault isolation so a failing upstream endpoint never degrades the rest of the feed.
4. **Offline Deterministic Verification:** Automated unit, integration, and Playwright end-to-end tests must never depend on live third-party servers in CI.

---

## Running the Full Verification Suite

Before opening a pull request, stop any running local dev servers (`5173` / `8080`) and run the complete verification pipeline from the repository root:

```bash
# 1. Backend unit, adapter & Spring MockMvc integration tests (JUnit 5)
mvn -B -ntp -f backend/pom.xml verify

# 2. Frontend strict TypeScript check & production Vite build
npm --prefix frontend ci
npm --prefix frontend run build

# 3. Playwright desktop & mobile Chromium E2E + ATS scoring suite
npm --prefix frontend run test:e2e
```

---

## Pull Request Workflow

1. Create a focused feature branch from `main` using conventional prefixes (`feat/...`, `fix/...`, `docs/...`, `test/...`, `chore/...`).
2. Keep commits atomic with descriptive conventional commit messages (e.g., `feat(ats): ...`, `fix(matching): ...`, `docs: ...`).
3. Update relevant technical documentation under [docs/](docs/README.md) and add a concise note under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md) when user-facing or architectural behavior changes.
4. Open a Pull Request against `main` summarizing the problem, design trade-offs, and test verification results. All GitHub Actions checks (**Build and integration tests**) must pass before merge.

---

## Security & Data Hygiene

- Never commit API tokens, personal CVs, real candidate data, or runtime cache files (`data/*.json`).
- Use deterministic synthetic fixtures in automated tests and documentation examples.
