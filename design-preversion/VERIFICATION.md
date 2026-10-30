# Verification

The branch-specific **Verify Current preversion** workflow runs on relevant pushes to `design/current-preversion`.

It checks:

- Every internal page and asset reference, including fragment targets.
- Fourteen HTML pages at 320, 390, 768 and 1440 px in Chromium using `file://`.
- The Current background and heading family, plus horizontal overflow.
- Four full interactive journeys covering search, combined filters, ordering, both result layouts, cross-page saved offers, empty states, company discovery, profile validation and preview reset.
- Keyboard activation, visible title focus, dialog dismissal and focus recovery.
- Long profile values, literal text rendering and zero years of experience.
- Navigation with browser storage blocked and honest profile-save feedback in that state.
- Absence of network requests during the normal journeys and contrast for the main overview action in normal and hover states.

The workflow produces 60 screenshots: 56 page/viewport captures and four card-search captures. Evidence is retained for 14 days. The downloadable preview is retained for 30 days and is uploaded only after checks succeed.

A successful package includes `VERIFICATION_RESULT.json` with the tested commit, browser, transport and results. Consult the workflow result for the actual outcome; this document describes scope, not a claim that an untested revision passed.

## Limits

Local terminal provisioning was unavailable during preparation. Browser verification runs in GitHub Actions. Screenshots are review evidence; automated layout assertions do not establish visual approval.

Chromium is the verified browser. Other browsers and their local-file storage rules may differ. This is not a full accessibility audit. The preview has no real matching, authentication, applications, job feeds or backend persistence.

The original production frontend and backend are unchanged. No pull request is opened for this branch.
