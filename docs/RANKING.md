# Compatibility ranking

Today, opportunity details explain an individual comparison. Search offers **Newest** (default), **Salary** and an optional **Compatibility** order. The policy accepted in PR #24 is implemented in Java. It answers “Which of these opportunities should I read first?” for a single saved profile and a filtered set of offers. The search page requests a single batch when Compatibility is selected with a saved profile.

## Order of decisions

1. Apply the existing search filters. Compare the remaining opportunities against the same last-saved profile, using the accepted [matching rules](MATCHING.md). Each opportunity keeps its detail-page explanations.
2. Put evaluations with a confirmed constraint conflict **after** valid evaluations without one. A known attendance or residence conflict cannot be offset by advisory points. All offers remain visible. Keep NEEDS_INFORMATION, NOT_ASSESSED and NO_KNOWN_CONFLICT distinct in the explanation; none is a confirmed conflict.
3. Within each group, place evaluations with usable advisory evidence first, ordered by **net support**, highest first. At equal support, prefer greater **coverage**. If both are equal, use the stable opportunity ID in ascending ordinal order (the fictional IDs are lowercase ASCII). Dates and salaries do not silently influence this sort.
4. With no usable advisory evidence, keep the offer in its constraint group after the comparable entries, ordered by ID. Its fit is unavailable, even if a constraint could be checked. This deliberately prioritizes an evidenced difference over an entirely unassessed offer within the same group; it does not label the latter incompatible.

An invalid or unavailable evaluation is **not** a zero-evidence evaluation. Return it separately as “comparison unavailable” after valid results, retaining the offer and ordering unavailable entries by ID. With no saved profile or a failed ranking request, keep ordinary search available, use Newest as the fallback and make the active sort clear. Never combine results from different profiles or rules versions.

## Advisory weights

These are **fixed product defaults**, not empirical hiring probabilities. Role leads because the profile names one target; technical overlap follows closely. Work mode can change the order without becoming a hard requirement. The lower weights reflect that the profile records only total experience and residence, not technology-specific tenure or commuting range.

| Dimension | Weight out of 100 | Evidence |
| --- | ---: | --- |
| Target role | 35 | Accepted role comparison |
| Technologies | 30 | One budget shared across distinct listing terms |
| Work mode | 20 | Mode preference, separate from an office-day limit |
| Total experience | 10 | Only comparisons with the same experience scope |
| Office location | 5 | City and country where the office comparison applies |

Attendance and residence restrictions affect the constraint group only. Salary and other unevaluated conditions have no weight. An extra profile skill cannot lower an offer's position. The public ranker uses the fixed defaults. The alternative weights in the reference cases test sensitivity; they do not introduce profile controls or storage changes.

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

Compare unrounded fractions (e.g. by cross multiplication); round only for display. Weights for reference variants must be nonnegative whole numbers totalling 100. Zero disables that dimension; invalid configurations are rejected rather than silently normalized. Version this policy separately from matching: implemented pair `ranking-v1.0` / `matching-v1.0`. Changing weights or precedence requires a new ranking version and revised expectations.

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

## Java implementation

Call `new CompatibilityRanker().rank(profile, offersById)` with one normalized profile and the already-filtered opportunities. The ranker calls the existing matching engine once per offer; callers cannot mix precomputed results from different snapshots. No controller, network call, persistence or dependency is involved.

A `RankedBatch` carries the matching and ranking versions. Its `ranked` list retains each valid evaluation (including constraint explanations) and its exact A/D/U allocations, agreement, coverage and net support. Empty optional ratios mean undefined; zero is a real value. The immutable `unavailable` list follows the valid entries and retains the engine's validation errors. A known ID with a missing offer is retained there too. IDs must be nonblank and unique (map keys). An empty filtered set is valid.

With no profile, the domain returns `NoProfile.INSTANCE` and does not evaluate or personalize anything. The search page handles the ordinary Newest fallback without sending a request when the profile is absent. The accepted matching version is pinned so upgrading the matching rules requires a ranking policy review.

For exact allocation, one point is split into as many units as there are applicable technology assessments (at least one). Integer totals therefore share a denominator even for a seven-way tag split. Reduced fractions and cross multiplication preserve differences that display rounding would erase. The current engine caps a listing at 100 terms and the total weight is 100.

## Verification after merge

From the updated repository root, using Java 21 and Maven:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=CatalogRankingTest,CompatibilityRankerTest" test
mvn -B -ntp -f backend/pom.xml verify
```

The first command exercises R01–R14, including the real catalog facts for R01–R04, a weight reversal, unknown and inapplicable inputs, exact ties, unavailable items and input permutations. It does not start Spring or need a running server. The second command also verifies the existing domain and HTTP contracts. The implementation PR records actual execution results.

## Batch API and search behavior

`POST /api/matching/rank` takes exactly two JSON fields: the last saved `profile` (the [existing seven-field contract](MATCHING_API.md#request-and-response)) and `opportunityIds` (0–100 unique nonblank strings of at most 80 characters). The server resolves its own listing facts and uses the fixed policy. Client weights and listing facts are not accepted. Parsing shares the 16 KiB limit and duplicate-key/trailing-JSON checks with the detail endpoint.

For example, use a backend profile with Java and Spring Boot, three total years, Madrid/Spain, remote and a null office maximum. Given IDs `["frontend-react","not-in-catalog","backend-java"]`, the response is:

```json
{
  "apiVersion": "ranking-api-v1",
  "rankingVersion": "ranking-v1.0",
  "matchingVersion": "matching-v1.0",
  "ranked": ["backend-java","frontend-react"],
  "unavailable": ["not-in-catalog"]
}
```

The two arrays partition the input exactly. An unknown ID is an unavailable item, not a failed batch. An empty batch returns two empty arrays. Malformed JSON returns 400, oversized input 413, and invalid envelopes/IDs or profiles 422; application errors carry `code`, `message` and `errors` rather than an order. Responses use `Cache-Control: no-store`. The backend does not store or log the profile body. Only IDs and version metadata are needed in the success response; explanations remain available on individual opportunity pages.

The browser applies filters first. Newest and Salary stay local. Compatibility submits one batch per changed set of candidate IDs or saved snapshot, coalescing rapid edits for 150 ms. No batch is sent without a profile or candidates. Layout and shortlist changes alone do not request another ranking. The query string retains the active filters, view and sort on reload or Back.

During loading, the list remains usable in Newest order and reports the pending comparison. A response with incompatible versions, omitted/extra/repeated IDs or malformed content cannot replace the list. An eight-second deadline or request failure switches the selector and URL to Newest and offers **Retry ranking**, which uses the current filters. Navigating, changing sort or superseding the batch cancels it and invalidates late success or failure. Unavailable offers are appended and labelled **Comparison unavailable**; they can still be opened or saved.

## Verify compatibility search

From the repository root with Java 21 and Node 24, stop any development servers first. With frontend dependencies and Chromium installed as in [DEVELOPMENT.md](DEVELOPMENT.md), the focused commands are:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=RankingApiIntegrationTest" test
npm --prefix frontend run test:e2e -- ranking-search.spec.ts
```

For the full run use `mvn -B -ntp -f backend/pom.xml verify`, then `npm --prefix frontend ci`, `npm --prefix frontend run build` and `npm --prefix frontend run test:e2e`. No database, credentials or profile migration is required.

### Manual acceptance checklist

1. Open Opportunities without a saved profile. Selecting Compatibility should leave Newest active and offer profile setup.
2. Save the backend profile in the example above. In Opportunities, Newest starts with Data Engineer; Compatibility puts Backend Developer first. Open it to see the matching facts.
3. Select Remote. Only the three remote roles remain. Change between cards and list, save an offer, open a detail and use Back. The filtered ranking and layout should remain available.
4. Save a Data profile with Python, SQL, three years, Valencia/Spain, Hybrid and a maximum of two office days. With no filters, Compatibility should put Data Engineer first. Reduce the maximum to one and save: Backend Developer should now precede Data Engineer because of its office-day conflict. An unsaved edit must not change the order.
5. Stop the backend, reselect Compatibility, and confirm that the selector shows Newest, filters remain usable and offers can be saved. Restart the backend and retry. Rapidly changing filters must never restore an earlier result set.

This delivery uses the fictional catalog only. After acceptance, the next roadmap milestone is reviewing access and constraints for a first live provider.
