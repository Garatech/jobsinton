# JobSinton

**Job opportunities in tune with you.**

JobSinton is built to help software professionals find job opportunities that align with their skills, experience, location, and working preferences.

## Planned capabilities

- **Personalized search:** tailor results to professional goals, technologies, and working conditions.
- **Explainable matching:** understand why an opportunity fits, where requirements differ, and which details are missing.
- **Flexible preferences:** distinguish essential requirements from negotiable preferences.
- **European scope:** start with Spain and expand through supported job providers.
- **Saved opportunities:** keep relevant openings together for later review.

Match scores will express compatibility with a profile, not the likelihood of receiving a job offer.

## Project status

JobSinton runs out of the box with **zero API keys or `.env` configuration**. The Java 21 / Spring Boot backend aggregates live openings in parallel from **22 pre-seeded company boards across 5 public ATS JSON APIs** (Greenhouse, Lever, SmartRecruiters, Workable, and Ashby), normalizes publication timestamps and plain-text descriptions, and serves them with a 5-minute TTL cache and board-level fault isolation.

The approved Current React interface opens directly onto live ATS postings with **Live ATS Radar** on Home, stack presets (`All Tech`, `Track A · SAP`, `Track B · Full Stack`, `Custom Profile`), one-click freshness (`< 48h`) and Spain/Madrid/Remote filters, Blip-style recency badges, and direct links to each company's official career application page. The fictional `Sample catalog`, browser-local profile and shortlist, and deterministic Java matching/compatibility engine remain integrated alongside live ATS boards. Accounts and server-side persistence remain planned.

Stack: **Java 21 · Spring Boot · React · TypeScript**.

## Project documentation

[Run the project](docs/DEVELOPMENT.md) · [Multi-ATS Providers](docs/ATS_PROVIDERS.md) · [Design and acceptance checklist](docs/DESIGN.md) · [Matching verification](docs/MATCHING_API.md) · [Architecture](docs/ARCHITECTURE.md) · [Changelog](CHANGELOG.md) · [Development plan](docs/PROJECT_PLAN.md) · [Contributing](CONTRIBUTING.md)

A **Gara's Workshop** project.
