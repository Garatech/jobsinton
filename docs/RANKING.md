# Compatibility ranking — proposal for review

Today, opportunity details explain an individual comparison. Search still offers **Newest** and **Salary**. This proposal defines how a third, optional sort would answer “Which of these opportunities should I read first?” Merging this document accepts a specification; it does not change the application.

## Order of decisions

1. Apply the existing search filters. Compare the remaining opportunities against the same last-saved profile, using the accepted [matching rules](MATCHING.md). Each opportunity keeps its detail-page explanations.
2. Put evaluations with a confirmed constraint conflict **after** valid evaluations without one. A known attendance or residence conflict cannot be offset by advisory points. All offers remain visible. Keep NEEDS_INFORMATION, NOT_ASSESSED and NO_KNOWN_CONFLICT distinct in the explanation; none is a confirmed conflict.
3. Within each group, place evaluations with usable advisory evidence first, ordered by **net support**, highest first. At equal support, prefer greater **coverage**. If both are equal, use the stable opportunity ID in ascending ordinal order (the fictional IDs are lowercase ASCII). Dates and salaries do not silently influence this sort.
4. With no usable advisory evidence, keep the offer in its constraint group after the comparable entries, ordered by ID. Its fit is unavailable, even if a constraint could be checked. This deliberately prioritizes an evidenced difference over an entirely unassessed offer within the same group; it does not label the latter incompatible.

An invalid or unavailable evaluation is **not** a zero-evidence evaluation. Return it separately as “comparison unavailable” after valid results, retaining the offer and ordering unavailable entries by ID. With no saved profile or a failed ranking request, keep ordinary search available, use Newest as the fallback and make the active sort clear. Never combine results from different profiles or rules versions.

## Advisory weights

These are **proposed product defaults**, not empirical hiring probabilities. Role leads because the profile names one target; technical overlap follows closely. Work mode can change the order without becoming a hard requirement. The lower weights reflect that the profile records only total experience and residence, not technology-specific tenure or commuting range.

| Dimension | Weight out of 100 | Evidence |
| --- | ---: | --- |
| Target role | 35 | Accepted role comparison |
| Technologies | 30 | One budget shared across distinct listing terms |
| Work mode | 20 | Mode preference, separate from an office-day limit |
| Total experience | 10 | Only comparisons with the same experience scope |
| Office location | 5 | City and country where the office comparison applies |

Attendance and residence restrictions affect the constraint group only. Salary and other unevaluated conditions have no weight. An extra profile skill cannot lower an offer's position. The first implementation would use fixed defaults. The alternative weights in the reference cases test sensitivity; they do not introduce profile controls or storage changes.

## Calculation

Use only the engine's outcomes. For each dimension allocate its weight to **A** (aligned), **D** (different) or **U** (unknown). Exclude NOT_ASSESSED from all three. For a remote job, for example, office location drops out; if an office comparison applies but its city is unresolved, those five points are unknown instead.

Divide the technology weight equally among the distinct applicable listing terms after the engine's normalization. Two confirmed tags out of three allocate 20 aligned and 10 unknown. A profile omission is never a technology difference. A listing with no technology terms contributes the entire 30 to unknown; it does not shed the weight. The total technology budget never grows with the tag count, and duplicate terms count only once.

Let **P = A + D + U** (applicable weight) and **K = A + D** (known weight).

| Quantity | Calculation | Interpretation |
| --- | --- | --- |
| Agreement on known facts | A / K | How much of the comparable evidence aligns |
| Coverage | K / P | How much applicable evidence can be compared |
| Net support (sorting key) | (A − D) / P | Confirmed support minus confirmed differences, relative to the applicable evidence |

If K is zero, agreement and the sorting key are unavailable; coverage is zero if P is positive. If P is zero, all three quantities are unavailable. A known balance of positives and negatives can legitimately give net support zero; it is still comparable.

Unknown evidence adds neither support nor disagreement. It keeps the item from receiving full credit for information it does not supply. For example, one confirmed role and four unknown dimensions can yield 100% agreement **on the known facts**, but only 35% coverage. Never present that as “100% compatible”. The optional sort can explain its order with the relevant facts and coverage; it does not require a headline percentage or a visual redesign.

Compare unrounded fractions (e.g. by cross multiplication); round only for display. Weights for reference variants must be nonnegative whole numbers totalling 100. Zero disables that dimension; invalid configurations are rejected rather than silently normalized. Version this policy separately from matching: proposed first pair `ranking-v1.0` / `matching-v1.0`. Changing weights or precedence requires a new ranking version and revised expectations.

## Worked examples

**Current catalog.** Use the same fictional profile for both offers: backend, Java and Spring Boot, three total years, Madrid (Spain), remote, no office-day limit. Both residence policies are unknown, so the offers share a constraint group. Remote office location is inapplicable, giving each 95 applicable points. Scoped experience cannot be confirmed from total years.

| ID | Offer | A / D / U | Net support | Coverage | Expected order |
| --- | --- | --- | --- | --- | --- |
| R01 | backend-java | 75 / 0 / 20 | 75/95 | 75/95 | First: aligned role, two confirmed tags and preferred mode |
| R02 | frontend-react | 20 / 35 / 40 | −15/95 | 55/95 | Second: aligned mode, different role and no confirmed listing tags |

**A constraint changes the answer.** Take a data profile with Python and SQL, three total years, Valencia, hybrid and a one-day office maximum. The Data Engineer offer aligns on role, two of three technologies, mode and office city (80/100 net support). Its two required office days exceed the limit. Backend Developer has weaker advisory evidence (−55/95) but requires zero days. **R03: Backend Developer comes first.** Both retain their unknown residence policies; neither is hidden.

**Isolated reference pairs.** These fictional evaluations exercise cases that the six catalog entries cannot demonstrate on their own. Use the same constraint group unless stated. Unspecified applicable dimensions are unknown; each row uses the default weights unless stated.

| ID | Inputs | Expected result |
| --- | --- | --- |
| R04 | Only role aligns; everything else unknown (P=100). Compare with R01. | Agreement 35/35, coverage 35/100, support 35/100. R01 ranks first despite identical agreement on known facts. |
| R05 | X: role aligns, mode differs. Y: role differs, mode aligns. Office is applicable but unresolved for both. | With P=100, X (+15/100) precedes Y (−15/100). Both have coverage 55/100. |
| R06 | Same X and Y; swap role and mode weights to 20 and 35. | Y (+15/100) precedes X (−15/100); coverage remains 55/100. The underlying explanations do not change. |
| R07 | X from R05 versus Z: one confirmed of two tags, all other dimensions unknown. | Both have support 15/100. X wins on coverage (55/100 vs 15/100). |
| R08 | Identical evaluations for alpha and beta; reverse input order. | alpha always precedes beta, even with different dates or salaries. |
| R09 | Three tags: Java, SQL and Redis; only Java confirmed. Repeat Java with different case/spacing, add an unrelated profile skill and reorder the tags. | Technology allocation remains A=10, U=20. No change in position. An empty listing tag set instead gives A=0, U=30. |
| R10 | One of three tags aligns (10 points), total experience differs (10), everything else unknown, P=100. Compare with all-unknown and otherwise equal constraint status. | Known balance: support 0, coverage 20/100. It precedes all-unknown (K=0), which has no comparable fit. |
| R11 | Reference variant: all 100 points on office location, others disabled. Remote offer versus on-site offer with an unresolved office, same constraint group. | Remote: P=0, coverage unavailable. On-site: P=100, K=0, coverage zero. Both lack a sorting key and tie by ID. |
| R12 | Two otherwise equal evaluations: one has unknown residence policy, the other explicitly allows the profile country. | Neither has a conflict. Keep the two explanations distinct; their ordering follows the ordinary tie-break. |
| R13 | One valid evaluation with a conflict, one validation error and one request with no saved profile. | The valid offer stays ahead of the unavailable item. The no-profile search uses ordinary sorting and makes no personalized claim. |
| R14 | One of seven tags confirmed versus fourteen of ninety-nine; all other advisory facts unknown, P=100. | Both supports round to 4% at whole-percent precision. The first ranks higher because 1/7 exceeds 14/99. No display rounding decides the order. |

## Owner review and handoff

Start with R03 (can preferences outweigh a limit?), R04 (little evidence), and R05–R07 (weights and ties). Accept or adjust those outcomes before coding. The main trade-off is whether role 35 and mode 20 reflect what the first search should prioritize.

Once accepted, implement a pure Java ranker and turn these reference cases into executable tests in a fresh PR. A later integration can evaluate the filtered list as a batch and expose the optional sort without a request per card. Keep Newest as the default, Salary available, and the detail comparison independent. No installation or application restart is needed to review this proposal.
