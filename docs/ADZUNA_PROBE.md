# One local search: Java in Madrid

This trial answers one question: does the Spanish search return a relevant Java vacancy with enough information to identify it and refer a reader to it? One invocation sends at most one request for up to ten results. The website, ranking and fictional catalog do not use the response.

## Run from the repository root

Use Node 24 in an interactive VS Code PowerShell terminal. No server or package installation is needed. Run the offline checks first:

```powershell
node --test tools/adzuna/probe.test.mjs
```

For the live evaluation, use a [developer account](https://developer.adzuna.com/signup) with the free plan and an available request. Check the assigned quota and [source decision](PROVIDERS.md). If access would cost money, stop here and record that as the outcome.

```powershell
node tools/adzuna/probe.mjs
```

1. Type `YES` to confirm the plan and quota. Enter the application ID and key at the hidden prompts; neither will echo. The tool also accepts existing process variables `ADZUNA_APP_ID` and `ADZUNA_APP_KEY`. Do not include values in commands, configuration files, screenshots or issues. Use a private terminal without a transcript, debugger or network tracing.
2. The request searches page one of Spain for `Java` in `Madrid`. Redirects, timeouts and quota errors stop the run. The tool never follows a referral, fetches details, paginates or retries.
3. Review each short preview in the terminal. Enter three values separated by spaces: relevance (`y`, `n`, `u`), explicit mode (`r` remote, `h` hybrid, `o` on-site, `u` unknown), and stated office days per week (`0`–`5` or `?`). For example, `y h ?` means a relevant Java job in Madrid that says hybrid but gives no weekly schedule. JavaScript alone is not Java; a Madrid location alone does not establish on-site work. If the excerpt is inconclusive, use unknown. Enter alone marks all three facts unknown.
4. Copy **only the final JSON** into the evaluation issue. Prompts and previews use standard error; the aggregate uses standard output. Do not share the terminal transcript or any listings. No file is written by the tool.

## Read the result

- `returned`, `distinctIds` and `duplicateRows` distinguish rows from identifiable vacancies. `missing` counts rows with absent or unusable information. A usable referral here means a syntactically accepted HTTPS Adzuna listing link; its destination is not opened or verified.
- `relevantSoftwareInMadrid`, `workMode` and `explicitWeeklyDays` describe the manual review. The last is a count of rows with an explicit number, including zero. These checks say nothing about accepted countries of residence.
- `salary` separates advertised, predicted, unknown-origin, absent and invalid bounds. No currency, period or gross basis is inferred. A salary marked as not predicted is counted as advertised, not independently verified.
- `usableRelevantDistinct` counts distinct IDs for which the **same row** was judged relevant and supplied both a title and an accepted referral. At least one makes the source a candidate for connector design. Review the missing fields before making that decision; the flag alone is not approval for public display.
- A successful empty response records zero results. An error records an outcome and the HTTP status if one arrived, without the URL or response body. A 401/403, 429 or failed search is a reason to record the blocker, not to repeat the request automatically.

**Observation status:** completed; the [aggregate observation](https://github.com/Garatech/jobsinton/issues/31) supported [offline connector development](ADZUNA_MAPPING.md). The offline fixtures verify behavior only. The [source decision](PROVIDERS.md) lists the conditions that remain open before public use.
