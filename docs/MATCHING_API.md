# Matching API and opportunity comparison

Opportunity details now use the Java [matching engine](MATCHING_ENGINE.md) to compare the last saved profile with the selected fictional listing. The existing Current panel shows known comparisons and keeps unknown or unevaluated conditions in a disclosure.

No score, ranking, automatic exclusion, account or server-side profile storage is introduced.

## Request and response

`POST /api/matching/evaluate` accepts JSON with exactly two fields:

```json
{
  "opportunityId": "backend-java",
  "profile": {
    "role": "backend",
    "technologies": ["Java", "Spring Boot"],
    "experienceYears": 3,
    "country": "Spain",
    "city": "Madrid",
    "workMode": "remote",
    "maxOnsiteDays": null
  }
}
```

The profile uses the existing seven-field browser contract. The server validates required fields, types, numeric ranges, technology count/length/uniqueness and conditional office-day limits. Numeric strings and fractional years are rejected. Remote maximums remain null; hybrid/on-site maximums must be integers from 1 to 5.

The server reads at most 16,385 bytes and rejects a body above 16,384 bytes before JSON parsing, including requests without Content-Length. Malformed JSON, trailing JSON values and duplicate keys are rejected. Additional profile/envelope fields are not accepted.

On success, HTTP 200 returns:

```text
{
  apiVersion: "matching-api-v1",
  opportunityId: <requested ID>,
  evaluation: {
    rulesVersion: "matching-v1.0",
    constraintSummary: <CONFLICT | NEEDS_INFORMATION | NO_KNOWN_CONFLICT | NOT_ASSESSED>,
    assessments: [...]
  }
}
```

Each assessment retains criterion, dimension, kind, outcome, reason, explanation and evidence from the domain contract. Enums use uppercase names. Evidence contains compared profile/opportunity values and a limitation. The response does not return the raw saved profile, but its evidence may contain compared profile facts.

Application responses use Cache-Control: no-store. The frontend also disables request caching and does not persist or log comparison responses.

| Status | Application error code | Meaning |
| --- | --- | --- |
| 400 | INVALID_JSON | Malformed, duplicate-key, trailing, empty or unreadable JSON |
| 413 | REQUEST_TOO_LARGE | Body exceeds 16 KiB |
| 422 | INVALID_REQUEST | Incorrect envelope or opportunity identifier format |
| 422 | INVALID_PROFILE | Saved-profile contract rejected |
| 422 | INVALID_FACTS | Domain validation rejected the normalized facts |
| 404 | OPPORTUNITY_NOT_FOUND | No matching entry in the fictional catalog |

Application errors contain code, message and an errors array, with field-specific validation errors where available. They never include an evaluation or parser stack trace. Unsupported methods/content types use Spring's normal 405/415 handling. No CORS expansion is added; the existing Vite /api proxy remains the local request path.

## Catalog and profile adapters

The request carries an opportunity ID rather than client-supplied requirements. FictionalMatchingCatalog owns six explicit normalized listings, using the [documented mapping](MATCHING_ENGINE.md#fictional-catalog-mapping). It is a temporary backend companion to the frontend's display catalog. A browser-suite contract check compares every listing's role, technologies, mode, weekly office days, city and minimum years against the backend response to detect drift.

Every current listing has an unknown residence policy. Existing country labels identify display locations, not permission to work from a particular country. Experience scope remains technology-specific, role-specific or unknown, as documented; total profile years do not confirm those requirements.

SavedProfileAdapter resolves these explicit case-insensitive country names/codes:

| Canonical country | Recognized input |
| --- | --- |
| ES | ES, Spain, España |
| DE | DE, Germany, Alemania |
| FR | FR, France, Francia |
| PT | PT, Portugal |
| IT | IT, Italy, Italia |
| IE | IE, Ireland, Irlanda |
| GB | GB, UK, United Kingdom, Reino Unido |

City resolution is limited to Madrid, Valencia/València and Barcelona when the country resolves to ES. Whitespace is normalized. Other valid free-text places remain unresolved, not invalid or guessed. This is a limited fictional-catalog adapter, not a geocoder or a complete European place directory.

## Data handling and interaction

The profile remains stored only in browser localStorage. Opening an opportunity with a saved profile sends that snapshot to the configured backend for a transient comparison. Saving or editing a profile alone sends no matching request. The application adds no profile database or request-body logging; comparisons are not synchronized between devices.

Only successful saves update the shared snapshot. Drafts and failed saves keep the prior comparison input. Clearing the saved profile removes the comparison and restores the setup link. Existing profile tabs still use last-save-wins behavior and need a reload to see another tab's saved profile; live cross-tab profile synchronization is not introduced.

Without a valid saved profile, no comparison request is made. With a profile, the panel shows loading, the complete validated result, or an error with Retry comparison. A request has an eight-second deadline including response reading. Browsing and shortlist actions remain available during errors.

Requests are cancelled on navigation, profile replacement or retry. An active-request guard rejects late completions even if cancellation is ignored. A profile/offer identity key resets the panel before it can render a previous identity's result. Retries explicitly restore loading.

The client checks API/rules versions, requested offer ID, outcome/reason values, evidence shapes, required criteria and duplicate criteria before rendering. A malformed or incompatible response becomes recovery feedback, never a successful comparison.

Known comparisons use the existing panel and list styles. Technologies in common are grouped; unknown skills retain their actual listing names. Unknown and unevaluated conditions remain under the disclosure. The constraint summary does not hide offers or establish professional suitability or work authorization.

## Verification after merge

Update a clean checkout after merging, then stop old development servers so tests use the current code. From the repository root:

```powershell
mvn -B -ntp -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run build
npm --prefix frontend run test:e2e
```

Use the existing [development guide](DEVELOPMENT.md) if Chromium or browser dependencies need installing. To run only the new checks:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=MatchingApiIntegrationTest" test
npm --prefix frontend run test:e2e -- matching.spec.ts
```

The implementation PR records the tested commit and actual CI results. Existing domain and browser coverage remains part of the full workflow. Screenshots are review aids; automated checks are not a complete accessibility or visual audit.

## Manual acceptance checklist

Start the backend and frontend using DEVELOPMENT.md. Use fictional profile data.

- [ ] Without a saved profile, open an opportunity: How it lines up offers profile setup.
- [ ] Save Backend, Java, Spring Boot, 3 years, Spain, Madrid and Remote. Open Backend Developer: the panel lists both shared technologies.
- [ ] Expand the unknown section: Java-specific experience and residence policy remain unknown; salary remains unevaluated.
- [ ] Change the profile to Hybrid with a maximum of 1 office day, and save. Open Data Engineer: two required days produce a stated-limit conflict; residence remains unknown.
- [ ] Edit preferences without saving, then revisit the offer: the comparison still uses the previous saved snapshot.
- [ ] Save the new preferences and return: the comparison updates. Clear the profile and return: the setup link replaces the result.
- [ ] Stop the backend and reopen a detail page: loading resolves to an error or timeout. Saving the offer still works. Restart the backend and select Retry comparison.
- [ ] Navigate quickly between offers and use Back/Forward: no earlier offer's result appears in the current one.
- [ ] Check the panel on a narrow screen and with keyboard navigation. The disclosure and retry button remain usable, with no horizontal overflow.

## Limits and next step

The catalog is fictional, place normalization is deliberately limited, and profile transmission is transient. The existing rules and data gaps are unchanged. There is no claim of hiring probability or employer eligibility.

After owner acceptance of this integration, the next proposed increment is a reviewed ranking specification: weights, information coverage and tie-breaking, with reference examples before implementation. Providers, expanded preferences, accounts and persistence remain separate milestones.

Reverting the integration restores local descriptive details. It requires no profile-storage migration; the pure domain engine remains independently usable.
