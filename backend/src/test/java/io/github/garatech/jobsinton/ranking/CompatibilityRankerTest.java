package io.github.garatech.jobsinton.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import io.github.garatech.jobsinton.matching.MatchingInput.*;
import io.github.garatech.jobsinton.matching.MatchingResult.Summary;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CompatibilityRankerTest {
    private final CompatibilityRanker ranker = new CompatibilityRanker();
    private final Profile profile = profile(List.of("Java"));
    private final Opportunity x = offer(Role.BACKEND, WorkMode.ONSITE, null);
    private final Opportunity y = offer(Role.SAP, WorkMode.HYBRID, null);
    private final Opportunity sparse = offer(null, null, null);

    @Test
    void R05_defaultWeightsPreferTheTargetRole() {
        var batch = rank(ranker, Map.of("x", x, "y", y));
        assertThat(ids(batch)).containsExactly("x", "y");
        assertThat(batch.ranked().getFirst().scores().netSupport()).contains(new Ratio(15, 100));
        assertThat(batch.ranked().getLast().scores().netSupport()).contains(new Ratio(-15, 100));
        assertThat(batch.ranked()).allSatisfy(e -> assertThat(e.scores().coverage()).contains(new Ratio(55, 100)));
    }

    @Test
    void R06_swappingRoleAndModeWeightsReversesOrderButPreservesExplanations() {
        var offers = Map.of("x", x, "y", y);
        var original = rank(ranker, offers);
        var variant = rank(new CompatibilityRanker(new RankingPolicy("fixture-mode-first", 20, 30, 35, 10, 5)), offers);
        assertThat(ids(variant)).containsExactly("y", "x");
        assertThat(variant.rankingVersion()).isEqualTo("fixture-mode-first");
        assertThat(variant.ranked().getFirst().scores().netSupport()).contains(new Ratio(15, 100));
        assertThat(variant.ranked().getLast().scores().netSupport()).contains(new Ratio(-15, 100));
        for (var entry : variant.ranked()) {
            var before = original.ranked().stream().filter(e -> e.id().equals(entry.id())).findFirst().orElseThrow();
            assertThat(entry.evaluation()).isEqualTo(before.evaluation());
            assertThat(entry.scores().coverage()).isEqualTo(before.scores().coverage());
        }
    }

    @Test
    void R07_equalSupportPrefersHigherCoverageBeforeId() {
        var batch = rank(ranker, Map.of("z-covered", x, "a-sparse", offer(null, null, List.of("Java", "SQL"))));
        assertThat(ids(batch)).containsExactly("z-covered", "a-sparse");
        var first = batch.ranked().getFirst().scores();
        var last = batch.ranked().getLast().scores();
        assertThat(first.netSupport()).isEqualTo(last.netSupport()).contains(new Ratio(15, 100));
        assertThat(first.coverage()).contains(new Ratio(55, 100));
        assertThat(last.coverage()).contains(new Ratio(15, 100));
    }

    @Test
    void R08_tiesUseOrdinalIdInEveryInputPermutation() {
        var items = new LinkedHashMap<String, Opportunity>();
        items.put("beta", x); items.put("alpha", x); items.put("gamma", y);
        var expected = rank(ranker, items);
        assertThat(ids(expected)).containsExactly("alpha", "beta", "gamma");
        for (int rotation = 0; rotation < 3; rotation++) {
            assertThat(rank(ranker, items)).isEqualTo(expected);
            assertThat(rank(ranker, items.reversed())).isEqualTo(expected);
            var first = items.pollFirstEntry();
            items.put(first.getKey(), first.getValue());
        }
        items.clear();
        assertThat(ids(expected)).hasSize(3);
        assertThatThrownBy(() -> expected.ranked().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void R09_normalizedTagsShareOneBudgetAndUnlistedTermsRemainUnknown() {
        var standard = offer(null, null, List.of("Java", "SQL", "Redis"));
        var permuted = offer(null, null, List.of(" redis ", "JAVA", "SQL", "  java"));
        var first = rank(ranker, Map.of("a", standard, "b", sparse));
        var second = (RankedBatch) ranker.rank(profile(List.of("Java", "Rust")), Map.of("a", permuted, "b", sparse));
        assertThat(ids(first)).containsExactly("a", "b");
        assertThat(ids(second)).isEqualTo(ids(first));
        assertThat(first.ranked().getFirst().scores()).isEqualTo(second.ranked().getFirst().scores());
        var points = first.ranked().getFirst().scores();
        assertThat(points.aligned()).isEqualTo(new Ratio(10, 1));
        assertThat(points.different()).isEqualTo(new Ratio(0, 1));
        assertThat(points.unknown()).isEqualTo(new Ratio(90, 1));
        assertThat(first.ranked().getLast().scores().unknown()).isEqualTo(new Ratio(100, 1));
    }

    @Test
    void R10_knownZeroAndEvenNegativeEvidencePrecedeNoEvidenceWithinAGroup() {
        var balanced = new Opportunity(null, List.of("Java", "SQL", "Redis"),
                new Experience(4, ExperienceScope.TOTAL, null), null, null, null, null);
        var batch = rank(ranker, Map.of("z-balanced", balanced, "a-unknown", sparse, "negative", y));
        assertThat(ids(batch)).containsExactly("z-balanced", "negative", "a-unknown");
        assertThat(batch.ranked().getFirst().scores().netSupport()).contains(new Ratio(0, 1));
        assertThat(batch.ranked().getFirst().scores().coverage()).contains(new Ratio(20, 100));
        assertThat(batch.ranked().getLast().scores().netSupport()).isEmpty();
        assertThat(batch.ranked().getLast().scores().agreement()).isEmpty();
    }

    @Test
    void R11_inapplicableAndUnknownKeepDifferentCoverageButNeitherHasASortingKey() {
        var variant = new CompatibilityRanker(new RankingPolicy("fixture-office-only", 0, 0, 0, 0, 100));
        var batch = rank(variant, Map.of("alpha", offer(null, WorkMode.REMOTE, null),
                "beta", offer(null, WorkMode.ONSITE, null)));
        assertThat(ids(batch)).containsExactly("alpha", "beta");
        var remote = batch.ranked().getFirst().scores();
        var onsite = batch.ranked().getLast().scores();
        assertThat(remote.coverage()).isEmpty();
        assertThat(onsite.coverage()).contains(new Ratio(0, 1));
        assertThat(remote.unknown()).isEqualTo(new Ratio(0, 1));
        assertThat(onsite.unknown()).isEqualTo(new Ratio(100, 1));
        assertThat(batch.ranked()).allSatisfy(e -> {
            assertThat(e.scores().netSupport()).isEmpty();
            assertThat(e.scores().agreement()).isEmpty();
        });
    }

    @Test
    void R12_residenceUncertaintyIsNotAConflictOrAnAdvisoryBonus() {
        var allowed = new Opportunity(x.role(), x.technologies(), x.experience(), x.mode(), x.officeDays(),
                x.office(), new ResidencePolicy(ResidenceScope.COUNTRIES, List.of("ES")));
        var batch = rank(ranker, Map.of("alpha-unknown", x, "beta-allowed", allowed));
        assertThat(ids(batch)).containsExactly("alpha-unknown", "beta-allowed");
        assertThat(batch.ranked().getFirst().evaluation().constraintSummary()).isEqualTo(Summary.NEEDS_INFORMATION);
        assertThat(batch.ranked().getLast().evaluation().constraintSummary()).isEqualTo(Summary.NO_KNOWN_CONFLICT);
        assertThat(batch.ranked().getFirst().scores()).isEqualTo(batch.ranked().getLast().scores());
    }

    @Test
    void R13_unavailableItemsRemainSeparateAndNoProfileRequestsNoPersonalizedOrder() {
        var items = new LinkedHashMap<String, Opportunity>();
        items.put("z-invalid", new Opportunity(null, null, null, WorkMode.HYBRID, 6, null, null));
        items.put("a-missing", null);
        items.put("conflict", new Opportunity(null, null, null, null, null, null,
                new ResidencePolicy(ResidenceScope.COUNTRIES, List.of("DE"))));
        items.put("unconfirmed", sparse);
        var batch = rank(ranker, items);
        assertThat(ids(batch)).containsExactly("unconfirmed", "conflict");
        assertThat(batch.ranked().getLast().evaluation().constraintSummary()).isEqualTo(Summary.CONFLICT);
        assertThat(batch.unavailable()).extracting(UnavailableOffer::id).containsExactly("a-missing", "z-invalid");
        assertThat(batch.unavailable()).allSatisfy(item -> assertThat(item.validation().errors()).isNotEmpty());
        assertThat(rank(ranker, items.reversed())).isEqualTo(batch);
        assertThat(ranker.rank(null, items)).isEqualTo(NoProfile.INSTANCE);
    }

    @Test
    void R14_subPercentDifferencesUseExactRatiosBeforeTies() {
        var known = IntStream.range(0, 14).mapToObj(i -> "skill" + i).toList();
        var seven = new ArrayList<>(List.of("skill0"));
        IntStream.range(0, 6).forEach(i -> seven.add("other" + i));
        var ninetyNine = new ArrayList<>(known);
        IntStream.range(0, 85).forEach(i -> ninetyNine.add("other" + i));
        Collections.reverse(ninetyNine);
        var batch = (RankedBatch) ranker.rank(profile(known), Map.of(
                "z-seven", offer(null, null, seven), "a-ninety-nine", offer(null, null, ninetyNine)));
        assertThat(ids(batch)).containsExactly("z-seven", "a-ninety-nine");
        assertThat(batch.ranked().getFirst().scores().netSupport()).contains(new Ratio(3, 70));
        assertThat(batch.ranked().getLast().scores().netSupport()).contains(new Ratio(7, 165));
    }

    @Test
    void invalidWeightConfigurationsAreRejectedInsteadOfNormalized() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RankingPolicy("x", -1, 31, 40, 20, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new RankingPolicy("x", 35, 30, 20, 10, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> new RankingPolicy("ranking-v1.0", 20, 30, 35, 10, 5));
        assertThatIllegalArgumentException().isThrownBy(() -> new RankingPolicy("overflow", Integer.MAX_VALUE,
                Integer.MAX_VALUE, 102, 0, 0));
    }

    @Test
    void emptyFilteredSetIsValidButAnUnidentifiedOfferCannotBeOrdered() {
        var empty = rank(ranker, Map.of());
        assertThat(empty.ranked()).isEmpty();
        assertThat(empty.unavailable()).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> rank(ranker, Map.of(" ", sparse)));
    }

    private RankedBatch rank(CompatibilityRanker r, Map<String, Opportunity> items) {
        return (RankedBatch) r.rank(profile, items);
    }
    private static List<String> ids(RankedBatch batch) {
        return batch.ranked().stream().map(RankedOffer::id).toList();
    }
    private static Profile profile(List<String> technologies) {
        return new Profile(Role.BACKEND, technologies, 3, new Place("ES", "madrid"), WorkMode.HYBRID, 5);
    }
    private static Opportunity offer(Role role, WorkMode mode, List<String> tags) {
        Integer days = mode == null ? null : switch (mode) { case REMOTE -> 0; case HYBRID -> 2; case ONSITE -> 5; };
        return new Opportunity(role, tags, null, mode, days, null, null);
    }
}
