package io.github.garatech.jobsinton.providers.ats;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Aggregates live postings across all configured public ATS company boards in parallel,
 * isolating individual board failures and caching the combined feed in memory.
 */
public final class AtsFeedService {
    private static final Pattern SAFE_POSTING_ID = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    private final CompanyRegistry registry;
    private final Map<AtsProviderType, AtsProviderAdapter> adapters;
    private final SmartRecruitersAdapter smartRecruitersAdapter;
    private final Clock clock;
    private final Duration cacheTtl;

    private final Object lock = new Object();
    private volatile CachedFeed cache;
    private CompletableFuture<CachedFeed> inFlight;

    private record BoardOutcome(BoardStatus status, List<RawAtsJob> jobs) {}

    private record CachedFeed(Instant fetchedAt, List<BoardStatus> boards, List<RawAtsJob> jobs) {
        AtsFeedResponse toResponse(Instant now, boolean cached) {
            long ageSeconds = Math.max(0L, Duration.between(fetchedAt, now).getSeconds());
            int succeeded = (int) boards.stream().filter(BoardStatus::ok).count();
            int failed = boards.size() - succeeded;
            List<AtsJob> apiJobs = jobs.stream().map(job -> job.toApiJob(now)).toList();
            return new AtsFeedResponse(
                    fetchedAt.toString(),
                    cached,
                    cached ? ageSeconds : 0L,
                    boards.size(),
                    succeeded,
                    failed,
                    boards,
                    apiJobs
            );
        }
    }

    public AtsFeedService(
            CompanyRegistry registry,
            List<AtsProviderAdapter> adapterList,
            SmartRecruitersAdapter smartRecruitersAdapter,
            Clock clock,
            Duration cacheTtl
    ) {
        this.registry = registry;
        this.adapters = new EnumMap<>(AtsProviderType.class);
        for (AtsProviderAdapter adapter : adapterList) {
            this.adapters.put(adapter.type(), adapter);
        }
        this.smartRecruitersAdapter = smartRecruitersAdapter;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    public List<CompanyBoard> boards() {
        return registry.boards();
    }

    public AtsFeedResponse getFeed(boolean forceRefresh) {
        Instant now = clock.instant();
        if (!forceRefresh) {
            CachedFeed current = cache;
            if (current != null && !isExpired(current, now)) {
                return current.toResponse(now, true);
            }
        }

        CompletableFuture<CachedFeed> future;
        boolean owner = false;
        synchronized (lock) {
            if (!forceRefresh && cache != null && !isExpired(cache, clock.instant())) {
                return cache.toResponse(clock.instant(), true);
            }
            if (inFlight != null && !inFlight.isDone()) {
                future = inFlight;
            } else {
                future = new CompletableFuture<>();
                inFlight = future;
                owner = true;
            }
        }

        if (owner) {
            try {
                CachedFeed fresh = fetchAllBoards();
                synchronized (lock) {
                    cache = fresh;
                    inFlight = null;
                }
                future.complete(fresh);
                return fresh.toResponse(clock.instant(), false);
            } catch (Throwable error) {
                synchronized (lock) {
                    inFlight = null;
                }
                future.completeExceptionally(error);
                throw error;
            }
        }

        CachedFeed joined = future.join();
        return joined.toResponse(clock.instant(), false);
    }

    /** On-demand SmartRecruiters posting detail hydration for registered boards only. */
    public Optional<SmartRecruitersDetail> hydrateSmartRecruitersDetail(String companySlug, String postingId) {
        if (postingId == null || !SAFE_POSTING_ID.matcher(postingId).matches()) {
            throw new IllegalArgumentException("Invalid SmartRecruiters posting id");
        }
        Optional<CompanyBoard> boardOpt = registry.find(AtsProviderType.SMARTRECRUITERS, companySlug);
        if (boardOpt.isEmpty()) {
            return Optional.empty();
        }
        SmartRecruitersDetail detail = smartRecruitersAdapter.fetchDetail(boardOpt.get(), postingId);
        updateCachedSmartRecruitersJob(detail);
        return Optional.of(detail);
    }

    private void updateCachedSmartRecruitersJob(SmartRecruitersDetail detail) {
        synchronized (lock) {
            CachedFeed current = cache;
            if (current == null) {
                return;
            }
            boolean changed = false;
            List<RawAtsJob> updated = new ArrayList<>(current.jobs().size());
            for (RawAtsJob job : current.jobs()) {
                if (job.compositeId().equals(detail.id())) {
                    updated.add(new RawAtsJob(
                            job.sourceId(),
                            job.provider(),
                            job.companySlug(),
                            job.companyName(),
                            job.title(),
                            job.location(),
                            job.workplaceType(),
                            job.publishedAt(),
                            detail.description(),
                            detail.applyUrl() != null ? detail.applyUrl() : job.applyUrl(),
                            job.department(),
                            job.compensation(),
                            detail.hasFullDescription()
                    ));
                    changed = true;
                } else {
                    updated.add(job);
                }
            }
            if (changed) {
                cache = new CachedFeed(current.fetchedAt(), current.boards(), List.copyOf(updated));
            }
        }
    }

    private boolean isExpired(CachedFeed snapshot, Instant now) {
        if (cacheTtl.isZero() || cacheTtl.isNegative()) {
            return true;
        }
        return Duration.between(snapshot.fetchedAt(), now).compareTo(cacheTtl) >= 0;
    }

    private CachedFeed fetchAllBoards() {
        List<CompanyBoard> boards = registry.boards();
        List<BoardOutcome> outcomes;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<BoardOutcome>> futures = boards.stream()
                    .map(board -> CompletableFuture.supplyAsync(() -> fetchBoardSafely(board), executor))
                    .toList();
            outcomes = futures.stream().map(CompletableFuture::join).toList();
        }

        List<BoardStatus> statuses = new ArrayList<>(outcomes.size());
        Map<String, RawAtsJob> deduplicated = new LinkedHashMap<>();
        for (BoardOutcome outcome : outcomes) {
            statuses.add(outcome.status());
            for (RawAtsJob job : outcome.jobs()) {
                deduplicated.putIfAbsent(job.compositeId(), job);
            }
        }

        List<RawAtsJob> sortedJobs = new ArrayList<>(deduplicated.values());
        sortedJobs.sort(Comparator
                .comparing(RawAtsJob::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RawAtsJob::companyName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RawAtsJob::title, String.CASE_INSENSITIVE_ORDER));

        return new CachedFeed(clock.instant(), List.copyOf(statuses), List.copyOf(sortedJobs));
    }

    private BoardOutcome fetchBoardSafely(CompanyBoard board) {
        long startNanos = System.nanoTime();
        AtsProviderType type = board.providerType();
        AtsProviderAdapter adapter = adapters.get(type);
        if (adapter == null) {
            return new BoardOutcome(
                    new BoardStatus(board.slug(), board.name(), type.id(), type.displayName(), false, 0, 0L,
                            "No adapter registered"),
                    List.of()
            );
        }
        try {
            List<RawAtsJob> jobs = adapter.fetch(board);
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            return new BoardOutcome(
                    new BoardStatus(board.slug(), board.name(), type.id(), type.displayName(), true, jobs.size(), elapsedMs, null),
                    jobs
            );
        } catch (AtsFetchException e) {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            return new BoardOutcome(
                    new BoardStatus(board.slug(), board.name(), type.id(), type.displayName(), false, 0, elapsedMs,
                            sanitizeError(e.getMessage())),
                    List.of()
            );
        } catch (RuntimeException e) {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            return new BoardOutcome(
                    new BoardStatus(board.slug(), board.name(), type.id(), type.displayName(), false, 0, elapsedMs,
                            "Unexpected error reading board"),
                    List.of()
            );
        }
    }

    private static String sanitizeError(String message) {
        if (message == null || message.isBlank()) {
            return "Board request failed";
        }
        String singleLine = message.replaceAll("\\s+", " ").strip();
        return singleLine.length() > 140 ? singleLine.substring(0, 140) : singleLine;
    }
}
