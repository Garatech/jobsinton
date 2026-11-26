# JobSinton — Product Scope, Architectural Decisions & Roadmap

This document summarizes **JobSinton**'s product goals, engineering constraints, key architectural decisions, and milestone progression.

---

## 1. Product Vision & Scope

**JobSinton** helps software engineers—particularly early-career developers, recent graduates, and full-stack/backend/data/cloud specialists in **Spain and Europe**—discover genuine, fresh job openings directly from employer career portals and evaluate how well each role fits their working preferences.

### Core Principles

1. **Zero-Friction, EUR 0 Operation:** Runs locally or in Docker with zero API keys, zero paid subscriptions, and zero external database setup.
2. **Direct Source Provenance:** Queries official public company Applicant Tracking System (ATS) endpoints rather than third-party job boards, preserving original job descriptions, real timestamps, and direct `https://` employer application links.
3. **Honest Compatibility Semantics:** Matching and ranking express alignment with a candidate's stated profile—never a black-box "probability of getting hired." Hard constraints, advisory preferences, and missing facts remain explicitly separated.
4. **Local-First Candidate Privacy:** Candidate profiles and application triage state (`⭐ Save`, `✅ Applied`, `✕ Dismiss`) stay in browser `localStorage` without user accounts or server-side tracking.

---

## 2. Key Architectural Decisions

| Area | Decision | Rationale |
| --- | --- | --- |
| **Backend Runtime** | Java 21 LTS + Spring Boot 4.0.8 | Virtual threads allow high-concurrency I/O fan-out across 334+ upstream ATS boards with simple, readable blocking code bounded by a `Semaphore`. |
| **Live Job Ingestion** | Direct 9-ATS Provider Engine ([ATS_PROVIDERS.md](ATS_PROVIDERS.md)) | Replaced early key-gated aggregator prototypes (which returned truncated excerpts and required credentials) with 9 unauthenticated public ATS adapters across 334 verified Spain & Europe company boards. |
| **Cold-Start & Refresh Latency** | Disk Snapshot (`AtsFeedCacheStore`) + Stale-While-Revalidate (`AtsFeedService`) | Restores cached openings in `< 50ms` on startup, pre-warms asynchronously on `ApplicationReadyEvent`, and revalidates expired feeds in the background without dropping a single vacancy. |
| **Freshness Integrity** | Local `FirstSeenRegistry` (`data/ats-seen-jobs.json`) | Computes age from `min(publishedAt, firstSeenAt)` so recruiter edits to older postings (`updated_at`) never falsely trigger `< 24h` (`🔥`) freshness badges. |
| **Core Matching Domain** | Framework-free `matching` & `ranking` Java packages ([MATCHING.md](MATCHING.md), [RANKING.md](RANKING.md)) | Pure Java records and deterministic fraction math keep rule verification fast, reproducible, and decoupled from Spring/HTTP transport. |
| **Frontend Stack** | React 19 + strict TypeScript + Vite ([DESIGN.md](DESIGN.md)) | Lightweight bundle with runtime JSON validation, `AbortController` race-condition protection, bilingual `ES`/`EN` support, and custom token-boundary keyword scoring. |
| **Containerization** | Multi-stage Docker Compose ([../INSTRUCTIONS.md](../INSTRUCTIONS.md)) | Non-root `eclipse-temurin:21-jre-alpine` + `nginx:1.27-alpine` with a named `jobsinton-data` volume lets anyone boot the entire platform with `docker compose up --build -d`. |
| **License** | MIT License (`Garatech`) | Permissive open-source licensing suitable for public inspection, portfolio review, and community reuse. |

---

## 3. Completed Milestones

| Milestone | Scope Delivered | Verification |
| --- | --- | --- |
| **M1 · Runnable Foundation** | Spring Boot `/api/status` contract, React shell, `AbortController` timeout/retry handling, and GitHub Actions CI | Backend MockMvc + Playwright desktop/mobile E2E |
| **M2 · Local Profile & *Current* UI** | Versioned `localStorage` profile editor, *Current* visual design system, responsive `320px`–`1440px` layouts, and bilingual `ES`/`EN` localization | Storage corruption recovery + viewport & locale specs |
| **M3 · Explainable Matching & Ranking** | Pure Java `MatchingEngine` (M01–M24 reference cases), `CompatibilityRanker` (R01–R14 exact-fraction ordering), and `/api/matching/*` endpoints | 90+ domain & API integration tests + browser race-guard specs |
| **M4 · Multi-ATS Direct Engine & Auto-Discovery** | 9 ATS adapters (`Greenhouse`, `Lever`, `SmartRecruiters`, `Workable`, `Ashby`, `Personio`, `Teamtailor`, `Recruitee`, `Factorial HR`), URL Auto-Sniffer, parallel slug prober, and `FirstSeenRegistry` | Deterministic adapter unit tests & stubbed E2E journeys |
| **M5 · Multi-Discipline Tracks, Dockerization & Spain Junior Tuning** | 8 developer stack presets, 14 profile roles, 1-command Docker Compose, 3-tier Spain/EU geo filter + anti-fake-remote engine, SWR disk cache, SmartRecruiters `?country=es` pass, and expansion to **334 verified boards** | **136 backend tests + 148 Playwright E2E tests** passing offline in CI |
| **M6 · Portfolio & Documentation Polish** | Flagship `README.md` with Retina product tour and Mermaid architecture diagram, MIT `LICENSE`, cleaned `docs/` handbook, and aligned contributor guide | Full link integrity & CI suite verification |

---

## 4. Future Exploration Ideas

- **Exportable Triage & Application Notes:** Optional JSON/CSV export and import of saved/applied ATS roles across browsers without requiring server accounts.
- **Custom Feed Schedules & Notifications:** Optional local desktop/webhook digest for newly sighted `< 24h` Spain junior roles matching the saved profile.
- **Additional Regional European ATS Adapters:** Support for custom Workday/SuccessFactors JSON feeds where publicly exposed by Spanish and European tech employers.
