# JobSinton Technical Documentation

This directory contains the architectural specifications, ATS adapter contracts, domain rules, and verification guides for **JobSinton**.

---

## Overview & Operations

| Document | Purpose |
| --- | --- |
| **[../INSTRUCTIONS.md](../INSTRUCTIONS.md)** | One-command Docker Compose quickstart, persistent volume layout, and daily usage |
| **[DEVELOPMENT.md](DEVELOPMENT.md)** | Local native setup (Java 21 + Node 24), Codespaces setup, and full verification commands |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | End-to-end request flow, module boundaries, caching strategy, and runtime guarantees |
| **[PROJECT_PLAN.md](PROJECT_PLAN.md)** | Product scope, architectural trade-offs, completed milestones, and roadmap |

---

## Multi-ATS Aggregation Engine

| Document | Purpose |
| --- | --- |
| **[ATS_PROVIDERS.md](ATS_PROVIDERS.md)** | Public endpoints, field mappings, and resilience rules across all 9 ATS platforms (334 pre-seeded Spain & Europe boards), plus auto-discovery, SWR caching, and geo/junior scoring |

---

## Profile, Explainable Matching & Ranking Domain

| Document | Purpose |
| --- | --- |
| **[PROFILE.md](PROFILE.md)** | 14-role browser-local professional profile schema, validation rules, and storage recovery |
| **[MATCHING.md](MATCHING.md)** | Deterministic `v1` compatibility rules separating hard constraints, advisory preferences, and unknown data (24 reference cases) |
| **[MATCHING_ENGINE.md](MATCHING_ENGINE.md)** | Pure Java `matching` domain types, immutability guarantees, and adapter boundaries |
| **[MATCHING_API.md](MATCHING_API.md)** | `POST /api/matching/evaluate` HTTP contract, transient profile handling, and request cancellation |
| **[RANKING.md](RANKING.md)** | Exact-fraction compatibility batch ranking policy (`CompatibilityRanker`) and search integration |

---

## Interface & Localization

| Document | Purpose |
| --- | --- |
| **[DESIGN.md](DESIGN.md)** | Selected *Current* visual baseline, typography, color tokens, and responsive acceptance criteria |
| **[LANGUAGES.md](LANGUAGES.md)** | Bilingual (`Español` / `English`) locale detection, persistence, and presentation boundaries |
