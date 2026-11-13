# First live source: Spain

Source selected for local evaluation · 13 November 2026 · Personal portfolio, EUR 0 budget.

**Select Adzuna's Job Search API for one local evaluation.** It documents Spain, keyword/location search and a free evaluation key. This answers which source to test first. It does not establish Spanish vacancy volume, approve a permanent public demo, or activate a provider. The running application and CI still use fictional opportunities. No account was created, terms accepted, credentials inspected, provider contacted or live search performed in this review.

## Why this source

| Source | Spain and matching evidence | Access, display and limits | Decision |
| --- | --- | --- | --- |
| **Adzuna Job Search** | Its [published schema](https://developer.adzuna.com/swagger/spec/test2.json) accepts `es`, keywords, location and page number. Its [search example](https://developer.adzuna.com/docs/search) includes useful listing metadata but only a description excerpt. Spain-specific relevance and completeness have not been measured. | The [API terms](https://developer.adzuna.com/docs/terms_of_service) describe a free evaluation key without payment details; the account shows the applicable limits. Publishing adverts is a listed use with per-ad attribution. | **First local trial.** Enough evidence for a small evaluation; continuing service, retention and the intended portfolio display need a separate decision. |
| **InfoJobs** | Its [search](https://developer.infojobs.net/documentation/operation/offer-list-9.xhtml) supports geography and teleworking filters. [Detail](https://developer.infojobs.net/documentation/operation/offer-get-7.xhtml) documents skills, salary, residence and experience fields. A [Spanish technology search](https://www.infojobs.net/ofertas-trabajo/informatica-telecomunicaciones/j2ee) illustrates relevance, not API yield. | [Application registration](https://developer.infojobs.net/documentation/quick-start/index.xhtml) is required. The published [API terms, prohibited uses](https://developer.infojobs.net/legal/legal/terms-of-use.xhtml) explicitly exclude job aggregators. Limited caching language does not override that restriction. | **Do not integrate under the published terms.** Strong fields cannot compensate for an incompatible use. Reconsider only with explicit project-specific permission; no quota or price assumed. |
| **Tecnoempleo** | Its [Spanish IT portal](https://www.tecnoempleo.com/) targets the intended sector. The provider [describes an XML/JSON feed](https://blog.tecnoempleo.com/candidatos/2022/7162/tecnoempleo-com-se-integra-con-tu-herramienta-de-recursos-humanos/) for displaying its offers on other sites, separately from employer recruiting tools. | Access is by arrangement. The reviewed offer of integration does not establish a EUR 0 developer plan, guaranteed fields, quota, attribution, cache lifetime or ranking rights. Free candidate access is a different service. | **First Spain-specific alternative.** Request a sample schema and conditions if the Adzuna trial fails. A widget alone would not supply structured inputs for the ranker. |
| **Remotive public API** | The [maintainer's documentation](https://github.com/remotive-com/remote-jobs-api) includes software jobs and a candidate-location field. Remote-only inventory cannot cover the project's hybrid and on-site journeys. A worldwide listing is not evidence of a Madrid office. | Its [public API conditions](https://remotive.com/remote-jobs/api) require credit and a link to Remotive, delay listings by 24 hours, and restrict third-party redistribution and signup gates. The maintainer recommends at most four fetches a day. | **Remote-only fallback** if that narrower scope is acceptable; do not silently replace the Spain-wide catalog with it. |

The recommendation concerns the public Job Search product, not Adzuna Intelligence or a paid feed. An API country selector proves a market is supported, not how many relevant jobs an app can retrieve today.

## Conditions for JobSinton

- **Local evaluation:** review the [registration form](https://developer.adzuna.com/signup) and [API terms](https://developer.adzuna.com/docs/terms_of_service) before registering. Describe the actual portfolio use. Confirm the free plan and record its quota privately; do not copy historical third-party quota figures into the connector.
- **Public display:** Adzuna specifies linked attribution on each displayed ad, at least 116 × 23 pixels. Use the supplied referral URL for the vacancy. Estimated salaries have separate attribution; omit estimates from the initial display. The terms distinguish evaluation from ongoing use and impose additional conditions on organizational and revenue/lead-generating uses. Do not infer indefinite free hosting rights from receiving a key.
- **Retention:** the API terms require removal of acquired data from site pages on termination. No cache lifetime or persistent shortlist licence for this project was established. Use in-memory processing for the trial, commit no response bodies, and leave the public demo fictional until refresh, expiry, withdrawal and storage rules are clear. Recheck the general conditions linked from the API terms; that separate page could not be retrieved during this review.

## What the first response could actually support

| Returned fact | Proposed treatment |
| --- | --- |
| Listing ID, title, company, creation timestamp, referral URL | Namespace IDs by provider; accept only valid links and dates. A creation timestamp is not an expiry date. Missing company information must not become a fictional employer. |
| Location label / area and category | Retain the stated location. A city match is not a residence restriction. An IT category or a hit for “Java” is not proof of a backend role or a required skill. |
| Salary minimum/maximum and prediction flag | Keep absent bounds unknown. Separate estimates from advertised figures. Do not apply the existing EUR gross/year filter until currency, period and basis are established. |
| Description excerpt, contract time/type | An excerpt can omit requirements. Full-time is not on-site. Do not infer technology-specific tenure, mandatory skills, work mode, regular office days or accepted residence countries from missing fields. |

The present display catalog expects complete facts such as salary and office days; a live adapter must support missing values rather than substituting zero. Preserve the existing [matching semantics](MATCHING.md) and [ranking policy](RANKING.md): an offer with little information can be shown without claiming a high-confidence fit.

## Next step: one local probe, not a connector

1. Obtain the free credentials through the provider and confirm the assigned limits. Keep both values on the backend machine. Adzuna uses query-string credentials: do not log the complete URL or paste it into an issue, screenshot or browser.
2. Make **one** HTTPS GET to `api.adzuna.com/v1/api/jobs/es/search/1`, with `what=Java`, `where=Madrid`, `results_per_page=10`, the required application credentials and an `Accept: application/json` header. Use a fixed search, not a saved personal profile. No scheduled polling, detail fan-out or live CI call.
3. Record a redacted observation: date, market, HTTP status, number returned, number relevant to software work in Madrid, distinct IDs, safe referral links, missing fields and advertised versus predicted salary counts. Check examples of remote/hybrid/office evidence separately. Zero hits is a valid observation; it does not establish that Spain has no vacancies.
4. **Proceed to connector design only if** access is granted at EUR 0, the response contains at least one relevant vacancy with usable ID, title and referral link, and the observed fields can be represented honestly. If credentials fail, a quota is exhausted, payment is required or results are irrelevant, record the blocker instead of adding a connector. A useful private trial still does not authorize a public rollout.
5. Before public use, resolve the intended duration, attribution, ranking/display conditions and cache/shortlist retention for this use. Until then keep the existing fictional journey available. A later connector PR would cover pagination, bounded requests, deduplication, expired listings and offline tests using authored fixtures.

**Run the experiment:** [Local commands and review guide](ADZUNA_PROBE.md). The authenticated observation is pending.
