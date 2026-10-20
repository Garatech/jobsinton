# Visual direction research

Research date: 20 October 2026. Scope: a restrained, editorial interface for finding software jobs, retaining JobSinton's green identity.

## Recommendation

Keep a deep green identity on a predominantly light, quiet canvas. Use typography, alignment and spacing to create hierarchy. Reserve strong color for navigation, primary actions and a small number of deliberate accents. The starting recommendation is the current green with Folio's editorial hierarchy, with Ledger's compact arrangement available where comparison benefits from density.

This is a design judgment informed by evidence and the owner's preferences. No source establishes an objectively best hexadecimal color, corner radius or layout for JobSinton. The proposed versions are candidates for review, not experimentally proven winners.

## Research approach

The review combines a recent systematic review, earlier psychological research, original website-perception research, specialist UX guidance and W3C accessibility documentation. A psychology publication was also consulted to examine common popular explanations of green. Where a summary or abstract was available, conclusions are limited to that material. This is a targeted literature review, not a new systematic review or user study.

### Color: associations are not guaranteed outcomes

Jonauskaite and Mohr's 2025 systematic review covers 132 articles, 42,266 participants and 64 countries. Green and blue-green commonly corresponded with positive, lower-arousal emotions. Lightness and saturation mattered alongside hue. Crucially, most studies measured associations: they do not establish that a green job-search interface makes users calmer, more trusting or more successful. [1]

A 2020 study of 4,598 participants across 30 nations found broadly shared color-emotion patterns with geographic and linguistic differences. The practical implication is to avoid treating a color's meaning as identical for every visitor. [2]

Elliot's review highlights methodological weaknesses and cautions against confident real-world prescriptions from immature evidence. Color should be evaluated in context rather than selected from a universal emotion chart. [3]

Verywell Mind describes familiar associations between green, nature and calm, while also noting personal and cultural influences. This is useful context for the design brief, but its broader wellness claims are not evidence that a particular website background reduces stress. [4]

**Application:** retain green because it already suits the product identity and stated preference. Test muted surfaces and deep-green actions. Do not attach therapeutic or conversion claims to the palette.

### Visual complexity and familiarity

Tuch and colleagues studied aesthetic impressions of website screenshots under short exposure. Lower visual complexity combined with familiar website structure was rated favorably. These results concern first impressions, not completion of a job-search task. [5]

**Application:** keep a recognizable navigation model: Discover, Saved and Profile. Limit simultaneous visual emphases. Let the opportunity title, location, work mode and salary carry the page. Avoid decorative metrics, unnecessary illustrations and a large promotional hero on every working screen.

### Minimalism must retain useful information

Nielsen Norman Group's minimalist-design guidance prioritizes necessary information and removal of distracting material. Its flat-design guidance warns that removing visual cues can make clickable elements difficult to identify. [6][7]

**Application:** shorten slogans and repeated explanations. Keep field labels, error messages, salary units, residence restrictions and save confirmations. Use real buttons with clear boundaries or established link treatments. Essential controls remain visible without hover.

### Grouping and distribution

Proximity guidance explains how nearby elements are read as belonging together. Visual hierarchy guidance emphasizes scale, contrast, spacing and balance. These are design principles, not measurements proving one grid is optimal. [8][9]

**Application:** keep each job's company, title and location together; place salary and save controls consistently. Separate filters from results. Use larger gaps between groups than within a group. The catalog, details and profile should use a common spacing rhythm.

Folio tests open rows and a horizontal filter band. Ledger tests a filter rail and denser rows. Atlas tests side navigation and cards. Nocturne tests a darker canvas and a two-column editorial arrangement. Each changes more than color.

### Shape

Bar and Neta's study found contour-related preferences when judging visual objects. It does not establish a best web-button radius or justify making every element pill-shaped. [10]

**Application:** compare restrained rectangular controls with moderate rounding. Use a small, consistent family of radii in each proposal. Rounded cards can suit Atlas; sharper edges can reinforce Folio's page-like structure. Neither choice is labeled psychologically superior.

### Accessibility and reading

WCAG 2.2 specifies minimum contrast of 4.5:1 for ordinary text and 3:1 for qualifying large text. Essential non-text control cues generally need 3:1 against adjacent colors. Color alone must not communicate a state. The AA minimum target-size criterion is 24 by 24 CSS pixels, with exceptions; 44-pixel controls are a more generous choice in these prototypes, not the wording of that AA requirement. [11–14]

**Application:** use readable secondary text rather than faint gray; retain a visible focus indicator; combine state color with text; use explicit labels and native controls. Avoid a claim of full WCAG conformance based only on palette ratios.

## Proposed design rules

These numbers are starting design parameters, not psychological laws:

- **Color balance:** neutral or near-neutral surfaces occupy most of the page. Deep green provides identity and action emphasis. Pale green supports selected or grouped content. There is no required 60/30/10 ratio.
- **Accent discipline:** avoid decorative red/green match badges. Error color carries a written explanation. No compatibility percentage appears before a real scoring contract exists.
- **Type:** serif titles are tested in Folio and Nocturne; sans-serif controls and body text remain practical. Ledger and Atlas test a fully sans-serif alternative. Serif is an editorial choice, not proof of credibility.
- **Scale:** ordinary editable text is 16 px. Supporting information is smaller but contrasted. Titles use a small hierarchy rather than many competing sizes.
- **Spacing:** use a recurring 4/8 px rhythm, with approximately 16–24 px inside groups and 32–60 px between major regions where appropriate.
- **Measure:** bound content width. Wrap long labels and values. Mobile layouts stack instead of shrinking desktop text.
- **Motion:** no automatic animation, background video or looping decoration. Respect reduced-motion preferences.
- **Copy:** concise labels and task-relevant information. No testimonials, fabricated usage statistics or unsupported promises.
- **Appearance:** Nocturne is a comparison candidate, not evidence that dark mode is healthier or universally easier to read. The light alternatives remain the default recommendation.

## Palette candidates and measured contrast

Ratios below are calculated from the specified sRGB pairs using relative luminance. They test these pairs only; component states, rendered text and focus visibility need separate evaluation.

| Pair | Foreground | Background | Contrast |
| --- | --- | --- | --- |
| Current | `#172d2a` | `#f5f5ef` | 13.28:1 |
| Current secondary | `#51635c` | `#f5f5ef` | 5.83:1 |
| Current button | `#173e35` | `#d6ef9d` | 9.39:1 |
| Folio | `#20352b` | `#f7f5ef` | 12.00:1 |
| Folio secondary | `#536258` | `#f7f5ef` | 5.92:1 |
| Folio action | `#ffffff` | `#254d39` | 9.55:1 |
| Ledger | `#182d26` | `#ffffff` | 14.56:1 |
| Ledger secondary | `#52655b` | `#ffffff` | 6.23:1 |
| Ledger action | `#ffffff` | `#245b46` | 7.90:1 |
| Atlas | `#233b32` | `#edf2ec` | 10.62:1 |
| Atlas secondary | `#4d6357` | `#edf2ec` | 5.72:1 |
| Atlas action | `#ffffff` | `#285343` | 8.72:1 |
| Nocturne | `#eef2e9` | `#14231e` | 14.36:1 |
| Nocturne secondary | `#b4c3b6` | `#14231e` | 8.86:1 |
| Nocturne action | `#193223` | `#c5daa9` | 9.18:1 |

The original green is `#173e35`, with `#f5f5ef` as the existing canvas and `#d6ef9d` as a pale accent. Folio stays close to that family; the other proposals deliberately test different distributions and surface treatments.

## What the research does not settle

It does not tell us which version Alejandro should prefer, whether these visitors will trust green more than blue, which palette increases applications, or which screen produces the fastest comparison. Those require context-specific observation.

A useful next check is the same short task in each version: locate a remote Java role, inspect its constraints, save it and edit a profile. Observe hesitation, missed controls, reading effort and preference. Small informal reviews can guide refinement but should not be presented as statistically representative evidence.

## Sources

1. Jonauskaite, D., & Mohr, C. (2025). *Do we feel colours? A systematic review of 128 years of psychological research linking colours and emotions.* Psychonomic Bulletin & Review. https://doi.org/10.3758/s13423-024-02615-z — systematic review; abstract and relevant full-text sections consulted.
2. Jonauskaite et al. (2020). *Universal Patterns in Color-Emotion Associations Are Further Shaped by Linguistic and Geographic Proximity.* https://pubmed.ncbi.nlm.nih.gov/32900287/ — primary study; abstract consulted.
3. Elliot, A. J. (2015). *Color and psychological functioning: a review of theoretical and empirical work.* https://pmc.ncbi.nlm.nih.gov/articles/PMC4383146/ — review; methods limitations and conclusions consulted.
4. Cherry, K. (updated 2026). *What Does the Color Green Mean?* Verywell Mind. https://www.verywellmind.com/color-psychology-green-2795817 — psychology divulgation; not a causal website study.
5. Tuch et al. (2012). *The role of visual complexity and prototypicality regarding first impression of websites.* https://research.google/pubs/the-role-of-visual-complexity-and-prototypicality-regarding-first-impression-of-websites-working-towards-understanding-aesthetic-judgments/ — original research summary; abstract consulted.
6. Fessenden, T. (2021). *Aesthetic and Minimalist Design.* Nielsen Norman Group. https://www.nngroup.com/articles/aesthetic-minimalist-design/ — professional UX guidance.
7. Moran, K. (2017). *Flat-Design Best Practices.* Nielsen Norman Group. https://www.nngroup.com/articles/flat-design-best-practices/ — professional UX guidance.
8. Harley, A. (2020). *Proximity Principle in Visual Design.* Nielsen Norman Group. https://www.nngroup.com/articles/gestalt-proximity/ — professional UX guidance.
9. Gordon, K. (2020; reviewed 2026). *5 Principles of Visual Design in UX.* Nielsen Norman Group. https://www.nngroup.com/articles/principles-visual-design/ — professional UX guidance.
10. Bar, M., & Neta, M. (2006). *Humans prefer curved visual objects.* https://pubmed.ncbi.nlm.nih.gov/16913943/ — primary study; abstract consulted.
11. W3C. *Understanding SC 1.4.3: Contrast (Minimum).* https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
12. W3C. *Understanding SC 1.4.11: Non-text Contrast.* https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html
13. W3C. *Understanding SC 1.4.1: Use of Color.* https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html
14. W3C. *Understanding SC 2.5.8: Target Size (Minimum).* https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html

The W3C understanding documents explain the success criteria; their explanatory prose is informative rather than a separate conformance standard.
