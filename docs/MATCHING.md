# Matching v1 — Rule contract

Status: accepted by the owner on 2026-10-30 and merged in PR #18. The Java domain implementation follows this baseline; opportunity details now use it through the [matching API](MATCHING_API.md). See [the engine guide](MATCHING_ENGINE.md) for domain types and verification.

JobSinton will explain how a saved profile relates to an opportunity using reproducible comparisons. Each conclusion must identify the supplied facts behind it. A compatibility result is not a hiring probability or a judgment of professional ability.

## Delivery boundary

| Increment | Deliverable | Completion condition |
| --- | --- | --- |
| 1. Rules — this change | This contract and fictional reference cases | Owner reviews the decisions below and merges the documentation PR |
| 2. Domain engine | Validated inputs, deterministic comparisons and automated reference tests in Java | Accepted cases pass; no database or provider dependency |
| 3. Application integration | Backend request/response contract and explanations in the existing interface | Browser checks cover saved profiles, loading, errors and incomplete results |
| 4. Ranking | Separately reviewed weights, coverage, formula and tie-breaking | Reference cases justify ordering and show the effect of changing weights |

The contract was introduced as a documentation-only increment. The domain engine is a separate implementation. Accounts, persistence, providers and additional profile controls remain separate work. There is no percentage, overall suitability label or automatic exclusion of listings in the first engine delivery.

## Accepted decisions

- Keep the existing role and work-mode selections as preferences. They are not exclusion switches.
- Treat an explicitly supplied maximum number of office days as a limit. A remote profile currently stores `null`; do not silently reinterpret that as an explicit zero-day limit.
- Check a listing's explicit residence restriction separately from its office or company address. Residence alone does not establish work authorization.
- Treat technologies absent from a profile as unconfirmed, not as skills the person lacks.
- Do not compare total experience against a requirement for years in a particular technology or role.
- Show unknown information without converting it into a failure or a success.
- Begin with explanations and constraint outcomes. Decide weights and ranking in a later increment.

## Input boundary and existing data

The implemented [profile contract](PROFILE.md) remains unchanged. The engine consumes a normalized snapshot of the last successfully saved profile, not unsaved form edits.

| Existing input | Use in the first engine | Limit or required normalization |
| --- | --- | --- |
| Profile `role`; opportunity `role` | Compare canonical role identifiers | Explicit mapping: Backend → backend, Frontend → frontend, Full stack → full-stack, SAP → sap, Data → data, Platform → platform; unrecognized values stay unknown |
| Profile `technologies`; opportunity `tags` | Explain technology overlap | Tags are descriptive; mandatory/desirable requirement status is unknown unless separately supplied |
| Profile `experienceYears` | Total declared experience | No per-technology or per-role duration exists |
| Opportunity `minimumYears`, `experience`, `requirements` | Retain advertised facts | A number alone does not define its scope; use an explicit scope of total, role, technology or unknown |
| Profile `workMode`; opportunity `mode` | Compare canonical remote, hybrid or onsite | A preference difference is advisory; remote is not a geographic permission |
| Profile `maxOnsiteDays`; opportunity `officeDays` | Compare explicit weekly attendance against a supplied limit | Preserve null as not configured; zero is a valid known requirement |
| Profile `country`, `city`; opportunity `city`, `location` | Explain office location and an explicit residence restriction separately | Free-text location is not a country eligibility list; city needs country context |
| Opportunity `salary`, `max` | Continue presenting the existing salary information | No profile salary preference exists; currency, period and gross/net basis need structured metadata before a comparison |
| Opportunity `unknown` and requirement prose | Preserve source context | Do not automatically parse prose into executable rules in the first engine |

The bundled catalog is fictional. Its adapter must explicitly document any structured facts added for matching. Do not treat the existing remote listings' `location: Spain` as proof of a residence restriction, or `minimumYears` as total experience by default. Catalog tags do not become mandatory requirements simply because they appear on a card.

The normalized contract needs nullable facts for role, mode, office days, office country/city, accepted residence countries, and experience minimum/scope. Null means not supplied. An explicit unrestricted residence policy is distinct from a missing policy. An empty country list is invalid, not unrestricted.

Country and city comparison requires resolved identifiers. The initial fictional fixtures can use an explicit mapping; unrecognized or ambiguous free text remains unknown. Do not silently geocode, infer relocation, or interpret residence as nationality.

Invalid input produces a validation result instead of a compatibility evaluation. Examples: negative experience, office days outside 0–5, or conflicting remote mode and a positive regular office-day requirement. Occasional travel is a separate, currently unevaluated condition. Invalid data is never defaulted to zero.

## Outcome vocabulary

Each assessment has a stable criterion identifier, its dimension, its kind (`constraint` or `advisory`), an outcome, a reason code, a plain-language explanation and the compared facts. Relevant scope or normalization limits travel with the assessment. Presentation text can change without changing its reason code.

| Outcome | Meaning |
| --- | --- |
| `aligned` | The available, comparable facts agree or meet the stated threshold |
| `different` | Comparable facts show a difference or an exceeded limit |
| `unknown` | The comparison applies, but evidence is absent, ambiguous or not comparable |
| `not_assessed` | No relevant preference is configured, the criterion does not apply, or it is outside this version |

Technology assessments retain the actual terms and confirmed/unconfirmed lists. An omitted skill is `unknown`, not `different`. No absence of evidence establishes lack of ability.

Constraint summary, in this order:

1. `conflict`: at least one constraint is `different`.
2. `needs_information`: no constraint differs, but at least one applicable constraint is `unknown`.
3. `no_known_conflict`: at least one constraint is evaluated, all evaluated constraints align, and none is unknown.
4. `not_assessed`: no constraints apply or are configured.

Advisory outcomes never change the constraint summary. Even a conflict retains every other explanation and unknown. `no_known_conflict` only describes the checked constraints; it does not mean every employer requirement has been met. No summary hides an opportunity.

## Comparison rules

### Role and technologies

Role equality is an advisory alignment; different recognized roles produce an advisory difference. Backend and full stack are not automatically equivalent.

Technology terms are trimmed, internal whitespace is collapsed, and comparisons ignore case. Duplicates have no additional effect. Punctuation is retained: Java and JavaScript are distinct, as are C, C++ and C#. No fuzzy or synonym matching is included; Node and Node.js need a later explicit alias rule.

A listing term found in the profile is confirmed overlap. A term missing from the profile is unconfirmed. An empty or unavailable listing tag set produces one unknown technology assessment, not complete alignment. Extra profile technologies have no negative effect.

### Experience

A declared total-experience minimum can be compared with the profile's total years: equality satisfies it; a lower value is an advisory difference. Zero remains a known value.

For a role-specific or technology-specific minimum, the current profile cannot establish comparable years, even if its target role or technology list matches. Return unknown and explain the scope difference. An absent minimum or unknown scope also returns unknown. Do not infer years from seniority titles or cap experience at an advertised range maximum.

### Work mode and attendance

Equal recognized modes align; unequal modes differ as an advisory preference. An unknown listing mode remains unknown.

When a profile has an explicit office-day maximum, compare it with the listing's regular weekly office days as a constraint. A requirement at or below the maximum aligns; exceeding it differs; an absent requirement is unknown. Without a configured maximum, return not assessed for this constraint.

Mode and attendance remain independent. A hybrid preference with a two-day maximum can meet a zero-day remote attendance requirement while still having a work-mode preference difference.

### Office location and residence restriction

For hybrid or on-site work, compare resolved office and profile city/country as an advisory assessment. A different city is a difference, not proof that commuting or relocation is impossible. Missing or unresolved values are unknown. Office location is not assessed for remote work; an unknown mode makes applicability unknown.

An explicit accepted-country list creates a residence constraint: membership aligns and non-membership differs. An explicitly unrestricted residence policy aligns. An absent policy returns unknown. For a country-list policy, an unresolved profile country also returns unknown; an explicitly unrestricted policy needs no country comparison. Employer location is never a substitute for that policy. This assessment checks advertised residence conditions only; work authorization remains unevaluated.

### Salary and other conditions

The current profile has no salary preference. Salary compatibility is therefore not assessed, including when the listing omits salary. That omission remains visible as a listing information gap.

A future salary rule must distinguish missing salary with a configured preference (unknown) from no configured preference (not assessed), preserve currency, period and gross/net basis, and define interval overlap before evaluating it. No currency conversion or invented salary is allowed.

Languages, education, contract type, hours, travel, on-call duties, relocation and work authorization remain unevaluated until their inputs and rules are explicitly defined. Employer prose can still be shown as context.

## Reference cases

These are owner-accepted fictional expectations, implemented in MatchingEngineTest. They are not live-catalog results. Each case starts from the baseline below and changes only the stated facts. Unless specified, every other assessment retains its baseline outcome. The implementation PR records actual execution results.

Baseline profile: backend; technologies Java and Spring Boot; 2 total years; resolved country ES and city Madrid; remote preference; office-day maximum null.

Baseline normalized listing: backend; tags Java and Spring Boot; 2-year minimum explicitly scoped to Java; remote; 0 regular office days; office Madrid, ES; explicit accepted residence countries [ES]; salary absent.

Baseline outcomes: role aligned; both technology overlaps aligned; experience unknown; work mode aligned; attendance not assessed; office location not assessed; residence aligned; salary not assessed. Constraint summary: no known conflict. Work authorization and other unsupported conditions remain unevaluated.

| ID | Change from baseline | Expected affected result | Constraint summary |
| --- | --- | --- | --- |
| M01 | None | Baseline outcomes above; scoped Java years cannot be confirmed from total years | no_known_conflict |
| M02 | Listing role full-stack | Role different; no role equivalence inferred | no_known_conflict |
| M03 | Tags are " java ", "JAVA", "Spring   Boot" | Two unique confirmed overlaps after normalization | no_known_conflict |
| M04 | Tags are JavaScript and Spring Boot | JavaScript unknown; Spring Boot aligned; Java is not JavaScript | no_known_conflict |
| M05 | Tags are absent | Technology evidence unknown | no_known_conflict |
| M06 | Minimum explicitly total 2 years | Experience aligned at the inclusive boundary | no_known_conflict |
| M07 | Minimum explicitly total 3 years | Experience different, advisory only | no_known_conflict |
| M08 | Minimum 2, scope unknown | Experience unknown despite equal numeric values | no_known_conflict |
| M09 | Profile total 0; minimum explicitly total 0 | Experience aligned; zero is not missing | no_known_conflict |
| M10 | Profile hybrid, maximum 2; listing hybrid, office days 2 | Mode, attendance and office location aligned | no_known_conflict |
| M11 | As M10, office days 3 | Attendance different because 3 exceeds 2 | conflict |
| M12 | As M10, office days absent | Attendance unknown | needs_information |
| M13 | Profile hybrid, maximum 2; listing remains remote, days 0 | Mode different; attendance aligned; office location not assessed | no_known_conflict |
| M14 | Accepted residence countries [DE] | Residence different; remote does not imply worldwide eligibility | conflict |
| M15 | Residence policy absent; company and office still in Spain | Residence unknown; company address cannot fill the gap | needs_information |
| M16 | Explicit unrestricted residence policy | Residence aligned; work authorization remains unevaluated | no_known_conflict |
| M17 | As M10, listing office Valencia, ES | Office location different, advisory only; no relocation assumption | no_known_conflict |
| M18 | Profile country cannot be resolved | Residence unknown; do not guess a country identifier | needs_information |
| M19 | Listing includes a salary range | Salary still not assessed because the profile has no preference | no_known_conflict |
| M20 | As M11, residence policy absent | Attendance different and residence unknown both retained | conflict |
| M21 | Profile adds Python and SQL | Existing overlaps unchanged; extra skills have no negative effect | no_known_conflict |
| M22 | Profile technologies [Node]; listing tags [Node.js] | Technology evidence unknown; no undocumented alias | no_known_conflict |
| M23 | Listing remote but regular office days 2 | Validation error; no compatibility result | not produced |
| M24 | Office days 6, negative experience, or explicit empty accepted-country list | Each is independently invalid; no compatibility result | not produced |

The aggregate rule also has a direct unit case with zero applicable constraints: it returns not_assessed, never no_known_conflict. Assessment order and input tag ordering must not change semantic results. Re-evaluating identical normalized inputs with the same rules version must return the same assessments.

## Owner review checklist

No installation or application startup is needed for this documentation change.

- [x] Accept role and work mode as advisory preferences, without automatically filtering listings.
- [x] Accept explicit office-day limits and residence policies as separate constraints, with missing information preserved.
- [x] Accept that null office-day limits in existing remote profiles are not silently converted into zero.
- [x] Accept the distinction between total experience and scoped experience, and between unlisted and absent skills.
- [x] Review M01, M11, M14, M15 and M20 in particular: they cover limited evidence, a known conflict and mixed outcomes.
- [x] Accept explanations before scores, with salary, weights and ranking deferred.
- [x] Merge the documentation PR after accepting the intended behavior (PR #18).

Requested adjustments belong in this contract and its reference cases before implementation. Owner approval must not be inferred from automated checks.

## Implementation handoff

The Java domain engine is delivered separately from the accepted contract. It accepts normalized values and returns versioned structured assessments without storage, network calls or frontend dependencies. The reference cases are executable before an endpoint is introduced.

The [engine guide](MATCHING_ENGINE.md) documents concrete Java types, validation errors, stable reason codes and the intended mapping for the existing fictional catalog, including unknown scope and residence policy. The profile storage format is preserved; any new preference controls require a separate reviewed change.

Record the rules version in each result. Later changes to semantics, aliases or constraints require updated reference cases and a version change. HTTP delivery, UI integration, ranking and providers each follow in their own focused PR.

The original documentation increment checked the current models, case outcomes and internal document links. Engine verification now runs the accepted cases and additional validation, reproducibility and immutability checks. Consult the implementation PR for the checked commit and actual results.
