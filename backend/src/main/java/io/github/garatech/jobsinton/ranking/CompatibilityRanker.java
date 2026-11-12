package io.github.garatech.jobsinton.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.garatech.jobsinton.matching.MatchingEngine;
import io.github.garatech.jobsinton.matching.MatchingInput;
import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;
import io.github.garatech.jobsinton.matching.MatchingInput.Profile;
import io.github.garatech.jobsinton.matching.MatchingResult;
import io.github.garatech.jobsinton.matching.MatchingResult.*;

/** One normalized profile per batch. All comparisons come from the same matching engine. */
public final class CompatibilityRanker {
    private static final String SUPPORTED_MATCHING = "matching-v1.0";
    private final MatchingEngine engine = new MatchingEngine();
    private final RankingPolicy policy;

    public CompatibilityRanker() { this(RankingPolicy.DEFAULT); }

    CompatibilityRanker(RankingPolicy policy) { this.policy = Objects.requireNonNull(policy); }

    public sealed interface Result permits NoProfile, RankedBatch {}
    public enum NoProfile implements Result { INSTANCE }

    public record RankedBatch(String rankingVersion, String matchingVersion,
                              List<RankedOffer> ranked, List<UnavailableOffer> unavailable) implements Result {
        public RankedBatch {
            ranked = List.copyOf(ranked);
            unavailable = List.copyOf(unavailable);
        }
    }

    public record RankedOffer(String id, Evaluation evaluation, Scores scores) {}
    public record UnavailableOffer(String id, Invalid validation) {}
    public record Scores(Ratio aligned, Ratio different, Ratio unknown, Optional<Ratio> agreement,
                         Optional<Ratio> coverage, Optional<Ratio> netSupport) {}

    /**
     * The caller supplies an already filtered set keyed by stable opportunity ID. A missing
     * opportunity value is evaluated as invalid and retained in the separate unavailable tail.
     */
    public Result rank(Profile profile, Map<String, Opportunity> offers) {
        if (profile == null) return NoProfile.INSTANCE;
        Objects.requireNonNull(offers, "offers");
        if (!SUPPORTED_MATCHING.equals(MatchingEngine.RULES_VERSION)) {
            throw new IllegalStateException("Review the ranking policy before using a new matching version.");
        }
        var ranked = new ArrayList<RankedOffer>();
        var unavailable = new ArrayList<UnavailableOffer>();
        offers.forEach((id, offer) -> {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("A stable opportunity ID is required.");
            MatchingResult result = engine.evaluate(new MatchingInput(profile, offer));
            switch (result) {
                case Evaluation evaluation -> ranked.add(new RankedOffer(id, evaluation, score(evaluation)));
                case Invalid invalid -> unavailable.add(new UnavailableOffer(id, invalid));
            }
        });
        ranked.sort(CompatibilityRanker::compare);
        unavailable.sort(Comparator.comparing(UnavailableOffer::id));
        return new RankedBatch(policy.version(), SUPPORTED_MATCHING, ranked, unavailable);
    }

    private Scores score(Evaluation evaluation) {
        // Only technology subdivides a dimension. Using its term count as a common unit
        // denominator keeps every allocation integral, even for 1/7 versus 14/99.
        int scale = Math.max(1, (int) evaluation.assessments().stream()
                .filter(a -> a.dimension() == Dimension.TECHNOLOGY && a.outcome() != Outcome.NOT_ASSESSED).count());
        int aligned = 0, different = 0, unknown = 0;
        for (var a : evaluation.assessments()) {
            if (a.kind() != Kind.ADVISORY || a.outcome() == Outcome.NOT_ASSESSED) continue;
            int weight = policy.weight(a.dimension());
            int units = a.dimension() == Dimension.TECHNOLOGY ? weight : weight * scale;
            switch (a.outcome()) {
                case ALIGNED -> aligned += units;
                case DIFFERENT -> different += units;
                case UNKNOWN -> unknown += units;
                default -> throw new IllegalStateException("Unexpected applicable outcome: " + a.outcome());
            }
        }
        int known = aligned + different;
        int applicable = known + unknown;
        return new Scores(new Ratio(aligned, scale), new Ratio(different, scale), new Ratio(unknown, scale),
                ratio(aligned, known), ratio(known, applicable),
                known == 0 ? Optional.empty() : ratio(aligned - different, applicable));
    }

    private static Optional<Ratio> ratio(int numerator, int denominator) {
        return denominator == 0 ? Optional.empty() : Optional.of(new Ratio(numerator, denominator));
    }

    private static int compare(RankedOffer left, RankedOffer right) {
        int group = Boolean.compare(left.evaluation().constraintSummary() == Summary.CONFLICT,
                                    right.evaluation().constraintSummary() == Summary.CONFLICT);
        if (group != 0) return group;
        var l = left.scores().netSupport();
        var r = right.scores().netSupport();
        if (l.isPresent() != r.isPresent()) return l.isPresent() ? -1 : 1;
        if (l.isPresent()) {
            int support = r.orElseThrow().compareTo(l.orElseThrow());
            if (support != 0) return support;
            int coverage = right.scores().coverage().orElseThrow().compareTo(left.scores().coverage().orElseThrow());
            if (coverage != 0) return coverage;
        }
        return left.id().compareTo(right.id());
    }
}
