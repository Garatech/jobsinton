package io.github.garatech.jobsinton.matching.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.garatech.jobsinton.matching.MatchingInput.*;
import io.github.garatech.jobsinton.matching.MatchingResult.Summary;
import io.github.garatech.jobsinton.matching.MatchingResult.Reason;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker.*;
import io.github.garatech.jobsinton.ranking.Ratio;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Use the same normalized fictional facts as the current comparison endpoint. */
class CatalogRankingTest {
    private final CompatibilityRanker ranker = new CompatibilityRanker();
    private final Profile backend = new Profile(Role.BACKEND, List.of("Java", "Spring Boot"), 3,
            new Place("ES", "madrid"), WorkMode.REMOTE, null);

    @Test
    void R01_R02_backendAndFrontendUseDifferentEvidenceOnTheSameProfile() {
        var result = (RankedBatch) ranker.rank(backend, Map.of(
                "frontend-react", FictionalMatchingCatalog.find("frontend-react"),
                "backend-java", FictionalMatchingCatalog.find("backend-java")));
        assertThat(result.rankingVersion()).isEqualTo("ranking-v1.0");
        assertThat(result.matchingVersion()).isEqualTo("matching-v1.0");
        assertThat(result.ranked()).extracting(RankedOffer::id).containsExactly("backend-java", "frontend-react");
        assertThat(result.unavailable()).isEmpty();
        var first = result.ranked().getFirst();
        var second = result.ranked().getLast();
        assertThat(first.evaluation().constraintSummary()).isEqualTo(Summary.NEEDS_INFORMATION);
        assertThat(second.evaluation().constraintSummary()).isEqualTo(Summary.NEEDS_INFORMATION);
        assertThat(first.scores()).isEqualTo(scores(75, 0, 20));
        assertThat(second.scores()).isEqualTo(scores(20, 35, 40));
    }

    @Test
    void R03_knownConflictCannotBeOffsetByStrongAdvisorySupport() {
        var profile = new Profile(Role.DATA, List.of("Python", "SQL"), 3, new Place("ES", "valencia"), WorkMode.HYBRID, 1);
        var result = (RankedBatch) ranker.rank(profile, Map.of(
                "data-engineer", FictionalMatchingCatalog.find("data-engineer"),
                "backend-java", FictionalMatchingCatalog.find("backend-java")));
        assertThat(result.ranked()).extracting(RankedOffer::id).containsExactly("backend-java", "data-engineer");
        var remote = result.ranked().getFirst();
        var hybrid = result.ranked().getLast();
        assertThat(remote.scores().netSupport()).contains(new Ratio(-55, 95));
        assertThat(hybrid.scores().netSupport()).contains(new Ratio(80, 100));
        assertThat(hybrid.evaluation().constraintSummary()).isEqualTo(Summary.CONFLICT);
        assertThat(hybrid.evaluation().assessments()).anyMatch(a -> a.criterion().equals("residence")
                && a.reason() == Reason.RESIDENCE_POLICY_UNKNOWN);
    }

    @Test
    void R04_oneConfirmedRoleDoesNotOutrankBroaderConfirmedEvidence() {
        var sparse = new Opportunity(Role.BACKEND, null, null, null, null, null, null);
        var result = (RankedBatch) ranker.rank(backend, Map.of("alpha-sparse", sparse,
                "backend-java", FictionalMatchingCatalog.find("backend-java")));
        var rich = result.ranked().getFirst();
        var limited = result.ranked().getLast();
        assertThat(rich.id()).isEqualTo("backend-java");
        assertThat(limited.scores().agreement()).isEqualTo(rich.scores().agreement()).contains(new Ratio(1, 1));
        assertThat(limited.scores().coverage()).contains(new Ratio(35, 100));
        assertThat(limited.scores().netSupport()).contains(new Ratio(35, 100));
    }

    private static Scores scores(int aligned, int different, int unknown) {
        int k = aligned + different, p = k + unknown;
        return new Scores(new Ratio(aligned, 1), new Ratio(different, 1), new Ratio(unknown, 1),
                Optional.of(new Ratio(aligned, k)), Optional.of(new Ratio(k, p)),
                Optional.of(new Ratio(aligned - different, p)));
    }
}
