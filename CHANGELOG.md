# Changelog

This is a record of notable changes to JobSinton: their purpose, behavior, verification, and known limits. Unreleased work becomes part of `main` only after its pull request is merged.

## [Unreleased]

### Added — Spain Junior Harvest, SmartRecruiters `country=es` Pass, Adapter Pagination/Priority Tuning & 334-Board Catalog

- Enhanced `SmartRecruitersAdapter` to run a targeted `?limit=100&country=es` pass (up to 200 Spain postings) whenever a global enterprise board hits the 100-item page cap, extract structured list-level `experienceLevel` and `typeOfEmployment` metadata (preserving `hasFullDescription: false` until full detail hydration), prioritize Spanish junior/entry/trainee postings (`+55` pts) and Barcelona/Madrid (`+25` pts) during initial detail enrichment (`jobsinton.ats.smartrecruiters-detail-cap=14`), and expand the in-memory detail cache to `4,000` entries.
- Enhanced `FactorialAdapter` to prioritize junior/trainee (`+60` pts) and engineering (`+45` pts) cards ahead of non-tech portal cards up to `jobsinton.ats.factorial-detail-cap=12`, and extract `datePosted` timestamps from detail HTML when missing from the portal card.
- Enhanced `WorkableAdapter` `v3` fallback (`POST /api/v3/accounts/{slug}/jobs`) to follow `nextPage` cursor tokens across up to 6 pages with deduplication instead of stopping after the first 10 results.
- Widened `detectSenioritySignal` description inspection in `frontend/src/ats/scoring.ts` from `1,200` to `12,000` characters and added structured ATS seniority/experience and Spanish graduate contract patterns (`contrato formativo`, `contrato en prácticas`, `recién licenciado`, `Experience Level: Entry Level`, `Seniority: entry-level`, `Experience: < 1` / `0-1` / `0-2`).
- Expanded `backend/src/main/resources/ats/companies.json` from 317 to **334 verified public company boards** (+17 Spanish and European tech hubs including Idealista Tech, Wallapop, New Relic Barcelona Hub, Twilio Madrid Hub, MongoDB Barcelona & Dublin, Canonical Ubuntu, GitLab Europe, ServiceNow Spain & Europe, Experian Spain & Tech, SGS Digital & IT Spain, Hugging Face Europe, Lovable, Linear Europe, Deliveroo Tech, SumUp Europe, Dataiku Europe, and Algolia Europe).

### Added — ATS Loading-Speed Optimization with Zero Opportunity Loss (Disk Snapshot, Startup Pre-Warming & Stale-While-Revalidate)

- Added `AtsFeedCacheStore` (`data/ats-feed-cache.json`) so cold starts and Docker restarts restore all cached opportunities into memory immediately (`< 50ms`) with zero truncation or loss of offers while revalidating live boards in the background.
- Added `ApplicationReadyEvent` startup pre-warming (`jobsinton.ats.warm-on-startup=true`) and Stale-While-Revalidate (SWR) background virtual-thread refreshes in `AtsFeedService` (`refreshing: true`), returning cached postings immediately when the 10-minute TTL expires while preserving existing cached postings if the host is temporarily offline (`Force Refresh` / `?refresh=true` continues to refresh synchronously).
- Updated `LiveJobsBoard.tsx`, `Home.tsx`, and `frontend/src/api/ats.ts` (`FEED_TIMEOUT_MS=50_000`, `bypassClientCache`) to display cached opportunities immediately and seamlessly merge newly refreshed roles in place once background revalidation completes.

### Added — ATS Tuning, Junior & Freshness Overhaul, Web/Java Role Recovery & 317-Board Catalog

- Expanded `FULLSTACK_KEYWORDS` (`frontend/src/ats/presets.ts`) and bilingual EN/ES `KEYWORD_ALIASES` (`frontend/src/ats/scoring.ts`) with software and web role/ecosystem descriptors (`Software Engineer`, `Software Developer`, `Backend`, `Frontend`, `Full Stack`, `Web Developer`, `Spring`, `Hibernate`, `Express`, `NestJS`, `HTML/CSS`) so unhydrated listings match immediately by title.
- Refined `AtsSupport.isEuropeEligibleJob` (`backend/.../providers/ats/AtsSupport.java`) and `hasFakeRemoteRestriction` (`frontend/src/ats/scoring.ts`) with `STRICT_SINGLE_COUNTRY_LOCK_PATTERN` so multi-country remote roles that explicitly include a European country alongside the US (e.g. `Remote - US, UK, Germany`) are preserved, and expanded Spanish tech hub municipality patterns (`Sant Cugat`, `Paterna`, `Sabadell`, `Terrassa`, `Leganés`, `Alcalá de Henares`, `Majadahonda`, `Donostia`, `Vitoria`, `Las Palmas`, etc.).
- Expanded `JUNIOR_FRIENDLY_PATTERNS` (`frontend/src/ats/scoring.ts`) with bilingual Spanish and European entry/trainee signals (`sin experiencia`, `recién titulado`, `primer empleo`, `trainee`, `prácticas`, `associate`, `working student`, `werkstudent`, `0-2 años`), added title penalties for `Senior`/`Sr.` (`-18`) and `Tech Lead`/`Team Lead`/`Architect` (`-25`), and introduced graduated recency scoring with stale-posting penalties for `> 60d` (`-20`) and `> 90d` (`-45`).
- Added `📅 Recent (< 30d)` and `🎯 Junior / Entry` quick-filter pills in `frontend/src/pages/LiveJobsBoard.tsx` with Spanish translations (`frontend/src/i18n/messages.ts`).
- Expanded `backend/src/main/resources/ats/companies.json` from 293 to **317 verified public company boards** (+24 Spanish and European boards across SmartRecruiters, Teamtailor, Recruitee, Personio, Factorial HR, Workable, Ashby, and Greenhouse).

### Added — Minimalist One-Command Dockerization & Operating Instructions (`INSTRUCTIONS.md`)

- Added root `docker-compose.yml` orchestrating the `backend` and `frontend` containers with a persistent `jobsinton-data` volume (`/app/data` for `custom-companies.json` and `ats-seen-jobs.json`) and healthcheck-gated startup exposed on `http://localhost:5173`.
- Added multi-stage `backend/Dockerfile` (`maven:3.9.9-eclipse-temurin-21-alpine` → non-root `eclipse-temurin:21-jre-alpine`) and `backend/.dockerignore`.
- Added multi-stage `frontend/Dockerfile` (`node:24-alpine` → `nginx:1.27-alpine`), `frontend/nginx.conf` (serving the production Vite bundle and proxying `/api/*` to `http://backend:8080/api/`), and `frontend/.dockerignore`.
- Added root `INSTRUCTIONS.md` and updated `README.md` with step-by-step instructions to clone and launch JobSinton on any computer with Docker installed.

### Added — Expanded 14-Role Professional Profile Taxonomy & Backend Adapter Alignment

- Expanded `ROLES` (`frontend/src/profile/model.ts`), Spanish translations (`frontend/src/i18n/messages.ts`), `profileSearch` (`frontend/src/pages/Home.tsx`), and `SavedProfileAdapter` (`backend/.../matching/api/SavedProfileAdapter.java`) from 6 to **14 developer and tech roles**: `Backend developer`, `Frontend developer`, `Full stack developer`, `SAP developer`, `Data engineer`, `Platform engineer`, `Data analyst`, `Python developer`, `AI / ML engineer`, `Cloud & DevOps engineer`, `QA & test automation`, `Cybersecurity specialist`, `Mobile developer`, and `ERP / CRM consultant`.
- Added `presetForProfileRole` (`frontend/src/ats/presets.ts`) mapping each of the 14 profile roles to its corresponding multi-discipline ATS stack preset, with automated unit and integration coverage in `profile-model.spec.ts`, `live-ats.spec.ts`, and `MatchingApiIntegrationTest.java`.

### Added — Multi-Discipline Developer Stack Tracks & Bilingual Tech Keyword Scoring

- Expanded `STACK_PRESETS` (`frontend/src/ats/presets.ts`) from two CV-hardcoded presets to **8 multi-discipline developer tracks**: `All Tech / Software Roles`, `Software & Web Development`, `Python, Data & Analytics`, `AI & Machine Learning`, `Cloud, DevOps & Systems`, `QA Automation & Cybersecurity`, `SAP, ERP & Enterprise CRM`, and `Custom Profile`.
- Expanded `KEYWORD_ALIASES` and `TECH_TITLE_INDICATORS` (`frontend/src/ats/scoring.ts`) with bilingual (EN/ES) token-boundary matching for Data Analyst (`Analista de Datos`), Data Engineer (`Ingeniero de Datos`), Data Scientist, BI, Power BI, Tableau, Spark, Databricks, Snowflake, Airflow, dbt, AI/LLM/RAG/PyTorch/LangChain, Cloud/DevOps/SRE/SysAdmin (`Ingeniero de Sistemas`), QA Automation (`Playwright`, `Cypress`, `Selenium`), Cybersecurity (`Ciberseguridad`, `SOC`, `SIEM`, `Pentesting`), and SAP functional/technical modules plus Salesforce, Dynamics 365, and ServiceNow.
- Updated `LiveJobsBoard.tsx`, `Home.tsx`, and Spanish translations (`messages.ts`) to display live match counts, active keyword chips, and home page shortcuts for all 8 tracks.

### Added — Europe-Eligibility Ingestion Filter, SmartRecruiters Multi-Page Pagination & 293-Company Spain/Europe Seed Catalog

- Added `AtsSupport.isEuropeEligibleJob` in `AtsFeedService` to discard non-European (US, Canada, LATAM, APAC, India, Australia) job postings at ingestion unless the role includes a Spanish/European location or offers European/EMEA/CET/Worldwide remote eligibility.
- Added bounded multi-page pagination (up to 3 pages / 300 postings per board) in `SmartRecruitersAdapter` when a board returns a full 100-item page, and expanded `COUNTRY_NAMES` ISO country code resolution in `AtsSupport`.
- Expanded `classifyGeoTier` in `frontend/src/ats/scoring.ts` so hybrid and on-site European tech hub roles (Germany, France, Portugal, Netherlands, Ireland, Italy, Poland, Sweden, Austria, Belgium, Switzerland, Denmark, Finland, Norway, Czechia, Romania, etc.) classify into Tier 2 (`🇪🇺 Spain + EU Remote`) while Tier 1 (`🇪🇸 Spain Only`) continues to rank first.
- Expanded `backend/src/main/resources/ats/companies.json` from 222 to **293 verified public company boards** (+71 new Spanish and European tech boards across Personio, Teamtailor, Recruitee, SmartRecruiters, Workable, Ashby, Greenhouse, and Lever EU).

### Added — ATS Auto-Discovery Engine (Career Page URL Sniffer & 9-ATS Batch Slug Probe)

- Added `AtsDiscoveryService` (`POST /api/ats/discover/url` and `POST /api/ats/discover/probe`) supporting automatic detection of embedded Personio, Teamtailor, Recruitee, Factorial HR, Greenhouse, Lever, SmartRecruiters, Workable, and Ashby boards from any corporate career page HTML or direct ATS URL, plus parallel virtual-thread slug probing across all 9 ATS platforms.
- Extended `CompanyRegistry` to persist newly discovered boards in `data/custom-companies.json` (gitignored) and extended `AtsFeedService` (`mergeDiscoveredBoard`) to merge newly discovered postings directly into the active feed cache without re-fetching all 222+ boards.
- Added the expandable **`➕ Discover / Add Companies`** panel in `frontend/src/pages/LiveJobsBoard.tsx` with bilingual EN/ES support, unit tests (`AtsDiscoveryServiceTest`), integration tests (`AtsControllerIntegrationTest`), and Playwright E2E coverage (`live-ats.spec.ts`).

### Added — 3-Tier Spain/EU Geographical Filter, Anti-Fake-Remote Blocker, Seniority Scoring & 1-Click Job Triage

- Replaced the single geographical toggle with a **3-Tier Smart Geographical Filter** (`🇪🇸 Spain Only`, `🇪🇺 Spain + EU Remote` active by default on launch, and `🌍 Global`) in `frontend/src/ats/scoring.ts` and `frontend/src/pages/LiveJobsBoard.tsx`.
- Added a strict **Anti-"Fake Remote" Blocker** under `🇪🇺 Spain + EU Remote` that excludes non-EU remote roles (`US only`, `U.S. only`, `United States`, `Remote - US`, `North America`, `Canada`, `LATAM`, `Brazil`, `Mexico`, `APAC`, `India`, `UK only`, `Australia`, `PST`/`EST`/`CST`, `must reside in the US`) and sorts Tier 1 (`Spain`) above Tier 2 (`EU Remote`) on relevance ties.
- Added **Junior/Entry Affinity Boost** (`🎯 Junior / Entry Fit`, `+20` to `+30` pts) and **Senior/Staff Penalty** (`⚠️ Senior/Staff`, `-25` pts) to rank entry-level and mid-level opportunities higher for junior developers.
- Added browser-persisted **1-Click Job Triage** (`⭐ Save`, `✅ Applied`, `✕ Dismiss` stored under `jobsinton.ats-triage-v1`) with `Hide Applied & Dismissed` (enabled by default), `⭐ Saved`, and `View Applied` quick filters.

### Added — Teamtailor, Factorial HR & 222-company Spain/EU seed catalog

- Added `TeamtailorAdapter` (`teamtailor`) supporting public RSS `/jobs.rss` feeds with `xmlns:tt="http://teamtailor.com/rss"` namespace (`<tt:remoteStatus>`, `<tt:locations>`, `<tt:department>`) and custom corporate career domains (`empleo.civitatis.com`, `careers.pccomponentes.com`, etc.).
- Added `FactorialAdapter` (`factorial`) supporting Spanish SME career portals on `.factorialhr.es` and `.factorialhr.com`, parsing embedded `application/ld+json` (`JobPosting`) and public `/job_posting/{id}` cards with bounded detail page enrichment (`jobsinton.ats.factorial-detail-cap=6`).
- Expanded `backend/src/main/resources/ats/companies.json` from 22 to **222 verified company boards** across all 9 ATS platforms focused on Madrid, Spain, and EU Remote SMEs, scaleups, and SAP/Java consultancies.
- Added bounded virtual-thread `Semaphore` concurrency (`jobsinton.ats.max-concurrency=18`) in `AtsFeedService` and increased the default feed cache TTL to 10 minutes (`600s`).

### Added — Personio and Recruitee European SME ATS adapters

- Added `PersonioAdapter` (`personio`) supporting public `<workzag-jobs><position>` XML feeds (`?language=es` and `?language=en` across `.jobs.personio.com` and `.jobs.personio.de`) with automatic dual-language description merging when English-only postings return empty `<jobDescriptions>` on `?language=es`.
- Added `RecruiteeAdapter` (`recruitee`) supporting Tellent/Recruitee public `/api/offers/` JSON endpoints with native `remote`/`hybrid`/`on_site` flags, cleaned description + requirements, and structured salary ranges.
- Extended `CompanyBoard` and `CompanyRegistry` to support custom `host`, `careersUrl`, `apiEndpoint`, `fallbackEndpoint`, `locations`, `stackMatch`, and `priority` metadata; added Personio and Recruitee provider filters and badges in the frontend.

### Fixed — ATS EU cluster fallbacks, Spain geo-priority enrichment & First-Seen age tracking

- Updated `GreenhouseAdapter` to prefer `first_published` over `updated_at` and automatically fall back to `boards-api.eu.greenhouse.io` on HTTP 404 or empty boards; added bidirectional Global ↔ EU 404 fallback in `LeverAdapter`.
- Updated `JdkAtsHttpClient` with a browser-compatible `User-Agent` header so Ashby boards never reject requests with HTTP 403, and updated `SmartRecruitersAdapter` to prioritize Spain (`Madrid`, `Barcelona`, `ES`, etc.) and EU Remote postings before spending bounded detail calls.
- Added lightweight local first-seen timestamp persistence (`FirstSeenRegistry` → gitignored `data/ats-seen-jobs.json`) so recruiter edits to old listings never fake a `< 24h` freshness badge.

### Added — Multi-ATS direct provider engine

- Replaced the aggregator-based live job path with a zero-configuration **Multi-ATS Direct Provider Engine** querying 5 unauthenticated public ATS JSON APIs (**Greenhouse**, **Lever Global & EU**, **SmartRecruiters**, **Workable v1 & v3**, and **Ashby**) across **22 pre-seeded company boards** (`backend/src/main/resources/ats/companies.json`).
- Added Java 21 virtual-thread parallel fetching with board-level fault isolation, 8-second per-request timeouts, 12 MiB payload cap, `https://`-only URL validation, HTML entity/tag cleaning, deduplication, newest-first sorting, 5-minute TTL caching with stampede coalescing, and on-demand SmartRecruiters detail hydration (`GET /api/ats/jobs`, `GET /api/ats/boards`, `GET /api/ats/smartrecruiters/{companySlug}/{postingId}`).
- Added a zero-friction **Live ATS Boards** experience as the default view on `#opportunities` plus **Live ATS Radar** on `#home`, complete with developer stack presets (`All Tech / Software Roles`, `Track A · SAP & Enterprise`, `Track B · Full Stack (Java / JS)`, `Custom Profile`), token-boundary skill matching, one-click quick filters (`My Profile Matches`, `Fresh (< 48h)`, `Spain / Madrid / Remote`, `All Fetched Jobs`), first-48h freshness badges (`🔥 <= 24h`, `⚡ 25–48h`), board health diagnostics, Force Refresh, and direct `Apply on Company Site` links.
- Added offline backend unit/MockMvc tests and deterministic Playwright E2E + scoring tests in English and Spanish; documented the architecture in [ATS_PROVIDERS.md](docs/ATS_PROVIDERS.md).

### Removed — Deprecated Adzuna integration

- Removed the key-gated Adzuna backend adapter, local Spring profile, frontend search/comparison components, PowerShell/Node probe scripts, and dedicated Adzuna CI/Playwright configs in favor of direct public company ATS endpoints. Historical evaluation docs remain archived for reference.

### Decision — Adzuna result order

- Retain provider order and per-listing profile comparisons. The first measured live page produced no evidence-based ordering across ten listings. [Observation and review criteria](docs/ADZUNA_PAGE_STUDY.md#ordering-decision-5d2).

### Added — Local Adzuna page study

- Measure comparable facts, confirmed conflicts and evidence ties on a returned page without another provider request. [Offline checks and report guide](docs/ADZUNA_PAGE_STUDY.md).

### Added — Compare Adzuna results in search

- Open a bilingual profile comparison on a returned listing without losing the page or repeating the search. Weekly attendance shows the stated requirement and saved limit when known. [Verification](docs/ADZUNA_COMPARISON.md#verify-the-search-card).

### Added — Compare an Adzuna result locally

- Compare the selected search result with a saved profile without fetching or storing the advert. The response uses the existing matching rules and preserves unknown conditions. See [the contract and offline checks](docs/ADZUNA_COMPARISON.md).

### Added — Explicit facts from Adzuna listings

- Recognise exact role titles and explicit work mode and weekly attendance statements in the supplied advert. Missing or conflicting schedules stay unknown. See [examples and offline verification](docs/ADZUNA_MAPPING.md#explicit-matching-facts-5a).

### Added — Spanish and English interface (2026-11-14)

- Added a persistent language choice across the demo and local provider search. Switching updates owned text, matching explanations and accessible labels in place. Adverts and user-entered fields keep their original text. See [language verification](docs/LANGUAGES.md).

### Added — Client language resources (2026-11-14)

- Prepared Spanish and English copy, sample catalog translations, matching reason labels and locale-aware dates and amounts. This increment does not change the active interface.

### Added — Local Adzuna search in Opportunities (2026-11-14)

- Added an optional Spain source beside the sample catalog. Explicit searches and page controls use the backend; opening an excerpt reuses the current result. Unknown facts stay visible and provider results remain in memory.
- Added [local browser setup and offline checks](docs/ADZUNA_SEARCH.md#verify-the-browser-without-keys). The default development server continues to serve the sample catalog.

### Fixed — Office-day limit in the saved profile (2026-11-14)

- A remote preference now shows “Not specified” for the weekly office limit, consistent with matching. Saved profiles and ranking rules are unchanged.

### Fixed — Encoding in local Adzuna search responses (2026-11-13)

- Declared UTF-8 on local search JSON responses so clients can decode Spanish accents. Added an [offline PowerShell check](docs/ADZUNA_SEARCH.md#verify-without-keys) using a fictional excerpt.

### Added — Optional local Adzuna search (2026-11-13)

- Added a loopback-only Spring profile for one bounded page of Spanish vacancies. The backend reuses the listing mapper, preserves provider order and distinguishes duplicate, rejected and absent results from provider errors.
- Added offline verification and [PowerShell instructions for the optional live check](docs/ADZUNA_SEARCH.md). Regular startup and the fictional frontend require no credentials.

### Added — Offline Adzuna listing conversion (2026-11-13)

- Added a Java mapper for individual search results. It preserves listing metadata and salary provenance, rejects unusable referrals and keeps unstated matching facts unknown.
- Added [offline verification steps](docs/ADZUNA_MAPPING.md). The website and its catalog have not changed.

### Added — Local Adzuna evaluation (2026-11-13)

- Added a [one-request evaluation tool and review guide](docs/ADZUNA_PROBE.md) for Java vacancies in Madrid. The tool reports aggregate evidence and leaves unknown fields explicit.
- Added offline checks for the request limit, failure handling and evaluation counts. The authenticated result is pending; the application catalog remains fictional.

### Documented — First live source review (2026-11-13)

- Compared four job sources for Spain and selected Adzuna for a bounded, zero-cost evaluation, subject to account access. [Provider decision and next probe](docs/PROVIDERS.md).
- Documented display, retention and matching limitations. The application still uses fictional listings; there is no live connector or new setup requirement.

### Added — Compatibility sort in opportunity search (2026-11-12)

- Connected the Java ranker to search with one validated batch of filtered IDs and the saved profile. Newest remains the default and Salary remains available.
- Preserved unavailable offers and added loading, explicit Newest fallback, retry and stale-response protection. Layout and shortlist actions retain the same result set.
- Added HTTP and browser verification plus a [manual search checklist](docs/RANKING.md#manual-acceptance-checklist). The implementation PR records the tested commit and results.

### Added — Compatibility ranking domain (2026-11-12)

- Implemented the accepted ordering of filtered opportunities for one profile: confirmed constraints first, then exact net support, coverage and ID. Invalid offers remain individually identifiable.
- Added executable R01–R14 examples, including fractional tag weights, changed priorities and stable order across input permutations.
- Added [local verification commands](docs/RANKING.md#verification-after-merge). The website and HTTP contracts are unchanged; search integration follows separately.

### Proposed — Compatibility ranking (2026-11-11)

- Added [ranking rules and worked examples](docs/RANKING.md) for review: constraint precedence, advisory weights, evidence coverage and stable tie-breaks.
- Recorded verification of the comparison journey by the owner. Search still offers Newest and Salary; the proposal changes no runtime behavior.

### Added — Matching API and opportunity details (2026-10-31)

- Connected the accepted matching engine to the existing opportunity detail panel through a validated, bounded POST endpoint, tracked in issue #21.
- Added saved-profile normalization and server-owned fictional comparison facts; retained unknown residence policies and scoped-experience limitations.
- Added loading, timeout/error recovery, explicit retry, response validation and cancellation/stale-response protection.
- Preserved draft/save separation, shortlist behavior, browser profile format and the Current design. Updated profile wording to explain transient server transmission.
- Added HTTP, browser and catalog-consistency checks; actual results and the checked commit are recorded in the implementation PR.
- Added [API documentation and a manual acceptance checklist](docs/MATCHING_API.md). No ranking, provider, account, dependency or database migration is introduced.

### Added — Matching domain engine (2026-10-30)

- Implemented the owner-accepted matching rules in a standalone Java module, tracked in issue #19.
- Added immutable normalized inputs, structured validation errors and versioned assessments with stable reasons, compared facts and limitations.
- Kept advisory preferences separate from explicit attendance and residence constraints. Unknown evidence is preserved alongside conflicts.
- Added executable M01–M24 reference cases and checks for incomplete facts, invalid inputs, deterministic ordering and defensive snapshots.
- Added [engine verification instructions and the catalog mapping plan](docs/MATCHING_ENGINE.md); recorded contract acceptance and the next integration step.
- The implementation PR records build and test results for its checked commit. No endpoint, frontend change, score, dependency or data migration is introduced.

### Added — Matching rule contract (2026-10-30)

- Opened `docs/matching-v1-contract` for the first matching definition, tracked in issue #17.
- Added [comparison rules, 24 fictional reference cases and an owner review checklist](docs/MATCHING.md).
- Separated advisory preferences from explicit constraints, unknown evidence and unevaluated conditions; documented limits in the current profile and fictional catalog.
- Split implementation into domain engine, application integration and later ranking increments. Scores and weights remain deferred.
- Checked source-model alignment, reference expectations and internal links. The reference cases are not executable tests yet; owner acceptance of the proposed rules is pending.
- Updated the project plan and implementation handoff. Application code, stored profiles and the accepted design are unchanged.

### Changed — Page transitions (2026-10-30)

- Added a 200 ms opacity fade when navigating to another page, with the shared header and navigation kept stable.
- Preserved immediate interaction, profile drafts, browser history and normal page focus. Initial loads and in-page actions do not animate.
- Cancelled superseded transitions and respected reduced motion, including preference changes during an active fade.
- Kept immediate navigation as the fallback when the browser animation API is unavailable; no new dependency was added.
- Added transition-specific browser checks and a [manual acceptance checklist](docs/DESIGN.md#transition-acceptance-checklist).
- Recorded owner acceptance of the integrated Current interface and the close of this design stage. Matching v1 definition is the next planned increment.

Verification results and the tested commit are recorded in the implementation PR.

### Added — Current application interface (2026-10-30)

**Purpose:** apply the owner-selected design to the real application and use it as the baseline for subsequent frontend work.

- Integrated Current's palette, typography, hero and page layouts, with Ledger-inspired list search and Atlas-inspired card search.
- Added navigation, a six-offer fictional catalog, three company profiles, opportunity pages, filters, sorting and empty states.
- Added persistent browser-local saved opportunities with validation, confirmed clearing and storage-failure recovery.
- Preserved the existing profile storage format and validation; added data/platform roles, draft retention during navigation and discard controls.
- Connected home direction, search links and descriptive opportunity facts to the last saved profile. No numerical matching score is implied.
- Kept the real API check, timeout and retry on Service status. Application actions explain that fictional offers cannot receive applications.
- Added build, browser, storage, navigation and responsive checks. Actual results are recorded in the implementation PR.
- Added [DESIGN.md](docs/DESIGN.md) with the selected baseline, verification steps and manual acceptance checklist.

The exploratory preversion branch remains separate. No database, live provider, account or production deployment is introduced.

### Added — Visual design studies (2026-10-29)

**Purpose:** compare visual directions before extending the product frontend. Tracked in issue #11.

- A source-based Current reference and four independent static alternatives: Folio, Ledger, Atlas and Nocturne.
- Comparable fictional catalog, details, saved-opportunity and profile interactions with session-only state.
- An offline comparison page and downloadable notes for selecting elements across versions.
- [Research and sources](design-exploration/RESEARCH.md), a [review guide](design-exploration/REVIEW.md), and [verification scope and limits](design-exploration/VERIFICATION.md).
- Browser checks for five designs across four widths, with a ZIP package produced only after successful verification.
- Corrected Nocturne's narrow layout, Current's reference styling and focus recovery after removing a saved item.
- The design workflow runs on relevant branch pushes and pull requests. Review the PR checks for the tested revision and result.
- This study does not select or apply a redesign to the production application. The owner reviews the alternatives before the next frontend increment.

### Added — Professional profile (2026-10-20)

**Purpose:** turn professional goals and working preferences into an editable, browser-local profile. Tracked in issue #9.

- Target role, add/remove technologies, whole years of experience, country, city, work mode, and conditional on-site day limits.
- Explicit save, reload recovery, last-saved summary, unsaved-change feedback, and confirmed clear.
- Shared validation for form input and versioned saved data; keyboard-accessible errors and responsive controls.
- Storage failure handling that preserves edits and never reports a false save.
- Automated validation and desktop/mobile browser coverage for the main journeys and failure cases.
- [Profile verification guide](docs/PROFILE.md) with commands and a post-merge acceptance checklist.

### Updated

- README, architecture, and project plan reflect the profile increment.
- Development instructions clarify local-first setup, Node 24, Java 21, stopping servers before reinstalling dependencies, Windows file locks, and stale VS Code environments.
- Recorded owner-reported successful Windows foundation verification on 2026-10-20.

### Profile verification and limits

- Implementation commit `25d971b` passed [GitHub Actions run 39445633841](https://github.com/Garatech/jobsinton/actions/runs/39445633841): 2 backend HTTP tests, locked dependency installation, TypeScript checking, production build, and 40 Playwright executions (30 browser journeys plus 5 validation cases repeated in both project configurations). See [PR #10](https://github.com/Garatech/jobsinton/pull/10) for the final checked revision.
- The profile is stored only in this browser and origin; there is no device synchronization, account, backend profile endpoint, or database.
- Unsaved edits are lost on reload. Clearing browser data removes the profile; multiple tabs use last-save-wins behavior.
- Automated viewport and keyboard checks do not replace human visual acceptance. The owner has reported completing the profile manual checklist successfully, including browser-local persistence.
- The development terminal is unavailable; remote changes do not automatically update the local checkout.

### Documentation update — 2026-10-18

- Opened `docs/documentation-update` for a wording update across the README, development notes, project plan, contribution guide, access review, and PR template.
- Refined the wording in `docs/editorial-refinement` for a consistent, project-focused tone.
- Preserved the technical scope, instructions, and verification records; no application code or configuration changes.

### Added — Runnable foundation (2026-10-18)

**Purpose:** establish a small, testable connection between the Java and React stacks, with a shared workflow for desktop, laptop, and optional browser development. Tracked in issue #3.

- Java 21 / Spring Boot service with `GET /api/status`, a deliberately narrow connectivity contract.
- React / TypeScript introduction with live connection status, loading feedback, bounded wait, failure handling, and manual retry.
- Responsive layout, keyboard skip link, visible focus, and clearly labeled planned capabilities.
- Backend HTTP integration tests and browser checks for a real connection, outage recovery, invalid responses, and timeouts.
- GitHub Actions build/test workflow with a read-only token and pinned action revisions.
- Optional dev container and a development guide covering startup, verification, and safe changes between devices.
- Architecture notes explaining the request path, runtime validation, cancellation, and deliberately limited scope.
- This changelog as a required record for subsequent updates.

### Changed

- README reflects the runnable foundation while keeping future job-discovery capabilities separate.
- Project plan records the implementation increment and remaining environment acceptance checks.
- Contribution guide and PR template require changelog updates and actual verification evidence.

### Verification and limits

- Initial cloud verification passed: 2 backend HTTP tests, strict TypeScript checking, a frontend production build, and 8 browser checks across desktop/mobile Chromium ([run 39141434566](https://github.com/Garatech/jobsinton/actions/runs/39141434566)).
- Committed the generated npm lockfile, removed the one-time bootstrap step, and updated GitHub Actions from deprecated runtime versions to current pinned releases. The final PR check verifies these changes using only locked installs.
- Review [PR #4](https://github.com/Garatech/jobsinton/pull/4) for the final checked commit and validation result.
- The local Windows checkout could not be inspected in this session because the development terminal failed to initialize its sandbox. Local synchronization and local test results remain unverified.
- Codespaces configuration has not yet been exercised; no cloud environment or paid resource was created.
- There is no database, profile editor, job search, matching, live provider, or production deployment yet.
- No independent human review or full accessibility audit has been performed.

## Documentation baseline — 2026-10-17

- Established the public repository, VS Code workspace, review process, and access-review notes.
- Separated the product README from the working project plan in PR #2, subsequently merged by the owner.
