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

For step 5D.2, review the real page size, coverage, explicit conflicts and tied pairs together. Many ties or mostly unknown facts favour keeping provider order. Few ties alone do not justify sorting if they rest on sparse evidence. Different pages can contain overlapping vacancies; do not add their counts as though they were unique across a market.
