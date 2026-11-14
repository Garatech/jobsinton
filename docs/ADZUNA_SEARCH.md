# Local Adzuna search

The Java backend can search one page of Spanish vacancies and convert each row with the [listing mapper](ADZUNA_MAPPING.md). The normal application has no provider endpoint and the demo continues to use its fictional catalog. The local profile does not fetch anything at startup. There is no live CI step.

## Verify without keys

From the repository root in PowerShell:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=Adzuna*Test" test
mvn -B -ntp -f backend/pom.xml verify
```

The tests use invented responses and a loopback HTTP server. They cover the normal application, local endpoint, original row mapper, provider failures, oversized bodies and read deadlines.

To check accented text without keys or quota, run the focused HTTP tests from the repository root:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=AdzunaLocalApiTest" test
```

A fictional provider returns `¡España! Certificación en A Coruña…`. The test checks the response charset and decodes the bytes. On Windows, a second check reads the same excerpt with Windows PowerShell's `Invoke-RestMethod` and compares the string in memory. Neither check uses Adzuna. The endpoint now declares UTF-8 in the **response**; setting `-ContentType` on a request does not set the response encoding. Previously fetched PowerShell variables are not changed.

## Verify the browser without keys

From the repository root, with Node 24, Java 21 and Playwright Chromium installed:

```powershell
npm --prefix frontend ci
npm --prefix frontend run test:adzuna
```

The command builds the interface and starts its own local backend on port 8081. Desktop and mobile tests exercise the real same-origin route without credentials, then use fictional pages for display, errors and pagination. Stop a running Vite process before reinstalling dependencies on Windows.

## Optional: run one live search on Windows

Use a private PowerShell terminal with Java 21, Maven, a free evaluation account and available quota. Stop any backend already using port 8080 with Ctrl+C in its own terminal. For the browser, build the frontend once from the repository root with `npm --prefix frontend run build` (the browser test above already does this). Maven runs the backend from the backend module and serves `../frontend/dist`. No Vite terminal is needed. A CLI-only check does not need the build. Keep this developer session on the local machine; do not forward its port, run it behind a proxy or enable network/debug tracing.

**Terminal 1, repository root:** the prompts hide both values and keep them out of command history. The environment variables belong to this terminal and are removed at the end. No project file needs credentials.

```powershell
try {
    $env:ADZUNA_APP_ID = [System.Net.NetworkCredential]::new('', (Read-Host 'Adzuna app ID' -AsSecureString)).Password
    $env:ADZUNA_APP_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Adzuna app key' -AsSecureString)).Password
    mvn -f backend/pom.xml "-Dspring-boot.run.profiles=adzuna-local" spring-boot:run
} finally {
    Remove-Item Env:ADZUNA_APP_ID, Env:ADZUNA_APP_KEY -ErrorAction SilentlyContinue
}
```

**Browser:** open [local opportunities](http://127.0.0.1:8080/#opportunities). The sample catalog is selected. Choose **Adzuna · Spain**, enter a role or keyword and a location, and press **Search Adzuna** once. Opening the page, switching source, typing or expanding an excerpt makes no provider request. A page button makes one new request. A full page allows asking for another; it does not guarantee more vacancies.

Each result links to the provider referral and credits Adzuna. Unreported work mode and salary remain explicit. Predicted salaries are not published; advertised figures do not imply a verified currency or pay period. No live result enters the demo shortlist, compatibility ranking or browser storage. Returning to the sample discards live results. Local preview does not change the conditions for any future public display.

**PowerShell alternative:** to make the same search without the browser, wait for Spring Boot to start and run this in Terminal 2. Use either method if checking a single request.

```powershell
$query = @{ what = 'Java'; where = 'Madrid'; page = 1 } | ConvertTo-Json -Compress
$result = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/providers/adzuna/search' -ContentType 'application/json; charset=utf-8' -Body $query
$result | Select-Object source, page, received, rejected, duplicates
$result.listings | Select-Object id, title, locationLabel
```

Read excerpts locally with `$result.listings | Select-Object -ExpandProperty excerpt`; this reads the existing response and consumes no quota. The result is not stored by the backend. Do not publish listings or credentials. Stop Terminal 1 with Ctrl+C; start the backend normally for the fictional demo.

## Request and response

POST JSON with nonblank `what` and `where` (at most 100 characters each); optional `page` defaults to 1 and accepts 1–5. The market is fixed to Spain, with at most ten rows per page. The profile binds to IPv4 loopback. Local CLI requests and browser requests from the same local Spring origin are accepted. Other origins are rejected. Vite only proxies the status and matching routes. GET /api/providers/adzuna/availability reports the local feature without contacting the provider or exposing credential state. No search terms come from a saved profile. Each call requests only the selected page, with no application retries, redirect following, scheduled polling or follow-up requests.

On success, `received` counts all upstream rows. `rejected` counts rows the mapper could not use, and `duplicates` counts repeated accepted IDs within this page. The first accepted occurrence wins. The remaining `listings` retain provider order; an empty result is a successful response. No cross-page deduplication or expiry is inferred. A creation time alone does not prove an offer is still open.

| HTTP | Meaning |
| --- | --- |
| 400 | Invalid request, terms or page; no provider call. |
| 403 / 404 | Request not local or has a different origin / local profile not active. |
| 503 | Missing backend credentials; no provider call. |
| 429 | Provider quota or rate limit. Stop and check the account. |
| 502 | Provider denied access, returned an unexpected status or invalid/oversized data, or could not be reached. The error code distinguishes these cases. |
| 504 | The provider did not finish within the ten-second deadline. |

Responses returned by the search handler use `Cache-Control: no-store`. Provider error bodies and authenticated request URLs are not returned. This private evaluation does not enable public display, persistence or adding live results to the shortlist; see the [provider decision](PROVIDERS.md).
