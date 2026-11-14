import io.github.garatech.jobsinton.matching.MatchingInput.*;
import io.github.garatech.jobsinton.matching.MatchingResult.*;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker.RankedBatch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

// Java 21 source launcher; only the project's compiled domain classes are needed.
class PageStudy {
    private static final Profile PROFILE = new Profile(Role.BACKEND, List.of("Java"), 2,
            new Place("ES", "madrid"), WorkMode.HYBRID, 1);

    public static void main(String[] rows) {
        if (rows.length > 10) throw new IllegalArgumentException("Expected at most one page.");
        var offers = new LinkedHashMap<String, Opportunity>();
        for (String row : rows) {
            // The PowerShell adapter passes only three controlled values; IDs are local ordinals.
            String[] fields = row.split(":", -1);
            if (fields.length != 3) throw new IllegalArgumentException("Invalid study row.");
            var role = fields[0].equals("-") ? null : Role.valueOf(fields[0]);
            var mode = fields[1].equals("-") ? null : WorkMode.valueOf(fields[1]);
            var days = fields[2].equals("-") ? null : Integer.valueOf(fields[2]);
            offers.put(Integer.toString(offers.size()), new Opportunity(role, null, null, mode, days, null, null));
        }

        var batch = (RankedBatch) new CompatibilityRanker().rank(PROFILE, offers);
        if (!batch.unavailable().isEmpty()) throw new IllegalArgumentException("Inconsistent mapped facts; no report produced.");
        int roles = 0, modes = 0, within = 0, exceeds = 0, unknown = 0, conflicts = 0, noEvidence = 0;
        for (var offer : batch.ranked()) {
            boolean hasEvidence = false;
            if (offer.evaluation().constraintSummary() == Summary.CONFLICT) conflicts++;
            for (var assessment : offer.evaluation().assessments()) {
                boolean known = assessment.outcome() == Outcome.ALIGNED || assessment.outcome() == Outcome.DIFFERENT;
                switch (assessment.dimension()) {
                    case ROLE -> { if (known) { roles++; hasEvidence = true; } }
                    case WORK_MODE -> { if (known) { modes++; hasEvidence = true; } }
                    case ATTENDANCE -> {
                        switch (assessment.reason()) {
                            case ATTENDANCE_WITHIN_LIMIT -> { within++; hasEvidence = true; }
                            case ATTENDANCE_EXCEEDS_LIMIT -> { exceeds++; hasEvidence = true; }
                            case ATTENDANCE_UNKNOWN -> unknown++;
                            default -> throw new IllegalStateException("Unexpected attendance for the study profile.");
                        }
                    }
                    default -> { }
                }
            }
            if (!hasEvidence) noEvidence++;
        }

        // At most 45 pairs. Count each offer once even if it ties with several others.
        var ranked = batch.ranked();
        int tiedOffers = 0, tiedPairs = 0;
        for (int i = 0; i < ranked.size(); i++) {
            boolean tied = false;
            for (int j = 0; j < ranked.size(); j++) {
                if (i != j && CompatibilityRanker.compareEvidence(ranked.get(i), ranked.get(j)) == 0) {
                    tied = true;
                    if (j > i) tiedPairs++;
                }
            }
            if (tied) tiedOffers++;
        }
        System.out.printf(Locale.ROOT, """
                {
                  "studyVersion": "adzuna-page-v1",
                  "rankingVersion": "%s",
                  "matchingVersion": "%s",
                  "listings": %d,
                  "comparable": {"role": %d, "workMode": %d, "weeklyAttendance": %d},
                  "attendance": {"withinLimit": %d, "exceedsLimit": %d, "unknown": %d},
                  "explicitConflicts": %d,
                  "withoutComparableFacts": %d,
                  "ties": {"listings": %d, "pairs": %d, "possiblePairs": %d}
                }
                """, batch.rankingVersion(), batch.matchingVersion(), rows.length, roles, modes, within + exceeds,
                within, exceeds, unknown, conflicts, noEvidence, tiedOffers, tiedPairs, rows.length * (rows.length - 1) / 2);
    }
}
