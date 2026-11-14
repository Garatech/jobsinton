# Adzuna listing conversion

The backend can convert one object from an Adzuna search response into an `OpportunityListing`. This is an offline Java boundary: the caller passes a parsed JSON row and receives either a listing or a rejection reason. It has no credentials, network or persistence. The fictional catalog and public API continue to work as before.

The [local evaluation](https://github.com/Garatech/jobsinton/issues/31) found ten relevant, distinct results for Java in Madrid on 13 November 2026. All ten supplied basic listing metadata and a usable referral; none supplied salary. Nine previews left work mode unknown. That single page motivates separating discovery metadata from facts suitable for matching.

| Source | Conversion |
| --- | --- |
| ID, title and referral | Required. Prefix the source ID with `adzuna:`; trim the title. Retain an HTTPS referral on an accepted Adzuna host with a non-root path and no user info, nonstandard port or application credentials in query parameters. A rejected row has no partial listing. Links are not followed. |
| Company, location, creation time | Keep the company and location display names, falling back to the supplied area labels for location. Parse a timestamp with an offset to an instant. Missing or malformed optional fields stay unknown. Creation time is not expiry. |
| Description, category and contract | Keep the supplied excerpt without the terminal preview's length cap. The excerpt is still not a full advert. Category, full-time status and contract type describe the listing; none establish role, skills, residence or attendance requirements. |
| Salary | Keep positive numeric bounds as supplied, including one-sided ranges. Reject a malformed or inverted range without rejecting the vacancy. Prediction flag 1 means estimated, 0 means advertised, and other or absent flags leave provenance unknown. No currency, pay period or gross/net basis is assumed. |

For a Java vacancy labelled Madrid, the location is available for display even when the office, required attendance and accepted countries of residence are unknown. A salary without verified currency, period and basis does not enter the demo salary filter.

## Explicit matching facts (5A)

Only the listing title and its supplied excerpt feed the fact reader. The title must name one supported role exactly (for example, `Ingeniera de datos` or `Backend developer`). A complete Spanish or English declaration can establish work mode or a fixed count of weekly office days, independently. The reader checks the whole supplied excerpt, including text after the company introduction. It never follows the referral to fill gaps.

| Fictional source text | Facts available to the engine |
| --- | --- |
| Title: **Java developer**; location label: **Madrid**; company introduction only | Role, skills, office and attendance remain unknown. The search terms are never inputs. |
| **Modalidad: híbrida. Se requieren dos días presenciales por semana.** | Hybrid and two weekly office days. No office address or residence rule. |
| **Two days in the office per week required.** | Two office days; mode unknown. |
| **Trabajo 100% remoto.** | Remote; no numeric schedule or geographic permission inferred. |
| **Modelo flexible.** / **Hasta dos días presenciales por semana.** / **Dos días presenciales por…** | No fixed weekly attendance. A maximum, an option or a fragment is not a requirement. |
| **Trabajo 100% remoto. Dos días presenciales por semana.** | The schedule conflicts: both mode and count remain unknown. An independently recognised title is retained. |

This is a small set of supported declarations, not a general Spanish or English parser. Unrecognised wording, including many valid job titles, will often leave a field unknown. A competing count, a different mode, a schedule qualifier in the title or an unrecognised schedule clause discards the schedule rather than selecting the first match. Skills, years of experience, office location and residence restrictions are not extracted in this increment. The local search response uses its existing `matchingFacts` field; no profile is sent, no comparison is performed, and the search interface is unchanged.

## Verify from the repository root

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=AdzunaMatchingFactsTest,AdzunaListingMapperTest" test
mvn -B -ntp -f backend/pom.xml verify
```

The focused tests use invented JSON and exercise the real matching engine. No Adzuna keys, running servers or frontend build are needed. The separate [local search profile](ADZUNA_SEARCH.md) handles transport and page-level responses.
