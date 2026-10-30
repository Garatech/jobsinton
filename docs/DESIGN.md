# Current — application design

## Decision

On 2026-10-30, the owner selected the Current preversion as the application's primary design. It is the working baseline for future frontend increments. Further visual changes require a new design decision. The owner has verified the integrated interface and closed this design stage, with the short page fade as its final refinement.

Source reference: [`design/current-preversion`](https://github.com/Garatech/jobsinton/tree/design/current-preversion), commit `d2e3b7666eb3ade7da2c1baf26d219fe44c33914`. That exploratory branch remains separate and is not merged. Its approved design was integrated into the application through PR #14, merged on 2026-10-30.

- Current defines the palette, typography, wordmark, signal motif, spacing and rounded surfaces.
- Ledger informs the compact search list and filter organization.
- Atlas informs the alternative card view.
- Folio and Nocturne are excluded from the selected direction.

## Visual foundation

| Element | Baseline |
| --- | --- |
| Canvas | `#f5f5ef` |
| Primary text | `#172d2a` |
| Secondary text | `#51635c` |
| Main green | `#173e35` |
| Pale green surface | `#eaf0e1` |
| Lime accent | `#d6ef9d` |
| Rules and borders | `#d8ded7` |
| Main type | Inter when locally available, Segoe UI, system sans-serif |
| Editorial accent | Georgia italic, limited to the hero emphasis |
| Content shell | 1240 px maximum, with responsive side padding |
| Interaction | Visible keyboard focus, native controls and restrained hover states |

No external font, image or icon request is required. The interface uses the approved CSS foundation and shared components, rather than separate styles per route.

## Page transitions

A 200 ms ease-out opacity fade introduces the incoming page. The header, navigation and footer remain outside the effect. There is no slide, zoom, exit delay or interaction lock.

- Initial loads and reloads display immediately.
- Switching to another page or another opportunity/company detail starts one fade.
- Search parameters, filtering, layout selection, saved actions and profile editing do not restart it.
- The same main element and profile editor remain mounted, preserving drafts and state.
- Rapid navigation cancels the previous animation; Back and Forward use the same behavior.
- Route focus and scroll position update before paint. A new page starts at the top without an additional scrolling animation.
- `prefers-reduced-motion: reduce` skips the fade and cancels it immediately if the preference changes during navigation.
- Browsers without the native animation API keep immediate navigation.

The small `usePageFade` hook uses the browser's animation API. No dependency or data migration is required. Cancellation and completion restore the ordinary opaque style, so content never depends on a completion callback to become usable.

## Transition acceptance checklist

After merging and updating the checkout with the commands below:

- [ ] Switch between Overview, Opportunities, Companies, Saved and Profile: only the incoming content fades briefly.
- [ ] Open different offers and companies, then use Back and Forward.
- [ ] Navigate rapidly: the latest page wins and remains fully visible.
- [ ] Edit a profile field, navigate away and return: the draft remains.
- [ ] Change filters, result layout or saved state: the page does not fade again.
- [ ] Use keyboard navigation: page focus, skip navigation and dialog dismissal remain usable.
- [ ] Enable reduced motion in the operating system or browser emulation: navigation is immediate. Enabling it during a fade restores full opacity.
- [ ] Repeat on a narrow viewport.

This closes the current visual-design stage. The next planned increment is the matching v1 contract and reviewed reference cases, followed by implementation against the fictional catalog. The established design remains the baseline.

## Application pages

The application uses hash navigation so direct links, reloads and browser history work without introducing server rewrite requirements.

| URL fragment | Purpose |
| --- | --- |
| `#home` | Overview and saved professional direction |
| `#opportunities` | Search, combined filters, ordering and list/card layouts |
| `#opportunities/<id>` | Opportunity requirements, essentials, company and profile facts |
| `#companies` | Company search |
| `#companies/<id>` | Team profile, working practices and open roles |
| `#saved` | Browser-local shortlist |
| `#profile` | Existing profile editor, adapted to the approved layout |
| `#status` | Real Java connection, timeout and retry |
| Unknown route or ID | Recoverable page-not-found view |

Search filters and the selected layout are encoded in the fragment. Profile values are never included in URLs. Search from the profile passes only its role and work mode.

## Product behavior

The interface retains React and strict TypeScript, producing HTML, CSS and JavaScript through the existing Vite build. No new runtime dependency is introduced.

The six opportunities and three companies are explicitly fictional, bundled data. They provide a complete browsing journey while provider integration remains pending. Publication dates are fixed dates, not perpetually fresh relative claims. An application dialog explains that no application is submitted.

Saved opportunities start empty and persist under `jobsinton.saved-opportunities` with a versioned record. Reads validate the envelope and entries; unknown IDs are ignored. Saving or clearing only updates the interface after storage succeeds. Invalid records are preserved until an explicit save or clear. The shortlist synchronizes across open tabs through browser storage events.

The existing `jobsinton.professional-profile` v1 record remains compatible. No migration, reset or sample profile is written on startup. Data and platform roles extend the available target roles without invalidating existing records. Drafts stay in memory during navigation and remain separate from the last successful save. Reloading discards unsaved edits.

The home direction and opportunity facts use the last saved profile. The detail comparison shows exact technology-name overlap, stated experience, role, working mode and location. It does not rank opportunities, assign a score or infer eligibility to work. Matching v1 remains a separate milestone.

## Implementation map

- `frontend/src/foundation.css`: retained Current foundation.
- `frontend/src/styles.css`: approved page layouts and application states.
- `frontend/src/App.tsx`: shared navigation, saved state, retained profile draft and route composition.
- `frontend/src/navigation.ts`: hash-route reading and browser navigation.
- `frontend/src/usePageFade.ts`: cancellable page-entry fade with reduced-motion support.
- `frontend/src/ui.tsx`: shared cards, actions, headings and application dialog.
- `frontend/src/pages/`: home, search, companies, opportunity details and service status.
- `frontend/src/catalog/`: fictional data and guarded shortlist storage.
- `frontend/src/profile/`: existing profile contract, persistence and editor.
- `frontend/tests/current-interface.spec.ts`: integration, storage recovery, navigation and responsive evidence.

## Verify after merging

Stop development servers and test runners before installation or automated verification. From a clean checkout at the repository root, after the owner merges the PR:

```powershell
git status --short --branch
git fetch origin
git switch main
git pull --ff-only
mvn -B -ntp -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run build
cd frontend
npx playwright install chromium
npm run test:e2e
cd ..
```

If the initial status shows uncommitted changes, preserve them on their working branch before switching. Run commands individually and continue only after success. Chromium installation is needed only if its expected version is missing.

Start the two services as described in [DEVELOPMENT.md](DEVELOPMENT.md), then open [http://localhost:5173](http://localhost:5173). Use the same origin as before to retain an existing profile.

## Manual acceptance checklist

- [ ] The overview, green direction card, typography and spacing retain the approved Current design.
- [ ] An existing profile loads with its previous values. A fresh browser starts with no profile or saved opportunities.
- [ ] Search finds a role, technology or company; role, work mode and minimum salary filters combine correctly.
- [ ] Salary ordering and list/card selection work; reloading preserves the search fragment.
- [ ] Opportunity and company links work, including reload, Back and Forward.
- [ ] Saving works from results and details; the shortlist persists after reload.
- [ ] Removing the last saved opportunity shows an empty state and leaves keyboard focus on a useful control.
- [ ] Clearing the shortlist asks for confirmation and keeps the profile.
- [ ] Profile edits remain drafts while navigating; Save updates the summary, home direction and detail facts.
- [ ] The profile's Explore action opens the appropriate role and work-mode filters.
- [ ] An application action clearly identifies the fictional listing and sends nothing.
- [ ] Service status reports the real Java API, including unavailable and retry behavior.
- [ ] Pages remain usable at narrow widths, and keyboard focus, dialogs and skip navigation work.

## Verification scope

CI runs the backend HTTP contract, strict TypeScript build, existing profile and service tests, and the new application journeys. The explicit responsive matrix covers 15 routes at 320, 390, 768 and 1440 px, plus card search at each width. The `current-interface-previews` artifact contains those screenshots for 14 days.

The transition tests additionally cover intermediate opacity, completion, reduced motion (including live changes), rapid navigation, browser history, draft retention, in-page updates and fallback without the animation API. The PR records the actual tested commit and results. Automated Chromium checks are not human visual approval or a complete accessibility audit. Local terminal provisioning was unavailable during preparation; the verification runs in GitHub Actions.
