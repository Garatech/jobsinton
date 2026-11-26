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

## Quickstart (Docker — 1 Command)

With [Docker](https://docs.docker.com/get-docker/) installed, clone and boot the entire stack from the repository root:

```bash
git clone https://github.com/Garatech/jobsinton.git
cd jobsinton
docker compose up --build -d
```

Then open **[http://localhost:5173](http://localhost:5173)** in your browser. See **[INSTRUCTIONS.md](INSTRUCTIONS.md)** for full Docker and native setup instructions.

## Project status

JobSinton runs out of the box with **zero API keys or `.env` configuration**. The Java 21 / Spring Boot backend aggregates live openings in parallel from **334 pre-seeded Spain & Europe company boards across 9 public ATS platforms** (Personio, Teamtailor, Recruitee, Factorial HR, Greenhouse, Lever, SmartRecruiters, Workable, and Ashby), filters out non-European postings at ingestion unless eligible for Spain/Europe remote work, tracks true publication age via first-seen persistence, and includes an **ATS Auto-Discovery Engine** (Career Page URL Auto-Sniffer & 9-ATS Batch Slug Probe).

The React + TypeScript interface opens directly onto live ATS postings with **Live ATS Radar** on Home, **8 multi-discipline developer tracks** (`All Tech / Software Roles`, `Software & Web Development`, `Python, Data & Analytics`, `AI & Machine Learning`, `Cloud, DevOps & Systems`, `QA Automation & Cybersecurity`, `SAP, ERP & Enterprise CRM`, `Custom Profile`), a **3-tier Spain/Europe geographical filter** (`🇪🇸 Spain Only`, `🇪🇺 Spain + EU Remote`, `🌍 Global`) with an Anti-Fake-Remote blocker, **1-click job triage** (`⭐ Save`, `✅ Applied`, `✕ Dismiss`), first-48h recency badges, and direct links to each company's official career application page.

Stack: **Java 21 · Spring Boot · React · TypeScript · Docker**.

## Project documentation

[Boot Instructions (Docker & Local)](INSTRUCTIONS.md) · [Run the project](docs/DEVELOPMENT.md) · [Multi-ATS Providers](docs/ATS_PROVIDERS.md) · [Design and acceptance checklist](docs/DESIGN.md) · [Matching verification](docs/MATCHING_API.md) · [Architecture](docs/ARCHITECTURE.md) · [Changelog](CHANGELOG.md) · [Development plan](docs/PROJECT_PLAN.md) · [Contributing](CONTRIBUTING.md)

A **Gara's Workshop** project.
