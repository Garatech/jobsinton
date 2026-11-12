package io.github.garatech.jobsinton.ranking;

import io.github.garatech.jobsinton.matching.MatchingResult.Dimension;

/** Package-private reference variants do not add editable product settings. */
record RankingPolicy(String version, int role, int technology, int mode, int experience, int office) {
    static final RankingPolicy DEFAULT = new RankingPolicy("ranking-v1.0", 35, 30, 20, 10, 5);

    RankingPolicy {
        if (version == null || version.isBlank() || role < 0 || technology < 0 || mode < 0
                || experience < 0 || office < 0 || (long) role + technology + mode + experience + office != 100) {
            throw new IllegalArgumentException("A named policy needs nonnegative weights totalling 100.");
        }
        if ("ranking-v1.0".equals(version)
                && (role != 35 || technology != 30 || mode != 20 || experience != 10 || office != 5)) {
            throw new IllegalArgumentException("Changed weights require a different ranking version.");
        }
    }

    int weight(Dimension dimension) {
        return switch (dimension) {
            case ROLE -> role;
            case TECHNOLOGY -> technology;
            case WORK_MODE -> mode;
            case EXPERIENCE -> experience;
            case OFFICE_LOCATION -> office;
            default -> 0;
        };
    }
}
