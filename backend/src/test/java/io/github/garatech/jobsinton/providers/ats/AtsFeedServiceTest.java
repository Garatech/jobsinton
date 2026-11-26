package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AtsFeedServiceTest {

    @Test
    void loadsDefaultRegistryAcrossAllNineAtsProviders() {
        CompanyRegistry registry = CompanyRegistry.loadDefault();
        assertThat(registry.boards()).hasSize(334);
        assertThat(registry.boards()).extracting(CompanyBoard::providerType)
                .containsOnly(
                        AtsProviderType.GREENHOUSE,
                        AtsProviderType.LEVER,
                        AtsProviderType.SMARTRECRUITERS,
                        AtsProviderType.WORKABLE,
                        AtsProviderType.ASHBY,
                        AtsProviderType.PERSONIO,
                        AtsProviderType.RECRUITEE,
                        AtsProviderType.TEAMTAILOR,
                        AtsProviderType.FACTORIAL
                );
        assertThat(registry.find(AtsProviderType.LEVER, "tomtom"))
                .isPresent()
                .get()
                .extracting(CompanyBoard::isEuRegion)
                .isEqualTo(true);
    }

    @Test
    void isolatesBoardFailuresComputesFreshnessAndHonorsCacheTtl() {
        Instant fixedNow = Instant.parse("2026-11-17T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        AtomicInteger greenhouseCalls = new AtomicInteger();

        CompanyBoard okBoard = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        CompanyBoard brokenBoard = new CompanyBoard("broken", "Broken Co", "ashby", null, "Remote");
        CompanyRegistry registry = new CompanyRegistry(List.of(okBoard, brokenBoard));

        AtsProviderAdapter gh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard board) {
                greenhouseCalls.incrementAndGet();
                return List.of(
                        new RawAtsJob("1", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Older Role", "Madrid",
                                "hybrid", Instant.parse("2026-11-14T12:00:00Z"), "Java", "https://example.com/1",
                                "Eng", null, true),
                        new RawAtsJob("2", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Fresh Role", "Madrid",
                                "remote", Instant.parse("2026-11-17T06:00:00Z"), "Spring Boot", "https://example.com/2",
                                "Eng", null, true),
                        // Duplicate sourceId should be deduplicated cleanly.
                        new RawAtsJob("2", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Fresh Role", "Madrid",
                                "remote", Instant.parse("2026-11-17T06:00:00Z"), "Spring Boot", "https://example.com/2",
                                "Eng", null, true)
                );
            }
        };

        AtsProviderAdapter ashbyFailing = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.ASHBY;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard board) {
                throw new AtsFetchException("Timed out after 8000ms");
            }
        };

        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(null, 0);
        AtsFeedService service = new AtsFeedService(registry, List.of(gh, ashbyFailing), sr, clock, Duration.ofMinutes(5));

        AtsFeedResponse first = service.getFeed(false);
        assertThat(first.cached()).isFalse();
        assertThat(first.totalBoards()).isEqualTo(2);
        assertThat(first.succeededBoards()).isEqualTo(1);
        assertThat(first.failedBoards()).isEqualTo(1);
        assertThat(first.jobs()).hasSize(2);
        // Newest job first, with exact hoursSincePublished computed against Clock.
        assertThat(first.jobs().get(0).title()).isEqualTo("Fresh Role");
        assertThat(first.jobs().get(0).hoursSincePublished()).isEqualTo(6L);
        assertThat(first.jobs().get(1).title()).isEqualTo("Older Role");
        assertThat(first.jobs().get(1).hoursSincePublished()).isEqualTo(72L);
        assertThat(first.boards().stream().filter(b -> !b.ok()).findFirst().orElseThrow().error())
                .contains("Timed out");

        // Second call within TTL uses cache without calling adapters again.
        AtsFeedResponse second = service.getFeed(false);
        assertThat(second.cached()).isTrue();
        assertThat(greenhouseCalls.get()).isEqualTo(1);

        // Force refresh bypasses cache.
        AtsFeedResponse forced = service.getFeed(true);
        assertThat(forced.cached()).isFalse();
        assertThat(greenhouseCalls.get()).isEqualTo(2);
    }

    @Test
    void firstSeenRegistryPreventsEditedOldJobsFromFakingFreshAge() {
        Instant fixedNow = Instant.parse("2026-11-18T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        CompanyBoard board = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        CompanyRegistry registry = new CompanyRegistry(List.of(board));

        FirstSeenRegistry firstSeen = new FirstSeenRegistry();
        firstSeen.recordBatch(
                List.of(new RawAtsJob("101", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Edited Old Role", "Madrid",
                        "hybrid", Instant.parse("2026-11-13T12:00:00Z"), "Java", "https://example.com/101", "Eng", null, true)),
                Instant.parse("2026-11-13T12:00:00Z")
        );

        AtsProviderAdapter gh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard b) {
                return List.of(
                        new RawAtsJob("101", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Edited Old Role", "Madrid",
                                "hybrid", Instant.parse("2026-11-18T10:00:00Z"), "Java", "https://example.com/101",
                                "Eng", null, true)
                );
            }
        };

        AtsFeedService service = new AtsFeedService(
                registry, List.of(gh), new SmartRecruitersAdapter(null, 0), clock, Duration.ofMinutes(5), firstSeen
        );
        AtsFeedResponse feed = service.getFeed(false);
        assertThat(feed.jobs()).hasSize(1);
        assertThat(feed.jobs().getFirst().hoursSincePublished()).isEqualTo(120L);
        assertThat(feed.jobs().getFirst().firstSeenAt()).isEqualTo("2026-11-13T12:00:00Z");
    }

    @Test
    void purgesNonEuropeanPostingsAtIngestionWhilePreservingSpainEuropeAndEuRemote() {
        Instant fixedNow = Instant.parse("2026-11-18T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        CompanyBoard board = new CompanyBoard("datadog", "Datadog", "greenhouse", null, "Madrid");
        CompanyRegistry registry = new CompanyRegistry(List.of(board));

        AtsProviderAdapter gh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard b) {
                return List.of(
                        new RawAtsJob("es-1", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Python Data Engineer", "Madrid, Spain", "hybrid",
                                Instant.parse("2026-11-18T10:00:00Z"), "Python, SQL, Spark", "https://example.com/es-1",
                                "Data", null, true),
                        new RawAtsJob("es-muni", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Junior Java Developer", "Sant Cugat, Spain", "hybrid",
                                Instant.parse("2026-11-18T09:30:00Z"), "Java, Spring Boot", "https://example.com/es-muni",
                                "Eng", null, true),
                        new RawAtsJob("eu-1", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Backend Software Engineer", "Berlin, Germany", "hybrid",
                                Instant.parse("2026-11-18T09:00:00Z"), "Go, Java, Kubernetes", "https://example.com/eu-1",
                                "Eng", null, true),
                        new RawAtsJob("eu-rem", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "AI Engineer", "Remote - Europe (CET)", "remote",
                                Instant.parse("2026-11-18T08:00:00Z"), "Python, LLM", "https://example.com/eu-rem",
                                "AI", null, true),
                        new RawAtsJob("multi-eu-rem", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Full Stack Engineer", "Remote - US, UK, Germany", "remote",
                                Instant.parse("2026-11-18T07:30:00Z"), "React, Node.js", "https://example.com/multi-eu-rem",
                                "Eng", null, true),
                        new RawAtsJob("uk-only-lock", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Platform Engineer", "London, United Kingdom (UK only)", "remote",
                                Instant.parse("2026-11-18T11:00:00Z"), "AWS", "https://example.com/uk-only-lock",
                                "Eng", null, true),
                        new RawAtsJob("us-sf", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Staff Engineer", "San Francisco, CA", "onsite",
                                Instant.parse("2026-11-18T11:00:00Z"), "Go", "https://example.com/us-sf",
                                "Eng", null, true),
                        new RawAtsJob("ca-tor", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Frontend Developer", "Toronto, CA, Remote", "remote",
                                Instant.parse("2026-11-18T11:00:00Z"), "React", "https://example.com/ca-tor",
                                "Eng", null, true),
                        new RawAtsJob("us-rem", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "Cloud Engineer", "Remote - US Only", "remote",
                                Instant.parse("2026-11-18T11:00:00Z"), "Must reside in the United States.", "https://example.com/us-rem",
                                "Eng", null, true),
                        new RawAtsJob("in-blr", AtsProviderType.GREENHOUSE, "datadog", "Datadog",
                                "QA Automation Engineer", "Bengaluru, India", "onsite",
                                Instant.parse("2026-11-18T11:00:00Z"), "Selenium", "https://example.com/in-blr",
                                "QA", null, true)
                );
            }
        };

        AtsFeedService service = new AtsFeedService(
                registry, List.of(gh), new SmartRecruitersAdapter(null, 0), clock, Duration.ofMinutes(5)
        );
        AtsFeedResponse feed = service.getFeed(false);
        assertThat(feed.jobs()).extracting(AtsJob::sourceId)
                .containsExactly("es-1", "es-muni", "eu-1", "eu-rem", "multi-eu-rem");
        assertThat(feed.boards().getFirst().jobCount()).isEqualTo(5);
    }

    @Test
    void restoresFeedInstantlyFromDiskSnapshotAndRevalidatesInBackgroundWithoutBlocking(@TempDir Path tempDir) throws Exception {
        Path snapshotFile = tempDir.resolve("ats-feed-cache.json");
        AtomicReference<Instant> nowRef = new AtomicReference<>(Instant.parse("2026-11-26T10:00:00Z"));
        Clock mutableClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return nowRef.get();
            }
        };

        CompanyBoard board = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        CompanyRegistry registry = new CompanyRegistry(List.of(board));
        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(null, 0);

        AtsProviderAdapter initialGh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard b) {
                return List.of(
                        new RawAtsJob("101", AtsProviderType.GREENHOUSE, "cabify", "Cabify",
                                "Junior Java Developer", "Madrid, Spain", "hybrid",
                                Instant.parse("2026-11-26T08:00:00Z"), "Java 21, Spring Boot",
                                "https://example.com/101", "Engineering", "€32K", true)
                );
            }
        };

        // First service instance fetches live and persists data/ats-feed-cache.json.
        AtsFeedService firstInstance = new AtsFeedService(
                registry,
                List.of(initialGh),
                sr,
                mutableClock,
                Duration.ofMinutes(10),
                new FirstSeenRegistry(),
                24,
                new AtsFeedCacheStore(snapshotFile)
        );
        AtsFeedResponse initialFeed = firstInstance.getFeed(false);
        assertThat(initialFeed.cached()).isFalse();
        assertThat(initialFeed.jobs()).hasSize(1);
        assertThat(snapshotFile).isRegularFile();

        // Advance time by 30 minutes (past the 10-minute TTL) and simulate a restart with a gated upstream adapter.
        nowRef.set(Instant.parse("2026-11-26T10:30:00Z"));
        CountDownLatch backgroundStarted = new CountDownLatch(1);
        CountDownLatch releaseUpstream = new CountDownLatch(1);

        AtsProviderAdapter slowUpdatedGh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard b) {
                backgroundStarted.countDown();
                try {
                    releaseUpstream.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return List.of(
                        new RawAtsJob("101", AtsProviderType.GREENHOUSE, "cabify", "Cabify",
                                "Junior Java Developer", "Madrid, Spain", "hybrid",
                                Instant.parse("2026-11-26T08:00:00Z"), "Java 21, Spring Boot",
                                "https://example.com/101", "Engineering", "€32K", true),
                        new RawAtsJob("102", AtsProviderType.GREENHOUSE, "cabify", "Cabify",
                                "Junior Frontend Developer", "Madrid, Spain", "remote",
                                Instant.parse("2026-11-26T10:25:00Z"), "React, TypeScript",
                                "https://example.com/102", "Engineering", null, true)
                );
            }
        };

        AtsFeedService restartedInstance = new AtsFeedService(
                registry,
                List.of(slowUpdatedGh),
                sr,
                mutableClock,
                Duration.ofMinutes(10),
                new FirstSeenRegistry(),
                24,
                new AtsFeedCacheStore(snapshotFile)
        );

        // Stale-While-Revalidate returns the disk-loaded snapshot immediately without waiting for slowUpdatedGh.
        AtsFeedResponse instantStale = restartedInstance.getFeed(false);
        assertThat(instantStale.cached()).isTrue();
        assertThat(instantStale.refreshing()).isTrue();
        assertThat(instantStale.jobs()).hasSize(1);
        assertThat(instantStale.jobs().getFirst().description()).isEqualTo("Java 21, Spring Boot");
        assertThat(instantStale.jobs().getFirst().compensation()).isEqualTo("€32K");

        assertThat(backgroundStarted.await(2, TimeUnit.SECONDS)).isTrue();
        releaseUpstream.countDown();

        // Join the single-flight background refresh and verify both jobs are now cached with refreshing=false.
        restartedInstance.triggerBackgroundRefresh().join();
        AtsFeedResponse refreshed = restartedInstance.getFeed(false);
        assertThat(refreshed.cached()).isTrue();
        assertThat(refreshed.refreshing()).isFalse();
        assertThat(refreshed.jobs()).extracting(AtsJob::sourceId).containsExactly("102", "101");
    }

    @Test
    void warmUpOnStartupPreloadsCacheAndOfflineRefreshPreservesExistingJobs() {
        AtomicReference<Instant> nowRef = new AtomicReference<>(Instant.parse("2026-11-26T12:00:00Z"));
        Clock mutableClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return nowRef.get();
            }
        };

        AtomicBoolean offline = new AtomicBoolean(false);
        AtomicInteger calls = new AtomicInteger();
        CompanyBoard board = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        CompanyRegistry registry = new CompanyRegistry(List.of(board));

        AtsProviderAdapter gh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard b) {
                calls.incrementAndGet();
                if (offline.get()) {
                    throw new AtsFetchException("Network unreachable");
                }
                return List.of(
                        new RawAtsJob("201", AtsProviderType.GREENHOUSE, "cabify", "Cabify",
                                "Software Developer", "Madrid, Spain", "hybrid",
                                Instant.parse("2026-11-26T11:00:00Z"), "Java, React",
                                "https://example.com/201", "Eng", null, true)
                );
            }
        };

        AtsFeedService service = new AtsFeedService(
                registry, List.of(gh), new SmartRecruitersAdapter(null, 0), mutableClock, Duration.ofMinutes(10)
        );

        // Startup warm-up populates the cache before any browser request arrives.
        service.warmUpOnStartup().join();
        assertThat(calls.get()).isEqualTo(1);

        AtsFeedResponse warmHit = service.getFeed(false);
        assertThat(warmHit.cached()).isTrue();
        assertThat(warmHit.refreshing()).isFalse();
        assertThat(warmHit.jobs()).hasSize(1);
        assertThat(calls.get()).isEqualTo(1);

        // Expire TTL while network is offline: background refresh must NOT wipe out the cached job.
        offline.set(true);
        nowRef.set(Instant.parse("2026-11-26T12:25:00Z"));
        service.triggerBackgroundRefresh().join();

        assertThat(service.isRefreshing()).isFalse();
        AtsFeedResponse preservedAfterOfflineRefresh = service.getFeed(false);
        assertThat(preservedAfterOfflineRefresh.jobs()).extracting(AtsJob::sourceId).containsExactly("201");
    }
}
