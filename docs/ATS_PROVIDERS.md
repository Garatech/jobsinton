# Multi-ATS direct provider engine

JobSinton fetches live openings directly from public company Applicant Tracking System (ATS) JSON endpoints with **zero API keys, zero `.env` configuration, and EUR 0 budget**. Instead of relying on rate-limited job aggregators (such as the deprecated Adzuna integration), every fetched listing carries the employer's publication timestamp, plain-text description for skill matching, and direct `https://` application link.

## Supported public ATS platforms

| ATS | Public endpoint(s) | Key field mappings & handling |
| --- | --- | --- |
| **Personio** | Primary: `GET https://{slug}.jobs.personio.com/xml?language=es`<br>Fallback/Merge: `GET https://{slug}.jobs.personio.de/xml?language=en` | `<workzag-jobs><position>`: `<id>`, `<name>`, `<createdAt>` (ISO 8601), `<office>` + `<additionalOffices>`, `<jobDescriptions>` CDATA HTML, `<department>`, `<seniority>`, `<yearsOfExperience>`. **Dual-Language XML Rule:** merges English `<jobDescriptions>` automatically when an English-authored position returns empty descriptions on `?language=es` |
| **Recruitee (Tellent)** | `GET https://{slug}.recruitee.com/api/offers/` | `offers[]`: `id`, `title`, `published_at`, `location` / `city` / `country_code`, boolean `remote` / `hybrid` / `on_site`, `description` + `requirements`, structured `salary` (`min`, `max`, `currency`), `careers_apply_url` |
| **Teamtailor** | `GET https://{domain_or_slug}/jobs.rss` | `<channel><item>` with `xmlns:tt="http://teamtailor.com/rss"`: numeric ID from `<link>` (`/jobs/(\d+)`) or `<guid>`, `<title>`, RFC-1123 `<pubDate>`, `<tt:remoteStatus>` (`fully`/`temporary` → `remote`, `hybrid` → `hybrid`, `none` → `onsite`), `<tt:locations>`, `<tt:department>`, `<description>` |
| **Factorial HR** | `GET https://{slug}.factorialhr.es/` (fallback `.factorialhr.com`) | Parses embedded `application/ld+json` (`JobPosting`) and public `/job_posting/{slug_and_id}` cards (ignoring spontaneous `/apply` links), plus bounded detail page enrichment (`jobsinton.ats.factorial-detail-cap=6`) for Spain/EU eligible vacancies |
| **Greenhouse** | `GET https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true`<br>EU Fallback: `GET https://boards-api.eu.greenhouse.io/v1/boards/{slug}/jobs?content=true` | `id`, `title`, prefers `first_published` over `updated_at` (ISO 8601), `location.name`, `content` (double-decoded HTML → clean plain text), `absolute_url`. Automatically falls back to the EU cluster on HTTP 404 or empty `jobs[]` |
| **Lever** | Global: `GET https://api.lever.co/v0/postings/{slug}?mode=json`<br>EU: `GET https://api.eu.lever.co/v0/postings/{slug}?mode=json` | `id`, `text` (title), `createdAt` (epoch ms → ISO 8601), `categories.location` / `allLocations`, `workplaceType`, `descriptionPlain` + `lists[].content`, `applyUrl` / `hostedUrl`. Supports explicit `region: "eu"` or bidirectional Global ↔ EU fallback on HTTP 404 |
| **SmartRecruiters** | List: `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings?limit=100`<br>Detail: `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings/{id}` | `id`, `name`, `releasedDate`, `location` (`city`, `country`, `remote`), fallback apply URL `https://jobs.smartrecruiters.com/{slug}/{id}`. Prioritizes Spain/EU Remote postings up to `jobsinton.ats.smartrecruiters-detail-cap` during feed load and supports on-demand detail hydration (`GET /api/ats/smartrecruiters/{slug}/{id}`) |
| **Workable** | v1 widget: `GET https://apply.workable.com/api/v1/widget/accounts/{slug}?details=true`<br>v3 fallback: `POST https://apply.workable.com/api/v3/accounts/{slug}/jobs` | `shortcode`, `title`, `published_on`, `city` / `country` / `telecommuting`, `description` (HTML → plain text), `url` (fallback `https://apply.workable.com/{slug}/j/{shortcode}/`). Follows standard 302 redirects and falls back to v3 POST when v1 returns 404/410 or empty |
| **Ashby** | `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true` | `id`, `title`, `publishedAt`, `location` / `secondaryLocations`, `isRemote` / `workplaceType`, `descriptionPlain` (fallback `descriptionHtml`), `compensation.compensationTierSummary`, `applyUrl` / `jobUrl`. Sends browser-compatible `User-Agent` and skips `isListed == false` |

## Pre-seeded Spain & EU company registry (222 boards)

`backend/src/main/resources/ats/companies.json` ships with **222 pre-seeded company boards** spanning Madrid, Barcelona, Valencia, Málaga, Sevilla, Bilbao, and EU Remote tech employers across all 9 ATS platforms:

- **Personio (35):** Otto Group one.O Spain (OSP Spain), Paradigma Digital, Kaleidos (Penpot), Pasiona Consulting, XITASO Spain, rindus, TNG Technology Consulting, OKAPI:Orbits, Ultralytics, SENSIA Solutions, ECIX Group, Accumin, Astrafy, Caravelo, Bookboost, Ohpen, Farlabo, Tuio, Zaleos, STEMDO, Satlink, viastore GROUP, Mirai RiskTech, Plenit, YABA, Mercanis, GBTEC, Clicars, Fourvenues, Inconcert, Personio Madrid Hub, sennder Technologies, Holaluz, Trade Republic, Raisin
- **Teamtailor (26):** knowmad mood, Q-Tech, Kapres Technology, Next Digital, SDG Group Spain, Sacyr, Alsea España, Timestamp, CONVOTIS Iberia, Nextlane, Luzia, Ucademy, INNOCV Solutions, Multiverse Computing, Winid, Leadtech Group, Leyton Spain, Livo, Unleash, HORSE Spain, Endivo Consulting, OCA Global, Civitatis (`empleo.civitatis.com`), PcComponentes (`careers.pccomponentes.com`), BigBuy (`talent.bigbuy.eu`), Harbiz (`careers.harbiz.io`)
- **Recruitee (25):** ATG Europe (ESA / ESAC Madrid), etalytics, cbs Corporate Business Solutions Spain, DOSS2 Talent, Qualifyze, seQura, TransPerfect Tech, AIM Sport, Business Insights, Ready, Hostaway, Affidea Group, Tether, CyberArrow, Óscala, Zara Tech, SpotLab, Plukon Food Group Spain IT, Metyis Spain, LITIT, Making Science, Acid Tango, homming, Runroom, Tellent
- **Factorial HR (20):** Factorial HR, LedaMC, Embat, Grupo Noria (TIC KOBEX), Idrica, Occicor, ENCAMINA, Green Eagle Solutions, Interacso, Playtomic, Billin, FCM Digital, elDiario.es, KRUK España, Agentero, GameHouse Spain, Partium, The Keenfolks, Payflow, Wetaca
- **SmartRecruiters (23):** Devoteam Spain, Inetum Spain, T-Systems Iberia, Sopra Steria Spain, Scalian Spain, IODigital, Alter Solutions Spain, Miratech, SAP Spain, Bosch Group Spain, Nexthink Madrid Hub, Radisson Hotel Group IT Hub Madrid, Bertelsmann / Arvato Services Spain, Taxera Technologies, Colliers International Tech Hub Madrid, Bullhorn Consultants, Celonis, Visa Madrid, Amadeus IT Group Madrid, Eurofins Spain, Publicis Groupe / Sapient Spain, Econocom España, Avery Dennison Spain
- **Workable (23):** MediaRadar Madrid Hub, Treatwell Spain, Flexcompute, Codurance Spain, Plain Concepts, Frontiers Madrid, Teleperformance Spain Tech, Exoticca, Titan OS, Expereo, Unit8 Madrid, Destinus Madrid, Mondia Group Madrid, Trinetix, Toloka AI, Seedtag, ODILO, Internxt, Voicemod, Landbot, Declarando, Barkibu, Worldsensing
- **Ashby (18):** Docplanner (Doctoralia), Vic.ai Madrid, Air Apps Madrid, HappyRobot Madrid, Taxfix Madrid Hub, Pleo Madrid Hub, ElevenLabs, OpenAI Madrid, Sierra Madrid, Anyone AI, Everfield Spain, Perk Barcelona, Alan Spain, Joko, Invopop, PostHog, Supabase, Langfuse
- **Greenhouse (32):** BVNK, Affirm Spain, Thoughtworks Spain, Celonis Madrid Hub, Ebury Madrid, Fever, Okta Spain, Grafana Labs Spain, Netlify Spain, Scopely Spain, Appinio Madrid, Nuvolar, Mozilla Spain, Cabify, Clarity AI, Devo, CARTO, Tinybird, Job&Talent, RavenPack, Flywire, Glovo, Typeform, Wallbox, TravelPerk, Paack, Belvo, Freepik Company, Datadog Madrid Hub, Stripe Spain, Elastic Spain, Speechify
- **Lever (20):** Westernacher Consulting Spain, Prima Tech Hub Madrid, TomTom Madrid, OLX Group Spain, Finom, Creatio Madrid, XTB Madrid, CoinsPaid, Everseen Spain, Quadcode, Diabolocom Spain, Dun & Bradstreet, Spotahome, Lingokids, Capchase Madrid, Fintonic, Preply Barcelona, Qonto Spain, Spendesk, Stuart

Adding or removing a company requires editing `companies.json`.

## Backend architecture & resilience

All backend ATS code lives under `backend/src/main/java/io/github/garatech/jobsinton/providers/ats/`:

- **Parallel fault-isolated fetching (`AtsFeedService`):** Queries all registered boards concurrently using Java 21 virtual threads bounded by a configurable `Semaphore` (`jobsinton.ats.max-concurrency=18`). If an individual board times out, returns 404/429/5xx, or returns malformed JSON/XML, it is recorded in `boards[]` with `ok: false` and an explicit error summary without breaking the remaining boards.
- **Bounded HTTP transport (`JdkAtsHttpClient`):** Enforces a configurable per-request timeout (`jobsinton.ats.request-timeout-ms=8000`), sends a browser-compatible `User-Agent`, follows normal redirects (`Redirect.NORMAL`), caps streamed response payloads at 12 MiB, and validates every normalized `applyUrl` to require `https://`.
- **In-memory TTL cache & stampede coalescing:** Caches the aggregated `AtsFeedResponse` for `jobsinton.ats.cache-ttl-seconds=600` (10 minutes) and coalesces concurrent requests onto one in-flight fetch. Passing `?refresh=true` bypasses the cache.
- **First-Seen persistence & true freshness (`FirstSeenRegistry`):** Persists the earliest local sighting timestamp (`firstSeenAt`) per job ID in `data/ats-seen-jobs.json` (gitignored) and computes `hoursSincePublished` from `min(publishedAt, firstSeenAt)` so recruiter edits to old jobs (`updated_at`) never fake a `< 24h` badge.

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
- **Token-boundary keyword matching & Junior/Seniority scoring (`frontend/src/ats/scoring.ts`):** Matches technical terms cleanly without false substring hits (for example, `Java` does not match `JavaScript`, `RAP` does not match `bootstrap`, `CAP` does not match `capacity`, `SAP` does not match `asap`), boosts Junior / Graduate / Entry-level postings (`🎯 Junior / Entry Fit`, `+20` to `+30` pts), and penalizes Senior / Staff / Principal / Architect roles (`⚠️ Senior/Staff`, `-25` pts).
- **3-Tier Geographical Filter & Anti-"Fake Remote" Blocker (`frontend/src/ats/scoring.ts`):**
  - `🇪🇸 Spain Only` (`spain-only`): Madrid, Barcelona, Valencia, Málaga, Sevilla, Bilbao, Zaragoza, Spain, España, Iberia.
  - `🇪🇺 Spain + EU Remote` (`spain-eu-remote`, **default on launch**): Includes Tier 1 Spain roles plus EU/EMEA/CET remote roles while **strictly blocking Fake US/UK/LATAM/APAC Remote listings** (`US only`, `U.S. only`, `United States`, `Remote - US`, `North America`, `Canada`, `LATAM`, `Latin America`, `Brazil`, `Mexico`, `APAC`, `India`, `UK only`, `United Kingdom only`, `Australia`, `PST`, `EST`, `CST`, `must reside in the US`). Sorts Tier 1 (`Spain`) above Tier 2 (`EU Remote`) when relevance scores tie.
  - `🌍 Global` (`global`): Unfiltered worldwide view (`All Fetched Jobs` switches to `🌍 Global` automatically).
- **1-Click Job Triage (`frontend/src/pages/LiveJobsBoard.tsx`):** Every job card includes compact `⭐ Save`, `✅ Applied`, and `✕ Dismiss` actions persisted in `localStorage` (`jobsinton.ats-triage-v1`), paired with quick filter pills (`Hide Applied & Dismissed` on by default, `⭐ Saved`, and `View Applied`).
- **Freshness highlights:** Badges postings published within 24 hours (`🔥 Posted Xh ago`) and 25–48 hours (`⚡ Posted Xh ago`), displays live board health diagnostics, and links directly to each company's official career application page (`Apply on Company Site ↗`).

## Offline verification

Automated CI and local verification never depend on live third-party ATS servers:

- **Backend unit & MockMvc tests:** `mvn -B -ntp -f backend/pom.xml test` runs `HtmlTextCleanerTest`, `AtsAdaptersTest`, `JdkAtsHttpClientTest`, `AtsFeedServiceTest`, and `AtsControllerIntegrationTest` using deterministic fixtures and stub transports.
- **Frontend scoring & Playwright E2E tests:** `npm --prefix frontend run test:e2e` runs `ats-scoring.spec.ts` and `live-ats.spec.ts` (plus all existing desktop/mobile journeys) with deterministic stubbed `/api/ats/jobs` and `/api/ats/smartrecruiters/*` payloads.
