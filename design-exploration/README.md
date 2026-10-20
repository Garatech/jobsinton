# JobSinton — Design exploration

Five independent static previews for choosing JobSinton's visual direction.

## Open the previews

1. Download and extract the complete folder.
2. Open `index.html` in Chrome or Edge.
3. Open any design, or use the comparison section.
4. Review **Discover**, **Saved**, **Profile**, and an opportunity's details.
5. Use **Keep the best parts** to download your preferences as a text file.

No installation, build, server, account, API key, or internet connection is needed. Each design folder contains its own HTML, CSS and JavaScript and can be copied independently. Its **All designs** link returns to the parent review page when the package remains together.

If downloaded from a repository archive, open `design-exploration/index.html`. GitHub's source-code view does not execute the previews.

## Contents

| Folder | Direction | Distinctive choices |
| --- | --- | --- |
| `00-current/` | Current reference | Existing hero and connection card, original palette and source stylesheet, familiar profile layout |
| `01-folio/` | Warm editorial | Serif titles, ivory paper, open rows, horizontal filters, minimal card framing |
| `02-ledger/` | Quiet precision | Compact sans-serif hierarchy, filter rail, squared controls, aligned list content |
| `03-atlas/` | Sage workspace | Side navigation, muted sage canvas, contained rounded cards, integrated forms |
| `04-nocturne/` | Dark editorial | Forest canvas, pale-green accents, serif titles, two-column opportunities |

`RESEARCH.md` records evidence, limitations and proposed design decisions. `REVIEW.md` provides a short comparison exercise. `VERIFICATION.md` describes checks and their limits.

## Reference fidelity

The current reference is based on `frontend/src/App.tsx`, `frontend/src/profile/ProfileEditor.tsx` and `frontend/src/styles.css` at commit `2470b17350631c7b0d87ff9b6ef85d49b5168ade`.

`00-current/source-styles.css` is the original stylesheet, retained for comparison. The static adaptation preserves the existing hero, connection-card composition, palette, profile form and summary styling. It is not a pixel-certified export of the React application.

Necessary adaptations: the connection is explicitly simulated; preview navigation and example data controls are added; profile help text is shortened; profile interactions use the common preview script. Discover and Saved are proposed extensions, including in the Current folder. The comparison portal can open the same screen in both panes.

## Interaction scope

- Search by role, company, city or technology.
- Filter by role and work mode; clear filters and inspect empty results.
- Open details with the title button; close with the Close button or Escape.
- Save and remove opportunities; inspect the Saved view and its empty state.
- Load an example profile, add/remove technologies, change work mode, validate required fields and save a summary.
- Hybrid and on-site modes show an office-day limit.
- Edit a saved profile and observe that the summary changes only after saving.
- Clear the preview profile with confirmation.
- Export design-review notes from the parent page.

All companies, listings and salaries are fictional fixtures. There is no matching score or hiring prediction. No job application is submitted.

State lasts only while that preview document stays open. Reloading it resets the profile and saved jobs. The previews never read or write the production profile storage key and do not make network requests. Separate comparison panes have separate state.

These are visual prototypes. They do not replace the production profile validation contract or its persistence tests.

## Files

Each design has `index.html`, `styles.css` and `app.js`. Shared behavior is copied into each folder to preserve portability. Browser checks in `tests/` are maintainer tooling; they are not needed to open the previews.

## Decision status

No visual direction has been selected. Individual elements can be combined after review. Production application files are unchanged by this study.
