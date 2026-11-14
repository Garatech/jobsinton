# Evidence coverage on one Adzuna page

This local study answers a narrow question: would the existing ranking policy distinguish the listings on one returned page for the same reference profile? It produces aggregate counts, not an ordered list or a recommendation to enable sorting. Search and the comparison cards keep their existing behaviour.

## Run from the repository root

Use Java 21 and PowerShell. Compile the current domain classes and check the supplied fictional page first:

```powershell
mvn -B -ntp -f backend/pom.xml compile
.\tools\adzuna\Test-PageStudy.ps1
```

Expect `Page study checks passed.` No server or credentials are required. Maven may download build dependencies on first use; the measurement itself is offline.

If `$result` already contains a complete response from JobSinton's local Adzuna search, run:

```powershell
.\tools\adzuna\Measure-Page.ps1 -Page $result
```

The response must include the mapped `matchingFacts` for each accepted listing. The original aggregate probe report is not a page. The measurement does not need a valid comparison reference or a running backend, so an earlier response still held in memory is usable after a restart.

If the search is already visible in a browser and its request is in the Network panel, select the completed `POST /api/providers/adzuna/search` for the page you want, copy its **response** and run `$result = Get-Clipboard -Raw | ConvertFrom-Json` in PowerShell. Do not copy a comparison response or a request as a command. If neither the response nor its request history is available, use the next search you intentionally make; the tool cannot recover a closed browser page. Share only the JSON printed by the tool, not the clipboard or `$result`.

## What is measured

Version 1 uses a fixed fictional backend profile: Java, two years of total experience, Madrid (Spain), hybrid, at most **one** office day per week. Using the same profile makes pages comparable. It is not read from the browser and makes no claim about other profiles. The adapter passes only role, work mode and fixed office days from the mapped facts to the Java domain. It does not read advert text to guess missing values. The remaining listing facts are unknown in 5A; if those fields become populated the study must be reviewed. IDs are checked for duplicates in memory; neither IDs nor advert contents reach the Java command or report.

| Field | Meaning |
| --- | --- |
| `listings` | Distinct accepted listings on this page, excluding rejected or duplicate source rows. This is the denominator for coverage. |
| `comparable` | Listings with usable role, mode and weekly attendance evidence, counted separately. A stated difference still counts as evidence. |
| `attendance` | Within the one-day limit, exceeds the limit, or unknown. The three counts add up to `listings`. |
| `explicitConflicts` | Listings for which the engine confirms a constraint conflict. Unknown is never treated as a pass or a conflict. |
| `withoutComparableFacts` | None of the three measured dimensions could be compared. |
| `ties` | Listings tied with at least one other listing, tied unordered pairs, and all possible pairs on the page. A group of three contributes three listings and three pairs. Empty and single-row pages have zero possible pairs. |

Ties use the [existing ranking policy](RANKING.md): conflict grouping, availability of advisory evidence, exact net support and coverage. The final identifier tie-break is deliberately excluded: an identifier does not explain why a person should read one offer first. A confirmed within-limit schedule and an unknown schedule can tie if their other evidence is identical: the policy demotes confirmed conflicts, but does not award a bonus for meeting a constraint. The attendance counts preserve this distinction.

For example, the offline fixture has six listings. Three have no comparable facts; one exceeds the weekly limit. Five listings are tied with at least one other listing, producing four tied pairs out of fifteen. Those numbers are a test result, not observations about real adverts.

## Ordering decision (5D.2)

Keep Adzuna’s order and the existing **Compare with my profile** action. Leave compatibility sorting available in the fictional catalog only.

On 14 November 2026, the page study was run on a real first page with the reference profile above (`adzuna-page-v1`, `matching-v1.0`, `ranking-v1.0`). Repeating the measurement locally verified the same page; it did not add a second sample.

| Observation | Result |
| --- | ---: |
| Distinct listings measured | 10 |
| Comparable role / work mode / weekly attendance | 0 / 0 / 0 |
| Attendance within limit / exceeds limit / unknown | 0 / 0 / 10 |
| Confirmed constraint conflicts | 0 |
| Listings without comparable facts | 10 |
| Listings tied / tied pairs out of possible pairs | 10 / 45 of 45 |

For every pair the ranking policy reached the identifier fallback. Sorting would change, at most, the sequence of IDs; it would give the reader no profile-based reason to open one listing before another. Zero conflicts means none could be confirmed, not that ten listings met the limit.

These counts describe the facts the mapper recognised, not everything a reader could learn from the adverts. A manual check found an on-site indication in an excerpt that the current mapper left unknown. The accompanying working hours do not establish a fixed weekly office-day requirement. A Java title alone does not establish a backend role either. Improving recognition of explicit wording is a separate change from deciding how to order listings.

**When to revisit:** measure an already-returned page from a different search with the same reference profile. Keep each page’s denominator and ties separate; pages may overlap. For any pairs the policy distinguishes, check that the original wording supports an understandable difference, rather than one listing merely providing more text. If that pattern recurs across searches, reconsider an optional sort within the current page. Until then the provider order remains the default and only live order.
