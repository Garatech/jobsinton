# Client Localization (ES / EN)

The application header offers instant switching between **Español** and **English**.

- **First-visit detection:** Selects the first supported language in the browser's `navigator.languages` preference list, falling back to English (`en`).
- **Persistence:** Explicit selection is saved in `localStorage` (`jobsinton.language`) on the current origin and survives reloads. If storage access is blocked, language switching continues to work in-memory for the active session.
- **Presentation-only scope:** Routes, saved profiles, triage states (`⭐ Save`, `✅ Applied`, `✕ Dismiss`), filter IDs, API payloads, and matching/ranking rules are completely language-independent. Live ATS titles, company names, locations, and descriptions are rendered faithfully as published by the employer, while all UI controls, badges, and explainable matching reasons localize immediately without re-fetching.

## Automated Verification

From the repository root:

```bash
npm --prefix frontend run build
npm --prefix frontend run test:e2e -- language-switch.spec.ts locale.spec.ts live-ats.spec.ts
```

The Playwright suite exercises both desktop (`1440×900`) and mobile (`390×844` / `320px`) Chromium viewports against deterministic fixtures with zero external network calls.
