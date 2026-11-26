# Application Architecture

## Request Topologies

JobSinton runs identically across both containerized production builds and local development without broad CORS policies:

- **Docker Compose (`http://localhost:5173`):** Browser → `nginx:1.27-alpine` (serves static Vite bundle and proxies `/api/*` to `http://backend:8080/api/`) → Spring Boot 4 (`eclipse-temurin:21-jre-alpine`).
- **Local Development (`http://localhost:5173`):** Browser → Vite dev proxy (`/api/status`, `/api/matching`, `/api/ats` forwarded to `http://127.0.0.1:8080`) → Spring Boot 4.

---

## Module Responsibilities

| Location | Responsibility |
| --- | --- |
| `backend/.../JobSintonApplication.java` | Spring Boot entry point |
| `backend/.../system/SystemStatusController.java` | Lightweight `GET /api/status` health contract |
| `backend/.../providers/ats/` | Multi-ATS direct provider & auto-discovery engine: 9 public ATS adapters (Personio, Teamtailor, Recruitee, Factorial HR, Greenhouse, Lever, SmartRecruiters, Workable, Ashby), Europe-eligibility ingestion filter, 334-company Spain/EU registry + `data/custom-companies.json`, `AtsFeedCacheStore` (`data/ats-feed-cache.json`) with startup pre-warming and Stale-While-Revalidate, `FirstSeenRegistry` (`data/ats-seen-jobs.json`), bounded virtual-thread feed service, and `/api/ats/*` REST API |
| `backend/src/main/resources/ats/companies.json` | Pre-seeded registry of 334 verified public company ATS boards across Spain, European tech hubs, and EU Remote employers |
| `backend/.../matching/` | Pure Java matching domain (`MatchingEngine`), immutable facts, validation, and versioned explanations with zero framework dependencies |
| `backend/.../matching/api/` | Bounded matching & batch ranking HTTP controllers, 14-role saved-profile validation (`SavedProfileAdapter`), and catalog fact lookup |
| `backend/.../ranking/` | Pure Java compatibility ranker (`CompatibilityRanker`) ordering filtered sets by conflict tier, exact preference fractions, and evidence coverage |
| `frontend/src/api/` | Runtime-validated HTTP clients for `/api/ats/*`, `/api/matching/*`, and `/api/status` with `AbortController` deadlines |
| `frontend/src/ats/` | 8 multi-discipline developer stack presets, 3-Tier Spain/EU Geo Filter, Anti-Fake-Remote Blocker, Junior/Seniority & freshness scoring, and bilingual token-boundary keyword matcher |
| `frontend/src/pages/` | Home (with **Live ATS Radar**), Live ATS / Sample Opportunities board, Companies, Opportunity Detail, Saved shortlist, and Service Status |
| `frontend/src/profile/` | 14-role professional profile schema, input normalization, validation, guarded `localStorage` persistence, and `ProfileEditor` |
| `frontend/src/matching/Comparison.tsx` | Saved-profile compatibility lifecycle and explainable criteria breakdown |
| `frontend/src/i18n/` | Bilingual (`Español` / `English`) message catalogs and locale detection |
| `frontend/src/styles.css` | Responsive *Current* design system (`320px`–`1440px`) without external CSS frameworks |
| `frontend/tests/` | 148 Playwright desktop & mobile Chromium E2E journeys and deterministic unit specs |
| `docker-compose.yml` | Two-container orchestration (`frontend` + `backend`) with healthcheck-gated startup and persistent `jobsinton-data` volume |
| `.github/workflows/ci.yml` | Automated offline backend (`mvn verify`) and frontend/Playwright (`npm run build` + `test:e2e`) CI pipeline |

---

## Concurrency, Caching & Resilience

### 1. Virtual-Thread Fan-Out & Fault Isolation (`AtsFeedService`)
Live ATS aggregation queries all 334+ configured boards concurrently on **Java 21 Virtual Threads**, bounded by a `Semaphore` (`jobsinton.ats.max-concurrency=24`), an 8-second per-request timeout, and a 12 MiB payload cap. If an individual board times out or returns HTTP 4xx/5xx or malformed JSON/XML, the error is isolated to that board's diagnostic entry (`ok: false`) while all remaining boards succeed.

### 2. Disk Snapshot & Stale-While-Revalidate (`AtsFeedCacheStore`)
To eliminate cold-start latency without sacrificing a single job offer:
- On startup, `AtsFeedService` loads the last persisted feed from `data/ats-feed-cache.json` in `< 50ms` and triggers an asynchronous background pre-warm (`ApplicationReadyEvent`).
- When the 10-minute cache TTL expires, `GET /api/ats/jobs` returns the cached feed immediately (`cached: true`, `refreshing: true`) and launches a single coalesced background refresh on virtual threads. The React client renders immediately and seamlessly merges newly discovered openings once revalidation finishes.
- Explicit `Force Refresh` (`?refresh=true`) performs a synchronous live refresh, falling back to the cached snapshot if the machine is temporarily offline.

### 3. Client Async & Race-Condition Safety
Every frontend HTTP call owns an `AbortController` and request-generation guard. Navigating away, changing filters, or saving a new profile cancels in-flight requests so superseded responses can never overwrite current UI state. Every external `applyUrl` is validated on both server and client to require `https://`.

---

## Pure Matching & Ranking Domain

The `matching` and `ranking` packages implement the versioned contracts in [MATCHING.md](MATCHING.md) and [RANKING.md](RANKING.md) using immutable Java records and enums:
- **Zero framework coupling:** No Spring annotations, HTTP types, system clocks, databases, or ATS-specific DTOs enter the domain packages.
- **Honest semantics:** Hard constraint conflicts (`RESIDENCE`, `ONSITE_DAYS`, `WORK_MODE` incompatibilities), advisory matches, partial overlaps, and unknown facts remain distinct. Missing information is never penalized as a conflict or inflated into a match.
- **Transient profile privacy:** Profiles live in browser `localStorage` (`jobsinton.professional-profile`); when evaluating compatibility, the snapshot is posted transiently in memory and never persisted or logged on the server.
