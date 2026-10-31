# Matching domain engine

The first matching implementation is a pure Java 21 module under `backend/src/main/java/io/github/garatech/jobsinton/matching`. It implements the [accepted rules](MATCHING.md), version `matching-v1.0`, without introducing an endpoint or changing the interface.

## What this delivery provides

- Immutable snapshots of normalized profile and opportunity facts.
- A validation result or a complete evaluation, never a partial evaluation after invalid input.
- Stable criterion identifiers, typed outcomes and reason codes, plain-language explanations and compared facts.
- A constraint summary that preserves individual conflicts and unknowns.
- Executable reference cases and additional checks for incomplete facts, normalization, input order, defensive copies and validation.

There is no score, rank, automatic filtering, database, provider request or new dependency. The current website continues to use its existing descriptive facts until the next integration increment.

## Java boundary

| Type | Purpose |
| --- | --- |
| `MatchingInput` | Profile and opportunity snapshots; nested records and enums describe normalized facts |
| `MatchingResult.Evaluation` | Rules version, ordered assessments and constraint summary |
| `MatchingResult.Invalid` | Field-specific validation errors, with stable codes and messages |
| `MatchingEngine` | Stateless evaluation and constraint aggregation |
| `MatchingValidation` | Package-private validation of facts and contradictions |

Call `new MatchingEngine().evaluate(input)`. The result is either Evaluation or Invalid. The module has no Spring annotations and can be tested without starting the application.

Each assessment identifies a criterion such as `experience` or `technology:java`. Technology identifiers use case-insensitive, whitespace-normalized terms while evidence retains a deterministic representative of the supplied spelling. Duplicate terms do not produce duplicate assessments.

Enums use Java uppercase names: ALIGNED, DIFFERENT, UNKNOWN and NOT_ASSESSED correspond to the contract's lowercase outcomes. Constraint summaries follow the same convention. These are domain types; HTTP names and serialization are part of the next increment.

Reason codes are the `MatchingResult.Reason` enum. For example, EXPERIENCE_SCOPE_UNCONFIRMED, ATTENDANCE_EXCEEDS_LIMIT and RESIDENCE_POLICY_UNKNOWN explain distinct causes without relying on wording. Every assessment also includes the relevant limitation, such as the absence of skill proficiency or work-authorization evidence.

The result always includes salary and other-conditions assessments as NOT_ASSESSED. Salary amounts are intentionally absent from the engine input. M19 verifies that salary comparison remains unavailable; parsing or displaying salary metadata belongs outside this module.

## Normalized facts and validation

The engine trusts no numeric default and does not resolve place names or parse requirement prose.

- A missing profile or opportunity is invalid. Nullable facts within those records remain unknown, allowing incomplete normalized listings to be evaluated.
- Role and mode use enums; an adapter maps unrecognized source values to null.
- Experience years use nullable integers in 0–60. Profile years describe total experience. Opportunity experience also carries a scope and an optional subject.
- TOTAL experience must not carry a role or technology subject. Scoped years remain unknown for comparison with the current total-only profile.
- Office days and explicit maximums use nullable integers in 0–5. Null profile maximum means not configured; an explicit zero remains meaningful. The current frontend still preserves its original validation and storage rules.
- REMOTE plus positive regular weekly office days is contradictory. Occasional travel is outside this rule.
- Places carry resolved country and city identifiers. Country syntax is uppercase two-letter codes; the adapter is responsible for resolving real country identifiers. City identifiers compare exactly and require country context. Unresolved values become null, not guessed locations.
- A COUNTRIES policy needs a nonempty list of country identifiers. UNKNOWN or UNRESTRICTED cannot also carry a nonempty restriction list.
- Supplied technology terms must be nonblank and at most 80 characters, with at most 20 profile terms or 100 listing terms. These domain limits do not expand the frontend's existing 40-character profile limit.
- Country policy lists allow at most 250 entries; city identifiers at most 120 characters; experience subjects at most 80.
- Input lists are defensively copied, including malformed elements that the validator can report. Outputs and evidence lists are immutable.

The validator collects errors with REQUIRED, OUT_OF_RANGE, INVALID_VALUE, TOO_MANY_VALUES or CONTRADICTORY codes. Invalid input returns no compatibility summary. It does not throw merely because a nullable fact is absent.

Technology comparison trims and collapses Unicode whitespace and uses locale-independent lowercase keys. Punctuation remains significant; there are no aliases or fuzzy matches. Assessments use a fixed dimension order and sorted technology keys. Accepted-country evidence is deduplicated and sorted. The same facts and rules version produce the same result without time, randomness or network dependencies.

## Fictional catalog mapping for the next integration

This is the explicit mapping plan for the bundled catalog, not a new runtime adapter. No production offer or profile is generated by this increment.

| Current catalog field | Normalized fact |
| --- | --- |
| Role labels | Backend → BACKEND; Frontend → FRONTEND; Full stack → FULL_STACK; SAP → SAP; Data → DATA; Platform → PLATFORM |
| Mode labels | Remote → REMOTE; Hybrid → HYBRID; On-site → ONSITE |
| Tags | Descriptive technology terms; no mandatory-skill classification |
| Office days | Known regular weekly requirement, preserving zero |
| Country/city | The fictional fixture mapping explicitly assigns ES and madrid, valencia or barcelona; providers will need their own verified normalization |
| Residence policy | UNKNOWN for all six existing listings: their display locations do not declare employee residence restrictions |
| Salary and other prose | Retained by the catalog/presentation boundary; not compared by this engine |

Experience mapping must retain the scope actually stated in the fictional requirements:

| Opportunity ID | Minimum | Scope | Subject / rationale |
| --- | --- | --- | --- |
| backend-java | 2 | TECHNOLOGY | Java |
| frontend-react | 2 | ROLE | Frontend development |
| data-engineer | 2 | UNKNOWN | Card gives years without a structured scope |
| fullstack | 2 | UNKNOWN | Card gives years without a structured scope |
| platform-engineer | 4 | ROLE | Infrastructure or platform work |
| sap-developer | 1 | TECHNOLOGY | ABAP |

Therefore the current profile cannot confirm the existing catalog's experience requirements, and the current catalog does not confirm residence conditions. The first integration should honestly show those unknowns. The reference fixtures separately supply known facts to exercise every accepted rule.

A profile adapter must use only the last saved snapshot and preserve remote maximum null. Recognized fictional countries/cities can use an explicit mapping; unresolved free text stays unknown. Do not silently add aliases, geocoding or new profile fields during integration.

## Verification after merge

From a clean, updated repository root with Java 21 and Maven:

```powershell
mvn -B -ntp -f backend/pom.xml "-Dtest=MatchingEngineTest" test
mvn -B -ntp -f backend/pom.xml verify
```

The first command runs the domain checks without a Spring context. The second also runs the existing HTTP integration checks and packages the backend. Test reports are in `backend/target/surefire-reports`. An existing server on port 8080 does not need to stop for these tests: the domain tests use no port and the HTTP tests use a random one.

Expected review:

1. Match the named M01–M24 test cases to the table in MATCHING.md. M24 expands into independent invalid-value cases.
2. Confirm M20 preserves both the attendance conflict and unknown residence policy.
3. Confirm scoped experience is unknown, salary is not assessed and role differences remain advisory.
4. Confirm repeated/reordered inputs produce identical results and invalid inputs return field-specific errors.
5. Expect no visible website change in this delivery.

The implementation PR records actual CI results for its checked commit. Local terminal execution was unavailable during development; GitHub Actions provides the build and test evidence. The authored cases check rule behavior, not accuracy on real job advertisements.

## Next delivery and recovery

After owner review and merge, start a fresh branch from updated main for the backend request/response contract and application integration. Add the explicit catalog/profile adapter, server-boundary validation, loading/error handling and browser tests while preserving Current.

Weights, ranking, new profile preferences and providers remain later increments. Reverting this domain-module PR requires no data migration and does not alter saved browser profiles.
