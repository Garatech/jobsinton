# JobSinton — Current preversion

A complete static design preview on `design/current-preversion`. This branch is for exploration only and is not intended to be merged.

## Open the preview

Download the **JobSinton-current-preversion** artifact from a successful **Verify Current preversion** workflow run, extract it and open `index.html`.

Alternatively, download this branch as a ZIP, extract it and open `design-preversion/index.html`. No server, package installation or backend is needed.

For an existing checkout, first commit or stash any unrelated changes, then:

```powershell
git fetch origin
git switch --track origin/design/current-preversion
Start-Process .\design-preversion\index.html
```

If the local branch already exists, use `git switch design/current-preversion`. The preview can also be opened from an extracted branch archive without changing the working checkout.

## Direction

- **Current prevails:** its warm neutral canvas, sans-serif headings, italic accent, green palette, wordmark, signal motif, generous spacing and rounded surfaces remain the foundation.
- **Ledger informs search:** a compact result list, useful filters and clear salary information.
- **Atlas informs search:** an optional card layout for browsing the same results.
- **Folio and Nocturne are excluded** from this preversion.

The home card now introduces a working preference instead of displaying a development connection check. The established composition remains; its content serves the product journey.

## Pages

| Page | Example |
| --- | --- |
| `index.html` | Overview, example direction and selected opportunities |
| `opportunities.html` | Search, combined filters, salary ordering, list/card views and empty state |
| `job-*.html` | Six full opportunity pages, illustrative fit notes and an application preview |
| `companies.html` | Searchable directory of three fictional teams |
| `company-*.html` | Three company profiles, working practices, benefits and open roles |
| `saved.html` | Shortlist, removal and empty state |
| `profile.html` | Editable example preferences, validation, saved summary and filtered exploration |

All six offers, all three companies and the initial profile are written directly into the HTML. JavaScript enhances the existing content; it does not fetch or generate a catalog.

## Interactions and data

The preview starts with two saved opportunities and an example backend profile. Save actions work across the pages in the same tab. The selected offer IDs also travel in internal link parameters so that navigation works when file storage is blocked.

Example preferences and saved IDs use only the isolated `jobsinton.design.current-preversion.v1` session-storage key. They do not read or modify the application's profile. If storage is unavailable, a profile save updates only the current page and displays that limitation. Opening a separate tab or reopening a file can start a new preview session.

**Reset preview** restores the original sample profile and shortlist. **Discard changes** restores the last saved profile values. The profile's explore link applies the saved role and work mode to the example catalog.

The home direction and offer fit notes refer to the original sample profile. They are illustrative copy, not a live matching engine, and do not recalculate after editing preferences. Unknown information is shown explicitly; no invented numerical matching scores are used.

Applications are represented by a dialog. No application, upload, email or external request is sent. The companies, offers, compensation, benefits and profile are fictional design content. Use sample values when reviewing.

## Review

Follow [REVIEW.md](REVIEW.md) for a short walkthrough and a place to record decisions. [CHANGELOG.md](CHANGELOG.md) records this increment. [VERIFICATION.md](VERIFICATION.md) describes checks and limits.

This preview does not replace the application, select a final implementation or introduce a production feature.
