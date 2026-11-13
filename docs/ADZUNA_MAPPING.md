# Adzuna listing conversion

The backend can convert one object from an Adzuna search response into an `OpportunityListing`. This is an offline Java boundary: the caller passes a parsed JSON row and receives either a listing or a rejection reason. It has no credentials, network or persistence. The fictional catalog and public API continue to work as before.

The [local evaluation](https://github.com/Garatech/jobsinton/issues/31) found ten relevant, distinct results for Java in Madrid on 13 November 2026. All ten supplied basic listing metadata and a usable referral; none supplied salary. Nine previews left work mode unknown. That single page motivates separating discovery metadata from facts suitable for matching.

| Source | Conversion |
| --- | --- |
| ID, title and referral | Required. Prefix the source ID with `adzuna:`; trim the title. Retain an HTTPS referral on an accepted Adzuna host with a non-root path and no user info, nonstandard port or application credentials in query parameters. A rejected row has no partial listing. Links are not followed. |
| Company, location, creation time | Keep the company and location display names, falling back to the supplied area labels for location. Parse a timestamp with an offset to an instant. Missing or malformed optional fields stay unknown. Creation time is not expiry. |
| Description, category and contract | Keep the supplied excerpt without the terminal preview's length cap. The excerpt is still not a full advert. Category, full-time status and contract type describe the listing; none establish role, skills, residence or attendance requirements. |
| Salary | Keep positive numeric bounds as supplied, including one-sided ranges. Reject a malformed or inverted range without rejecting the vacancy. Prediction flag 1 means estimated, 0 means advertised, and other or absent flags leave provenance unknown. No currency, pay period or gross/net basis is assumed. |

For a Java vacancy labelled Madrid, the location is available for display even when the office, required attendance and accepted countries of residence are unknown. The mapper does not extract requirements from free text. The existing matching engine receives those fields as unknown; it does not treat them as conflicts. A salary without verified currency, period and basis does not enter the demo salary filter.

## Verify from the repository root

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=AdzunaListingMapperTest" test
mvn -B -ntp -f backend/pom.xml verify
```

The focused tests use invented JSON and exercise the real matching engine. No Adzuna keys, running servers or frontend build are needed. Step 3B will add optional backend transport and handle page-level responses; this increment adds no route for live searches.
