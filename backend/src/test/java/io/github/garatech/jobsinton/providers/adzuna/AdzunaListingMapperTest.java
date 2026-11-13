package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.MatchingEngine;
import io.github.garatech.jobsinton.matching.MatchingInput;
import io.github.garatech.jobsinton.matching.MatchingResult;
import io.github.garatech.jobsinton.opportunities.OpportunityListing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static io.github.garatech.jobsinton.providers.adzuna.AdzunaListingMapper.*;
import static io.github.garatech.jobsinton.opportunities.OpportunityListing.SalaryOrigin.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdzunaListingMapperTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final AdzunaListingMapper mapper = new AdzunaListingMapper();
    private static final String ROW = """
            {"id":"fixture-42","title":"Java developer","redirect_url":"https://www.adzuna.es/details/42",
             "company":{"display_name":"Example Workshop"},"location":{"display_name":"Madrid"},
             "description":"An introduction to a fictional company.","created":"2026-11-13T10:15:00+01:00",
             "category":{"label":"IT"},"contract_time":"full_time","contract_type":"permanent","extra":true}
            """;

    private Result map(String input) { return mapper.map(json.readTree(input)); }
    private OpportunityListing listing(String input) { return ((Accepted) map(input)).listing(); }
    private String with(String fields) { return ROW.replace("\"extra\":true", fields); }

    @Test
    void keepsDiscoveryFactsWithoutTurningThemIntoMatchingEvidence() {
        var ad = listing(ROW);
        assertThat(ad.id()).isEqualTo("adzuna:fixture-42");
        assertThat(ad.title()).isEqualTo("Java developer");
        assertThat(ad.company()).isEqualTo("Example Workshop");
        assertThat(ad.locationLabel()).isEqualTo("Madrid");
        assertThat(ad.createdAt()).isEqualTo(Instant.parse("2026-11-13T09:15:00Z"));
        assertThat(ad.referral().toString()).isEqualTo("https://www.adzuna.es/details/42");
        assertThat(ad.categoryLabel()).isEqualTo("IT");
        assertThat(ad.contractTime()).isEqualTo("full_time");
        assertThat(ad.contractType()).isEqualTo("permanent");
        assertThat(ad.salary()).isNull();
        var profile = new MatchingInput.Profile(MatchingInput.Role.BACKEND, List.of("Java"), 3,
                new MatchingInput.Place("ES", "Madrid"), MatchingInput.WorkMode.REMOTE, 0);
        var result = (MatchingResult.Evaluation) new MatchingEngine().evaluate(new MatchingInput(profile, ad.matchingFacts()));
        assertThat(result.constraintSummary()).isEqualTo(MatchingResult.Summary.NEEDS_INFORMATION);
        assertThat(result.assessments()).extracting(MatchingResult.Assessment::reason)
                .contains(MatchingResult.Reason.MODE_UNKNOWN, MatchingResult.Reason.ATTENDANCE_UNKNOWN,
                        MatchingResult.Reason.RESIDENCE_POLICY_UNKNOWN, MatchingResult.Reason.OFFICE_UNKNOWN,
                        MatchingResult.Reason.TECHNOLOGIES_UNKNOWN, MatchingResult.Reason.ROLE_UNKNOWN);
        assertThat(result.assessments()).extracting(MatchingResult.Assessment::outcome)
                .doesNotContain(MatchingResult.Outcome.DIFFERENT);
    }

    @Test
    void retainsTheWholeSuppliedExcerptAndUsesAreaOnlyAsADisplayLabel() {
        var excerpt = "Introduction. ".repeat(80) + "End of supplied fragment.";
        var ad = listing(ROW.replace("An introduction to a fictional company.", excerpt)
                .replace("\"display_name\":\"Madrid\"", "\"area\":[\"España\",null,\"Madrid\"]"));
        assertThat(ad.excerpt()).isEqualTo(excerpt);
        assertThat(ad.locationLabel()).isEqualTo("España, Madrid");
        assertThat(ad.matchingFacts().office()).isNull();
    }

    @Test
    void acceptsMinimalListingAndNormalizesANumericIdWithoutInventingDefaults() {
        var ad = listing("""
                {"id":42,"title":"  Example  ","redirect_url":"https://adzuna.es/details/42",
                 "company":[],"created":"not a date","description":17}
                """);
        assertThat(ad.id()).isEqualTo("adzuna:42");
        assertThat(ad.title()).isEqualTo("Example");
        assertThat(ad.company()).isNull();
        assertThat(ad.createdAt()).isNull();
        assertThat(ad.excerpt()).isNull();
        assertThat(ad.locationLabel()).isNull();
    }

    @Test
    void rejectsUnidentifiableOrUntitledRowsIndividually() {
        assertThat(map("null")).isEqualTo(new Rejected(Reason.INVALID_ROW));
        assertThat(map(ROW.replace("\"fixture-42\"", "false"))).isEqualTo(new Rejected(Reason.MISSING_ID));
        assertThat(map(ROW.replace("Java developer", "  "))).isEqualTo(new Rejected(Reason.MISSING_TITLE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://adzuna.es/job/42", "https://adzuna.es.example.org/job/42",
            "https://user@adzuna.es/job/42", "https://adzuna.es:8080/job/42", "https://adzuna.es/",
            "https://adzuna.es/job/42?%61pp_KEY=fake", "not a URL", ""})
    void rejectsInvalidReferrals(String url) {
        assertThat(map(ROW.replace("https://www.adzuna.es/details/42", url))).isEqualTo(new Rejected(Reason.INVALID_REFERRAL));
    }

    @Test
    void preservesOneSidedBoundsAndSeparatesPredictionsFromAdvertisedPay() {
        var ad = listing(with("\"salary_min\":42000.50,\"salary_is_predicted\":\"0\"")).salary();
        assertThat(ad.minimum()).isEqualByComparingTo("42000.50");
        assertThat(ad.maximum()).isNull();
        assertThat(ad.origin()).isEqualTo(ADVERTISED);
        var estimate = listing(with("\"salary_max\":50000,\"salary_is_predicted\":1")).salary();
        assertThat(estimate.minimum()).isNull();
        assertThat(estimate.maximum()).isEqualByComparingTo("50000");
        assertThat(estimate.origin()).isEqualTo(PREDICTED);
        assertThat(listing(with("\"salary_min\":1")).salary().origin()).isEqualTo(UNSPECIFIED);
        assertThat(listing(with("\"salary_min\":1,\"salary_is_predicted\":false")).salary().origin()).isEqualTo(ADVERTISED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"salary_max\":0", "\"salary_min\":-1", "\"salary_min\":500,\"salary_max\":300",
            "\"salary_min\":\"unknown\",\"salary_max\":500", "\"salary_is_predicted\":1"})
    void invalidOrAbsentSalaryDoesNotDiscardTheListing(String fields) {
        assertThat(listing(with(fields)).salary()).isNull();
    }
}
