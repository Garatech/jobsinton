package io.github.garatech.jobsinton.providers.ats;

import java.util.List;

/** Aggregated Multi-ATS feed response including cache, background-revalidation metadata, and per-board health. */
public record AtsFeedResponse(
        String fetchedAt,
        boolean cached,
        boolean refreshing,
        long cacheAgeSeconds,
        int totalBoards,
        int succeededBoards,
        int failedBoards,
        List<BoardStatus> boards,
        List<AtsJob> jobs
) {
    public AtsFeedResponse(
            String fetchedAt,
            boolean cached,
            long cacheAgeSeconds,
            int totalBoards,
            int succeededBoards,
            int failedBoards,
            List<BoardStatus> boards,
            List<AtsJob> jobs
    ) {
        this(fetchedAt, cached, false, cacheAgeSeconds, totalBoards, succeededBoards, failedBoards, boards, jobs);
    }
}
