# Client languages

The header offers Español and English. On first visit the client uses the first supported language in the browser's preference list, falling back to English. An explicit choice takes priority on subsequent visits to the same origin. If browser storage is unavailable, the choice still works for the current session. Ports 5173 and 8080 have separate browser storage.

Only presentation changes. Routes, saved profiles, shortlist IDs, filter values, API requests and ranking rules are language-independent. The fictional catalog is translated. Live advert titles, company names, locations and excerpts are displayed as supplied; user-entered skills, cities and search terms are preserved.

## Verify from the repository root

```powershell
npm --prefix frontend run build
npm --prefix frontend run test:e2e -- language-switch.spec.ts
npm --prefix frontend run test:adzuna
```

The checks use desktop and mobile Chromium, the local backend and invented provider responses. No credentials are needed.

## Manual checks

- Start in a fresh browser context with Spanish preferred. Check the heading, document language and navigation. Select English, reload, and check that English remains selected.
- Start editing a profile without saving. Switch languages; verify that the draft, selected option values and validation state remain intact.
- Open a demo listing and inspect pay, publication date, comparison explanations and saved controls. Switch back without leaving the listing.
- In local Adzuna search, go to page two and expand an excerpt. Change the language. The page and excerpt stay open; only interface text changes. No provider request is made.
