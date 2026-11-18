# Multi-ATS direct provider engine

JobSinton fetches live openings directly from public company Applicant Tracking System (ATS) JSON endpoints with **zero API keys, zero `.env` configuration, and EUR 0 budget**. Instead of relying on rate-limited job aggregators (such as the deprecated Adzuna integration), every fetched listing carries the employer's publication timestamp, plain-text description for skill matching, and direct `https://` application link.

## Supported public ATS platforms

| ATS | Public endpoint(s) | Key field mappings & handling |
| --- | --- | --- |
| **Greenhouse** | `GET https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true` | `id`, `title`, `updated_at` (ISO 8601), `location.name`, `content` (double-decoded HTML → clean plain text), `absolute_url` |
| **Lever** | Global: `GET https://api.lever.co/v0/postings/{slug}?mode=json`<br>EU: `GET https://api.eu.lever.co/v0/postings/{slug}?mode=json` | `id`, `text` (title), `createdAt` (epoch ms → ISO 8601), `categories.location` / `allLocations`, `workplaceType`, `descriptionPlain` + `lists[].content`, `applyUrl` / `hostedUrl`. Supports explicit `region: "eu"` or automatic Global → EU fallback on HTTP 404 |
| **SmartRecruiters** | List: `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings?limit=100`<br>Detail: `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings/{id}` | `id`, `name`, `releasedDate`, `location` (`city`, `country`, `remote`), fallback apply URL `https://jobs.smartrecruiters.com/{slug}/{id}`. Enriches top prioritized postings up to `jobsinton.ats.smartrecruiters-detail-cap` during feed load and supports on-demand detail hydration (`GET /api/ats/smartrecruiters/{slug}/{id}`) |
| **Workable** | v1 widget: `GET https://apply.workable.com/api/v1/widget/accounts/{slug}?details=true`<br>v3 fallback: `POST https://apply.workable.com/api/v3/accounts/{slug}/jobs` | `shortcode`, `title`, `published_on`, `city` / `country` / `telecommuting`, `description` (HTML → plain text), `url` (fallback `https://apply.workable.com/{slug}/j/{shortcode}/`). Follows standard 302 redirects and falls back to v3 POST when v1 returns 404/410 or empty |
| **Ashby** | `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true` | `id`, `title`, `publishedAt`, `location` / `secondaryLocations`, `isRemote` / `workplaceType`, `descriptionPlain` (fallback `descriptionHtml`), `compensation.compensationTierSummary`, `applyUrl` / `jobUrl` |

## Pre-seeded company registry

`backend/src/main/resources/ats/companies.json` ships with **22 pre-seeded company boards** spanning Madrid, Spain, Europe, and Remote tech employers across all 5 ATS platforms:

- **Greenhouse (7):** Cabify (`cabify`), Typeform (`typeform`), Glovo (`glovo`), Datadog (`datadog`), Stripe (`stripe`), Affirm (`affirm`), Speechify (`speechify`)
- **Lever EU (4):** TomTom (`tomtom`), OLX (`olx`), XTB (`xtb`), Creatio (`creatio`)
- **SmartRecruiters (4):** Alter Solutions (`AlterSolutions`), Celonis (`Celonis`), Bosch Group (`BoschGroup`), Visa (`Visa`)
- **Workable (2):** Teleperformance Spain (`teleperformance-spain`), MediaRadar (`mediaradar`)
- **Ashby (5):** Invopop (`invopop`), HappyRobot (`happyrobot.ai`), Vic.ai (`vic.ai`), Air Apps (`airapps`), OpenAI (`openai`)

Adding or removing a company requires editing `companies.json` (each entry specifies `slug`, `name`, `ats`, optional `region`, and optional `locationHint`).

## Backend architecture & resilience

All backend ATS code lives under `backend/src/main/java/io/github/garatech/jobsinton/providers/ats/`:

- **Parallel fault-isolated fetching (`AtsFeedService`):** Queries all registered boards concurrently using Java 21 virtual threads. If an individual board times out, returns 404/429/5xx, or returns malformed JSON, it is recorded in `boards[]` with `ok: false` and an explicit error summary without breaking the remaining boards.
- **Bounded HTTP transport (`JdkAtsHttpClient`):** Enforces a configurable per-request timeout (`jobsinton.ats.request-timeout-ms=8000`), follows normal redirects (`Redirect.NORMAL`), caps streamed response payloads at 12 MiB, and validates every normalized `applyUrl` to require `https://`.
- **In-memory TTL cache & stampede coalescing:** Caches the aggregated `AtsFeedResponse` for `jobsinton.ats.cache-ttl-seconds=300` (5 minutes) and coalesces concurrent requests onto one in-flight fetch. Passing `?refresh=true` bypasses the cache.
- **Deduplication & freshness:** Deduplicates across `(ats, companySlug, externalId)` and `applyUrl`, computes `hoursSincePublished` from the injected `Clock`, and orders jobs newest-first (undated listings last).

### Backend REST endpoints

- `GET /api/ats/jobs` (optional `?refresh=true`): Returns the unified `AtsFeedResponse` (`fetchedAt`, `cached`, `cacheAgeSeconds`, `totalBoards`, `succeededBoards`, `failedBoards`, `boards[]`, `jobs[]`).
- `GET /api/ats/boards`: Returns the configured `CompanyBoard` registry without making outbound HTTP calls.
- `GET /api/ats/smartrecruiters/{companySlug}/{postingId}`: Fetches full plain-text description and direct `applyUrl` for a SmartRecruiters posting on demand and patches the in-memory feed cache when present.

## Frontend zero-friction UX & stack presets

- **Instant load on launch:** Vite proxies `/api/ats` to `http://127.0.0.1:8080`. The Home page (`#home`) previews the freshest live ATS postings in **Live ATS Radar**, and Opportunities (`#opportunities`) defaults directly to **Live ATS Boards** (while keeping the fictional `Sample catalog` accessible via toggle or `?source=sample`).
- **Developer stack presets (`frontend/src/ats/presets.ts`):**
  - `All Tech / Software Roles`
  - `Track A · SAP & Enterprise` (`SAP`, `ABAP`, `S/4HANA`, `RAP`, `CAP`, `OData`, `Fiori`, `BTP` — with alias matching for `ABAP OO`, `OData v4`, `SAPUI5`, `SAP HANA`)
  - `Track B · Full Stack (Java / JS)` (`Java`, `Spring Boot`, `Angular`, `Node.js`, `React`, `TypeScript`, `PostgreSQL`, `Docker`)
  - `Custom Profile` (editable skill tags persisted in browser `localStorage`, plus one-click sync from the saved Professional Profile)
- **Token-boundary keyword matching (`frontend/src/ats/scoring.ts`):** Matches technical terms cleanly without false substring hits (for example, `Java` does not match `JavaScript`, `RAP` does not match `bootstrap`, `CAP` does not match `capacity`, `SAP` does not match `asap`) and supports one-click view toggles: **My Profile Matches**, **Fresh (< 48h)**, **Spain / Madrid / Remote**, and **All Fetched Jobs**.
- **Freshness highlights:** Badges postings published within 24 hours (`🔥 Posted Xh ago`) and 25–48 hours (`⚡ Posted Xh ago`), displays live board health diagnostics, and links directly to each company's official career application page (`Apply on Company Site ↗`).

## Offline verification

Automated CI and local verification never depend on live third-party ATS servers:

- **Backend unit & MockMvc tests:** `mvn -B -ntp -f backend/pom.xml test` runs `HtmlTextCleanerTest`, `AtsAdaptersTest`, `JdkAtsHttpClientTest`, `AtsFeedServiceTest`, and `AtsControllerIntegrationTest` using deterministic fixtures and stub transports.
- **Frontend scoring & Playwright E2E tests:** `npm --prefix frontend run test:e2e` runs `ats-scoring.spec.ts` and `live-ats.spec.ts` (plus all existing desktop/mobile journeys) with deterministic stubbed `/api/ats/jobs` and `/api/ats/smartrecruiters/*` payloads.
