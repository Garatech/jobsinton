package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AtsFeedServiceTest {

    @Test
    void loadsDefaultRegistryAcrossAllNineAtsProviders() {
        CompanyRegistry registry = CompanyRegistry.loadFromClasspath(CompanyRegistry.DEFAULT_RESOURCE, null);
        assertThat(registry.boards().size()).isGreaterThanOrEqualTo(160);
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
    void isolatesBoardFailuresComputesTrueFirstSeenAgeAndHonorsCacheTtl(@TempDir Path tempDir) {
        Instant fixedNow = Instant.parse("2026-11-18T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        AtomicInteger greenhouseCalls = new AtomicInteger();

        CompanyBoard okBoard = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        CompanyBoard brokenBoard = new CompanyBoard("broken", "Broken Co", "ashby", null, "Remote");
        CompanyRegistry registry = new CompanyRegistry(List.of(okBoard, brokenBoard));

        // Pre-seed FirstSeenRegistry to verify an edited old job (updated_at = 2h ago, firstSeenAt = 5d ago)
        // reports its true 120h age instead of faking a <24h badge.
        FirstSeenRegistry firstSeen = new FirstSeenRegistry(tempDir.resolve("seen.json"));
        firstSeen.recordBatch(
                List.of(new RawAtsJob("1", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Edited Old Role", "Madrid",
                        "hybrid", Instant.parse("2026-11-13T12:00:00Z"), "Java", "https://example.com/1", "Eng", null, true)),
                Instant.parse("2026-11-13T12:00:00Z")
        );

        AtsProviderAdapter gh = new AtsProviderAdapter() {
            @Override
            public AtsProviderType type() {
                return AtsProviderType.GREENHOUSE;
            }

            @Override
            public List<RawAtsJob> fetch(CompanyBoard board) {
                greenhouseCalls.incrementAndGet();
                return List.of(
                        new RawAtsJob("1", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Edited Old Role", "Madrid",
                                "hybrid", Instant.parse("2026-11-18T10:00:00Z"), "Java", "https://example.com/1",
                                "Eng", null, true),
                        new RawAtsJob("2", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Genuine Fresh Role", "Madrid",
                                "remote", Instant.parse("2026-11-18T06:00:00Z"), "Spring Boot", "https://example.com/2",
                                "Eng", null, true),
                        // Duplicate sourceId should be deduplicated cleanly.
                        new RawAtsJob("2", AtsProviderType.GREENHOUSE, "cabify", "Cabify", "Genuine Fresh Role", "Madrid",
                                "remote", Instant.parse("2026-11-18T06:00:00Z"), "Spring Boot", "https://example.com/2",
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
        AtsFeedService service = new AtsFeedService(
                registry, List.of(gh, ashbyFailing), sr, clock, Duration.ofMinutes(10), firstSeen, 8
        );

        AtsFeedResponse first = service.getFeed(false);
        assertThat(first.cached()).isFalse();
        assertThat(first.totalBoards()).isEqualTo(2);
        assertThat(first.succeededBoards()).isEqualTo(1);
        assertThat(first.failedBoards()).isEqualTo(1);
        assertThat(first.jobs()).hasSize(2);
        // Genuine fresh job sorts first (6h old), while edited old role reflects its true 120h firstSeenAt age!
        assertThat(first.jobs().get(0).title()).isEqualTo("Genuine Fresh Role");
        assertThat(first.jobs().get(0).hoursSincePublished()).isEqualTo(6L);
        assertThat(first.jobs().get(1).title()).isEqualTo("Edited Old Role");
        assertThat(first.jobs().get(1).hoursSincePublished()).isEqualTo(120L);
        assertThat(first.jobs().get(1).firstSeenAt()).isEqualTo("2026-11-13T12:00:00Z");

        // Second call within TTL uses cache without calling adapters again.
        AtsFeedResponse second = service.getFeed(false);
        assertThat(second.cached()).isTrue();
        assertThat(greenhouseCalls.get()).isEqualTo(1);

        // Force refresh bypasses cache.
        AtsFeedResponse forced = service.getFeed(true);
        assertThat(forced.cached()).isFalse();
        assertThat(greenhouseCalls.get()).isEqualTo(2);
    }
}
