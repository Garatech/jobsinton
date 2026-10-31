package io.github.garatech.jobsinton.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.garatech.jobsinton.matching.MatchingInput.*;
import static io.github.garatech.jobsinton.matching.MatchingResult.*;
import static org.assertj.core.api.Assertions.*;

/** Executable expectations from docs/MATCHING.md; no Spring context is needed. */
class MatchingEngineTest {
    private final MatchingEngine engine = new MatchingEngine();

    @ParameterizedTest(name = "{0}")
    @MethodSource("referenceCases")
    void acceptedReferenceCases(String id, MatchingInput input, Map<String, Outcome> expected, Summary summary) {
        var result = evaluate(input);
        assertThat(outcomes(result)).containsExactlyInAnyOrderEntriesOf(expected);
        assertThat(result.constraintSummary()).isEqualTo(summary);
        assertThat(result.rulesVersion()).isEqualTo("matching-v1.0");
        assertThat(result.assessments()).allSatisfy(a -> {
            assertThat(a.reason()).isNotNull();
            assertThat(a.explanation()).isNotBlank();
            assertThat(a.evidence().limitation()).isNotBlank();
        });
    }

    static Stream<Arguments> referenceCases() {
        return Stream.of(
                scenario("M01 scoped Java experience remains unconfirmed", f -> {}, Summary.NO_KNOWN_CONFLICT),
                scenario("M02 role differences are advisory", f -> f.jobRole = Role.FULL_STACK,
                        Summary.NO_KNOWN_CONFLICT, "role", Outcome.DIFFERENT),
                scenario("M03 normalize case, spaces and duplicates",
                        f -> f.jobTerms = List.of(" java ", "JAVA", "Spring   Boot"), Summary.NO_KNOWN_CONFLICT),
                scenario("M04 Java is not JavaScript", f -> f.jobTerms = List.of("JavaScript", "Spring Boot"),
                        Summary.NO_KNOWN_CONFLICT, "technology:java", null, "technology:javascript", Outcome.UNKNOWN),
                scenario("M05 missing listing terms are unknown", f -> f.jobTerms = null,
                        Summary.NO_KNOWN_CONFLICT, "technology:java", null, "technology:spring boot", null,
                        "technologies", Outcome.UNKNOWN),
                scenario("M06 equal total years meet the minimum", f -> f.experience = total(2),
                        Summary.NO_KNOWN_CONFLICT, "experience", Outcome.ALIGNED),
                scenario("M07 fewer total years are advisory", f -> f.experience = total(3),
                        Summary.NO_KNOWN_CONFLICT, "experience", Outcome.DIFFERENT),
                scenario("M08 an unknown scope cannot be compared",
                        f -> f.experience = new Experience(2, ExperienceScope.UNKNOWN, null),
                        Summary.NO_KNOWN_CONFLICT),
                scenario("M09 zero years are known", f -> { f.years = 0; f.experience = total(0); },
                        Summary.NO_KNOWN_CONFLICT, "experience", Outcome.ALIGNED),
                scenario("M10 attendance at the limit", Fixture::hybrid, Summary.NO_KNOWN_CONFLICT,
                        "attendance", Outcome.ALIGNED, "office-location", Outcome.ALIGNED),
                scenario("M11 attendance exceeds the limit", f -> { f.hybrid(); f.days = 3; },
                        Summary.CONFLICT, "attendance", Outcome.DIFFERENT, "office-location", Outcome.ALIGNED),
                scenario("M12 attendance unspecified", f -> { f.hybrid(); f.days = null; },
                        Summary.NEEDS_INFORMATION, "attendance", Outcome.UNKNOWN, "office-location", Outcome.ALIGNED),
                scenario("M13 remote meets a hybrid attendance maximum",
                        f -> { f.preferredMode = WorkMode.HYBRID; f.maximum = 2; }, Summary.NO_KNOWN_CONFLICT,
                        "work-mode", Outcome.DIFFERENT, "attendance", Outcome.ALIGNED),
                scenario("M14 remote residence restriction excludes ES",
                        f -> f.policy = countries("DE"), Summary.CONFLICT, "residence", Outcome.DIFFERENT),
                scenario("M15 office country cannot replace residence policy", f -> f.policy = null,
                        Summary.NEEDS_INFORMATION, "residence", Outcome.UNKNOWN),
                scenario("M16 explicit unrestricted residence", f -> f.policy = unrestricted(),
                        Summary.NO_KNOWN_CONFLICT),
                scenario("M17 another office city is advisory",
                        f -> { f.hybrid(); f.office = new Place("ES", "valencia"); },
                        Summary.NO_KNOWN_CONFLICT, "attendance", Outcome.ALIGNED, "office-location", Outcome.DIFFERENT),
                scenario("M18 unresolved profile country", f -> f.residence = new Place(null, "madrid"),
                        Summary.NEEDS_INFORMATION, "residence", Outcome.UNKNOWN),
                // Salary metadata is deliberately outside MatchingInput in this version.
                // Whether a card displays a range cannot change this contract's NOT_ASSESSED result.
                scenario("M19 salary has no configured comparison", f -> {}, Summary.NO_KNOWN_CONFLICT),
                scenario("M20 retain conflict and unknown together",
                        f -> { f.hybrid(); f.days = 3; f.policy = null; }, Summary.CONFLICT,
                        "attendance", Outcome.DIFFERENT, "office-location", Outcome.ALIGNED, "residence", Outcome.UNKNOWN),
                scenario("M21 extra profile skills do not penalize",
                        f -> f.profileTerms = List.of("Java", "Spring Boot", "Python", "SQL"),
                        Summary.NO_KNOWN_CONFLICT),
                scenario("M22 no undocumented Node alias",
                        f -> { f.profileTerms = List.of("Node"); f.jobTerms = List.of("Node.js"); },
                        Summary.NO_KNOWN_CONFLICT, "technology:java", null, "technology:spring boot", null,
                        "technology:node.js", Outcome.UNKNOWN)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidInputsNeverProduceCompatibility(String id, MatchingInput input, String field, ValidationCode code) {
        assertThat(engine.evaluate(input)).isInstanceOfSatisfying(Invalid.class, invalid ->
                assertThat(invalid.errors()).anySatisfy(error -> {
                    assertThat(error.field()).isEqualTo(field);
                    assertThat(error.code()).isEqualTo(code);
                }));
    }

    static Stream<Arguments> invalidCases() {
        var cases = new ArrayList<Arguments>();
        cases.add(invalid("M23 contradictory remote attendance", f -> f.days = 2,
                "opportunity.officeDays", ValidationCode.CONTRADICTORY));
        cases.add(invalid("M24 office days outside the weekly range", f -> { f.jobMode = WorkMode.HYBRID; f.days = 6; },
                "opportunity.officeDays", ValidationCode.OUT_OF_RANGE));
        cases.add(invalid("M24 negative profile experience", f -> f.years = -1,
                "profile.totalYears", ValidationCode.OUT_OF_RANGE));
        cases.add(invalid("M24 empty explicit residence list", f -> f.policy = countries(),
                "opportunity.residencePolicy.countries", ValidationCode.REQUIRED));
        cases.add(invalid("negative advertised experience", f -> f.experience = total(-1),
                "opportunity.experience.minimumYears", ValidationCode.OUT_OF_RANGE));
        cases.add(invalid("invalid profile office maximum", f -> f.maximum = 6,
                "profile.maxOfficeDays", ValidationCode.OUT_OF_RANGE));
        cases.add(invalid("null technology element", f -> f.jobTerms = Arrays.asList("Java", null),
                "opportunity.technologies[1]", ValidationCode.INVALID_VALUE));
        cases.add(invalid("blank technology element", f -> f.profileTerms = List.of("\u00a0\t"),
                "profile.technologies[0]", ValidationCode.INVALID_VALUE));
        cases.add(invalid("unresolved raw country text is not a normalized ID",
                f -> f.residence = new Place("Spain", "madrid"),
                "profile.residence.country", ValidationCode.INVALID_VALUE));
        cases.add(invalid("null country inside explicit policy",
                f -> f.policy = new ResidencePolicy(ResidenceScope.COUNTRIES, Arrays.asList("ES", null)),
                "opportunity.residencePolicy.countries[1]", ValidationCode.INVALID_VALUE));
        cases.add(invalid("unrestricted policy cannot carry restrictions",
                f -> f.policy = new ResidencePolicy(ResidenceScope.UNRESTRICTED, List.of("ES")),
                "opportunity.residencePolicy", ValidationCode.CONTRADICTORY));
        cases.add(invalid("total years cannot also name a technology",
                f -> f.experience = new Experience(2, ExperienceScope.TOTAL, "Java"),
                "opportunity.experience.subject", ValidationCode.CONTRADICTORY));
        cases.add(Arguments.of("missing input", null, "input", ValidationCode.REQUIRED));
        cases.add(Arguments.of("missing profile", new MatchingInput(null, new Fixture().input().opportunity()),
                "profile", ValidationCode.REQUIRED));
        cases.add(Arguments.of("missing opportunity", new MatchingInput(new Fixture().input().profile(), null),
                "opportunity", ValidationCode.REQUIRED));
        return cases.stream();
    }

    @Test
    void baselineReasonsAndEvidenceExplainTheActualLimits() {
        var result = evaluate(new Fixture().input());
        assertThat(at(result, "experience").reason()).isEqualTo(Reason.EXPERIENCE_SCOPE_UNCONFIRMED);
        assertThat(at(result, "experience").evidence().opportunityValues())
                .containsExactly("minimumYears=2", "scope=TECHNOLOGY", "subject=Java");
        assertThat(at(result, "technology:java").evidence().profileValues()).containsExactly("Java");
        assertThat(at(result, "residence").evidence().opportunityValues()).containsExactly("ES");
        assertThat(at(result, "attendance").reason()).isEqualTo(Reason.ATTENDANCE_NOT_CONFIGURED);
        assertThat(at(result, "salary").reason()).isEqualTo(Reason.SALARY_NOT_CONFIGURED);
        assertThat(at(result, "other-conditions").outcome()).isEqualTo(Outcome.NOT_ASSESSED);
        assertThat(at(result, "other-conditions").evidence().limitation()).contains("work authorization");
    }

    @Test
    void missingFactsRemainUnknownWithoutInventedZeroes() {
        var f = new Fixture();
        f.profileRole = null; f.jobRole = null; f.jobMode = null; f.preferredMode = null;
        f.years = null; f.experience = null; f.residence = null; f.office = null;
        f.jobTerms = List.of(); f.policy = new ResidencePolicy(ResidenceScope.UNKNOWN, null);
        var result = evaluate(f.input());
        for (String criterion : List.of("role", "technologies", "experience", "work-mode", "office-location", "residence")) {
            assertThat(at(result, criterion).outcome()).as(criterion).isEqualTo(Outcome.UNKNOWN);
        }
        assertThat(result.constraintSummary()).isEqualTo(Summary.NEEDS_INFORMATION);
        assertThat(at(result, "attendance").outcome()).isEqualTo(Outcome.NOT_ASSESSED);
    }

    @Test
    void unlistedProfileSkillsAreUnconfirmedRatherThanAbsent() {
        var f = new Fixture(); f.profileTerms = null;
        var result = evaluate(f.input());
        assertThat(at(result, "technology:java").reason()).isEqualTo(Reason.TECHNOLOGY_UNCONFIRMED);
        assertThat(at(result, "technology:java").outcome()).isEqualTo(Outcome.UNKNOWN);
        assertThat(result.constraintSummary()).isEqualTo(Summary.NO_KNOWN_CONFLICT);
    }

    @Test
    void roleScopedExperienceIsAlsoUnknown() {
        var f = new Fixture(); f.experience = new Experience(1, ExperienceScope.ROLE, "backend");
        assertThat(at(evaluate(f.input()), "experience").reason()).isEqualTo(Reason.EXPERIENCE_SCOPE_UNCONFIRMED);
    }

    @Test
    void missingMinimumAndMissingScopeAreUnknown() {
        for (var requirement : List.of(new Experience(null, ExperienceScope.TOTAL, null),
                new Experience(2, null, null))) {
            var f = new Fixture(); f.experience = requirement;
            assertThat(at(evaluate(f.input()), "experience").outcome()).isEqualTo(Outcome.UNKNOWN);
        }
    }

    @Test
    void explicitlyUnrestrictedResidenceDoesNotNeedAResolvedCountry() {
        var f = new Fixture(); f.residence = null; f.policy = unrestricted();
        var result = evaluate(f.input());
        assertThat(at(result, "residence").reason()).isEqualTo(Reason.RESIDENCE_UNRESTRICTED);
        assertThat(result.constraintSummary()).isEqualTo(Summary.NO_KNOWN_CONFLICT);
    }

    @Test
    void onsiteAndPartialOfficeFactsAreHandledWithoutRelocationAssumptions() {
        var f = new Fixture(); f.hybrid(); f.jobMode = WorkMode.ONSITE; f.days = 5; f.maximum = 5;
        f.office = new Place("ES", null);
        var result = evaluate(f.input());
        assertThat(at(result, "work-mode").outcome()).isEqualTo(Outcome.DIFFERENT);
        assertThat(at(result, "attendance").outcome()).isEqualTo(Outcome.ALIGNED);
        assertThat(at(result, "office-location").outcome()).isEqualTo(Outcome.UNKNOWN);
        f.office = new Place("DE", "madrid");
        assertThat(at(evaluate(f.input()), "office-location").outcome()).isEqualTo(Outcome.DIFFERENT);
    }

    @Test
    void explicitZeroAttendanceIsDistinctFromAnUnconfiguredMaximum() {
        var f = new Fixture(); f.maximum = 0;
        assertThat(at(evaluate(f.input()), "attendance").outcome()).isEqualTo(Outcome.ALIGNED);
        f.jobMode = WorkMode.HYBRID; f.days = 1;
        assertThat(evaluate(f.input()).constraintSummary()).isEqualTo(Summary.CONFLICT);
        f.maximum = null;
        assertThat(at(evaluate(f.input()), "attendance").outcome()).isEqualTo(Outcome.NOT_ASSESSED);
    }

    @Test
    void punctuationIsSignificantAndUnicodeWhitespaceIsNormalized() {
        var f = new Fixture(); f.profileTerms = List.of("C", "Spring\u00a0Boot");
        f.jobTerms = List.of("C++", "C#", "Spring Boot");
        var result = evaluate(f.input());
        assertThat(at(result, "technology:c++").outcome()).isEqualTo(Outcome.UNKNOWN);
        assertThat(at(result, "technology:c#").outcome()).isEqualTo(Outcome.UNKNOWN);
        assertThat(at(result, "technology:spring boot").outcome()).isEqualTo(Outcome.ALIGNED);
    }

    @Test
    void repeatedAndPermutedInputsHaveIdenticalResults() {
        var first = new Fixture();
        first.profileTerms = List.of("Java", "JAVA", "Spring Boot");
        first.jobTerms = List.of("Spring Boot", "java", "Java");
        first.policy = countries("ES", "DE", "ES");
        var second = new Fixture();
        second.profileTerms = List.of("Spring Boot", "JAVA", "Java");
        second.jobTerms = List.of("Java", "Spring Boot", "java");
        second.policy = countries("DE", "ES");
        var result = evaluate(first.input());
        assertThat(evaluate(first.input())).isEqualTo(result);
        assertThat(evaluate(second.input())).isEqualTo(result);
    }

    @Test
    void inputsAndOutputsAreDefensiveSnapshots() {
        var terms = new ArrayList<>(List.of("Java", "Spring Boot"));
        var countries = new ArrayList<>(List.of("ES"));
        var f = new Fixture(); f.profileTerms = terms; f.jobTerms = terms;
        f.policy = new ResidencePolicy(ResidenceScope.COUNTRIES, countries);
        var input = f.input();
        var expected = evaluate(input);
        terms.clear(); countries.clear();
        assertThat(evaluate(input)).isEqualTo(expected);
        assertThatThrownBy(() -> input.profile().technologies().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> expected.assessments().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> at(expected, "residence").evidence().opportunityValues().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validationCollectsMultipleErrorsWithoutAnEvaluation() {
        var f = new Fixture(); f.years = -1; f.maximum = 6; f.policy = countries();
        assertThat(engine.evaluate(f.input())).isInstanceOfSatisfying(Invalid.class, invalid ->
                assertThat(invalid.errors()).extracting(ValidationError::field)
                        .containsExactly("profile.totalYears", "profile.maxOfficeDays", "opportunity.residencePolicy.countries"));
    }

    @Test
    void aggregationIgnoresAdvisoryDifferencesAndNeverDropsUnknowns() {
        var baseline = evaluate(new Fixture().input());
        assertThat(MatchingEngine.summarize(List.of())).isEqualTo(Summary.NOT_ASSESSED);
        assertThat(MatchingEngine.summarize(List.of(at(baseline, "attendance"), at(baseline, "experience"))))
                .isEqualTo(Summary.NOT_ASSESSED);
        var f = new Fixture(); f.hybrid(); f.days = 3; f.policy = null;
        var mixed = evaluate(f.input());
        var attendance = at(mixed, "attendance");
        var residence = at(mixed, "residence");
        assertThat(MatchingEngine.summarize(List.of(attendance, residence))).isEqualTo(Summary.CONFLICT);
        assertThat(MatchingEngine.summarize(List.of(residence, attendance))).isEqualTo(Summary.CONFLICT);
        assertThat(residence.outcome()).isEqualTo(Outcome.UNKNOWN);
        assertThat(attendance.reason()).isEqualTo(Reason.ATTENDANCE_EXCEEDS_LIMIT);
    }

    private Evaluation evaluate(MatchingInput input) {
        var result = engine.evaluate(input);
        assertThat(result).isInstanceOf(Evaluation.class);
        return (Evaluation) result;
    }

    private static Assessment at(Evaluation result, String criterion) {
        return result.assessments().stream().filter(a -> a.criterion().equals(criterion)).findFirst().orElseThrow();
    }

    private static Map<String, Outcome> outcomes(Evaluation result) {
        return result.assessments().stream().collect(Collectors.toMap(Assessment::criterion, Assessment::outcome));
    }

    private static Arguments scenario(String name, Consumer<Fixture> change, Summary summary, Object... overrides) {
        var f = new Fixture(); change.accept(f);
        var expected = new LinkedHashMap<String, Outcome>();
        expected.put("role", Outcome.ALIGNED);
        expected.put("technology:java", Outcome.ALIGNED);
        expected.put("technology:spring boot", Outcome.ALIGNED);
        expected.put("experience", Outcome.UNKNOWN);
        expected.put("work-mode", Outcome.ALIGNED);
        expected.put("attendance", Outcome.NOT_ASSESSED);
        expected.put("office-location", Outcome.NOT_ASSESSED);
        expected.put("residence", Outcome.ALIGNED);
        expected.put("salary", Outcome.NOT_ASSESSED);
        expected.put("other-conditions", Outcome.NOT_ASSESSED);
        for (int i = 0; i < overrides.length; i += 2) {
            String key = (String) overrides[i];
            if (overrides[i + 1] == null) expected.remove(key);
            else expected.put(key, (Outcome) overrides[i + 1]);
        }
        return Arguments.of(name, f.input(), expected, summary);
    }

    private static Arguments invalid(String name, Consumer<Fixture> change, String field, ValidationCode code) {
        var f = new Fixture(); change.accept(f);
        return Arguments.of(name, f.input(), field, code);
    }

    private static Experience total(int years) { return new Experience(years, ExperienceScope.TOTAL, null); }
    private static ResidencePolicy countries(String... ids) {
        return new ResidencePolicy(ResidenceScope.COUNTRIES, List.of(ids));
    }
    private static ResidencePolicy unrestricted() { return new ResidencePolicy(ResidenceScope.UNRESTRICTED, null); }

    private static final class Fixture {
        Role profileRole = Role.BACKEND;
        Role jobRole = Role.BACKEND;
        List<String> profileTerms = List.of("Java", "Spring Boot");
        List<String> jobTerms = List.of("Java", "Spring Boot");
        Integer years = 2;
        Place residence = new Place("ES", "madrid");
        Place office = new Place("ES", "madrid");
        WorkMode preferredMode = WorkMode.REMOTE;
        WorkMode jobMode = WorkMode.REMOTE;
        Integer maximum = null;
        Integer days = 0;
        Experience experience = new Experience(2, ExperienceScope.TECHNOLOGY, "Java");
        ResidencePolicy policy = countries("ES");

        void hybrid() { preferredMode = WorkMode.HYBRID; jobMode = WorkMode.HYBRID; maximum = 2; days = 2; }
        MatchingInput input() {
            return new MatchingInput(new Profile(profileRole, profileTerms, years, residence, preferredMode, maximum),
                    new Opportunity(jobRole, jobTerms, experience, jobMode, days, office, policy));
        }
    }
}
