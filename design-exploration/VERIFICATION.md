# Preview verification

## Automated scope

The dedicated design-preview workflow opens the documents through `file://` in Chromium. It checks each of the five versions at 320, 390, 768 and 1440 px viewport widths.

Checks cover search and combined filters; zero-result recovery; detail opening, Escape and focus return; saving/removing opportunities; profile saving and last-saved summary; conditional office days; duplicate technologies; long values; no horizontal document overflow; no page script errors; and no remote requests from the prototypes.

The review page is checked for comparison-pane loading, mobile overflow and review-note download. Screenshots are captured for catalog, details and profile states.

Palette contrast calculations are in `RESEARCH.md`.

## Evidence

The workflow's **design-preview-evidence** artifact contains `verification.json` and screenshots. A successful workflow is the record that these checks executed on its commit. A source review alone is not a browser test.

## Limits

Automated checks do not establish visual preference, screen-reader usability, full WCAG conformance or behavior in every browser. Screenshots are evidence for inspection, not a claim of independent human review.

The Current version is an adaptation based on source, not a pixel-verified screenshot recreation. Production frontend and backend tests are outside this isolated design package.

The local execution environment could not initialize. Browser execution is therefore delegated to the repository workflow, with its actual outcome recorded after completion.
