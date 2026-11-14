# Compare a returned Adzuna listing

A local search returns a `comparisonRefs` object alongside `listings`. Its keys are the returned listing IDs. A value is an opaque reference to that listing's [explicit facts](ADZUNA_MAPPING.md#explicit-matching-facts-5a) at search time. Only the selected reference and the last saved seven-field profile are needed for a comparison. The server does not fetch the advert again, store it or send the profile to Adzuna.

## Contract

`POST /api/providers/adzuna/compare` accepts exactly `comparisonRef` and `profile` as JSON. Use the `adzuna-local` backend on loopback; browsers must use its own origin. The default backend has no such route. The response uses the existing [matching response and reason codes](MATCHING_API.md#request-and-response), with the original `adzuna:` ID. Success and application errors use `Cache-Control: no-store`. The search card calls this endpoint only when the reader asks to compare; provider order is unchanged.

The reference binds the ID and facts together with a server-generated signature. It contains no profile, excerpt, credentials or referral. It is signed, not encrypted. It expires two hours after issue or when the backend restarts. This is the lifetime of the reference, not the advert. If a provider ID is longer than 256 characters including its prefix, that listing is still shown but has no reference. Clients should only offer comparison when a reference exists.

Malformed, duplicate-key or trailing JSON returns 400; a body over 16 KiB returns 413. An invalid saved profile returns 422 `INVALID_PROFILE`. A missing field, extra listing facts or an incorrect envelope returns 422 `INVALID_REQUEST`. An altered, expired or otherwise unusable reference returns 422 `INVALID_COMPARISON_REF`; the user can explicitly search again. The endpoint does not retry or refresh references.

## Verify from the repository root

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=AdzunaComparisonApiTest,AdzunaComparisonRefsTest,AdzunaDisabledApiTest" test
```

The HTTP test searches a fictional provider once and selects two different returned rows. Two required days against a one-day maximum yields `ATTENDANCE_EXCEEDS_LIMIT`; changing the maximum to two yields `ATTENDANCE_WITHIN_LIMIT`. An introductory excerpt produces `ATTENDANCE_UNKNOWN`. The provider call count stays at one. No running server or credentials are needed.

## Inspect a result in PowerShell

With the local backend already running and `$result` from a search against **that same running backend**, select a listing and use the fictional saved-profile example below. This comparison does not spend search quota.

```powershell
$listing = $result.listings | Select-Object -First 1
$ref = $result.comparisonRefs.PSObject.Properties[$listing.id].Value
$profile = @{ role = 'backend'; technologies = @('Java'); experienceYears = 2
              country = 'Spain'; city = 'Madrid'; workMode = 'hybrid'; maxOnsiteDays = 1 }
$body = @{ comparisonRef = $ref; profile = $profile } | ConvertTo-Json -Depth 5
$comparison = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/providers/adzuna/compare' -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
$comparison.evaluation.assessments | Where-Object criterion -eq 'attendance' | Format-List outcome, reason, explanation
```

A real excerpt often lacks an exact schedule. In that case an unknown result is expected.

## Verify the search card

From the repository root, run `npm --prefix frontend run test:adzuna`. It builds the client and checks desktop and mobile against the local backend with fictional search results. No provider credentials are needed.

For a manual check, use the [local Adzuna setup](ADZUNA_SEARCH.md) and save a profile on the same origin as the search. On page two, expand an excerpt and choose **Compare with my profile**. The explanation opens inside that card. Switch languages or hide and reopen the result: the page, excerpt and comparison stay in place without another request. A fixed weekly requirement is shown alongside the saved limit; missing attendance stays unknown. Without a saved profile the card links to setup. An expired reference requires an explicit new search. Results are kept only while the current page is displayed.
