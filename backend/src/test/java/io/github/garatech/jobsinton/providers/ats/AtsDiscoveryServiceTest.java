package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtsDiscoveryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

    private static AtsHttpClient stubHttp(Map<String, String> fixtures) {
        return new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                String body = fixtures.get(uri.toString());
                if (body == null) {
                    return new HttpReply(404, new byte[0]);
                }
                return new HttpReply(200, body.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                return new HttpReply(404, new byte[0]);
            }
        };
    }

    @Test
    void sniffsCorporateCareerPageHtmlAndVerifiesEmbeddedPersonioBoard(@TempDir Path tempDir) {
        String careerHtml = """
                <html>
                  <head><title>Careers at Acme Iberia</title></head>
                  <body>
                    <iframe src="https://acme-iberia.jobs.personio.com/?language=es"></iframe>
                  </body>
                </html>
                """;
        String personioXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workzag-jobs>
                  <position>
                    <id>7001</id>
                    <name>Full Stack Java &amp; React Developer</name>
                    <office>Madrid</office>
                    <department>Engineering</department>
                    <createdAt>2026-11-17T10:00:00+01:00</createdAt>
                    <jobDescriptions>
                      <jobDescription>
                        <name>Role</name>
                        <value><![CDATA[<p>Spring Boot, React, PostgreSQL and Docker in Madrid.</p>]]></value>
                      </jobDescription>
                    </jobDescriptions>
                  </position>
                </workzag-jobs>
                """;

        AtsHttpClient http = stubHttp(Map.of(
                "https://careers.acme-iberia.es/jobs", careerHtml,
                "https://acme-iberia.jobs.personio.com/xml?language=es", personioXml
        ));

        Path customPath = tempDir.resolve("custom-companies.json");
        CompanyRegistry registry = new CompanyRegistry(List.of(), customPath);
        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(http, 0);
        AtsFeedService feedService = new AtsFeedService(
                registry,
                List.of(
                        new GreenhouseAdapter(http),
                        new LeverAdapter(http),
                        sr,
                        new WorkableAdapter(http),
                        new AshbyAdapter(http),
                        new PersonioAdapter(http),
                        new RecruiteeAdapter(http),
                        new TeamtailorAdapter(http),
                        new FactorialAdapter(http, 0)
                ),
                sr,
                FIXED_CLOCK,
                Duration.ofMinutes(10)
        );
        // Prime the feed cache first so we can verify discovered jobs merge into the active cache.
        assertThat(feedService.getFeed(false).jobs()).isEmpty();

        AtsDiscoveryService discovery = new AtsDiscoveryService(http, feedService);
        AtsDiscoveryService.DiscoveryResponse response = discovery.discoverFromUrl(
                "https://careers.acme-iberia.es/jobs",
                "Acme Iberia"
        );

        assertThat(response.totalAdded()).isEqualTo(1);
        assertThat(response.totalJobsFound()).isEqualTo(1);
        assertThat(response.discovered()).hasSize(1);
        AtsDiscoveryService.DiscoveredBoardResult board = response.discovered().getFirst();
        assertThat(board.slug()).isEqualTo("acme-iberia");
        assertThat(board.name()).isEqualTo("Acme Iberia");
        assertThat(board.provider()).isEqualTo("personio");
        assertThat(board.newlyRegistered()).isTrue();

        // Active feed cache immediately contains the newly discovered job without a full refresh.
        AtsFeedResponse cachedFeed = feedService.getFeed(false);
        assertThat(cachedFeed.jobs()).hasSize(1);
        assertThat(cachedFeed.jobs().getFirst().title()).isEqualTo("Full Stack Java & React Developer");

        // Custom registry persisted to disk and reloads on restart.
        CompanyRegistry reloaded = new CompanyRegistry(List.of(), customPath);
        assertThat(reloaded.find(AtsProviderType.PERSONIO, "acme-iberia")).isPresent();
    }

    @Test
    void batchProbesSlugsAcrossAllNineAtsPlatformsAndRegistersHits() {
        String recruiteeJson = """
                {
                  "offers": [
                    {
                      "id": 501,
                      "title": "SAP ABAP / BTP Consultant",
                      "published_at": "2026-11-17T08:00:00Z",
                      "city": "Madrid",
                      "country_code": "ES",
                      "hybrid": true,
                      "description": "<p>SAP S/4HANA, ABAP OO, RAP and OData v4.</p>",
                      "careers_apply_url": "https://ibertech.recruitee.com/o/sap-abap/c/new"
                    }
                  ]
                }
                """;

        AtsHttpClient http = stubHttp(Map.of(
                "https://ibertech.recruitee.com/api/offers/", recruiteeJson
        ));

        CompanyRegistry registry = new CompanyRegistry(List.of());
        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(http, 0);
        AtsFeedService feedService = new AtsFeedService(
                registry,
                List.of(
                        new GreenhouseAdapter(http),
                        new LeverAdapter(http),
                        sr,
                        new WorkableAdapter(http),
                        new AshbyAdapter(http),
                        new PersonioAdapter(http),
                        new RecruiteeAdapter(http),
                        new TeamtailorAdapter(http),
                        new FactorialAdapter(http, 0)
                ),
                sr,
                FIXED_CLOCK,
                Duration.ofMinutes(10)
        );

        AtsDiscoveryService discovery = new AtsDiscoveryService(http, feedService);
        AtsDiscoveryService.DiscoveryResponse response = discovery.probeCandidates(List.of("IberTech S.L."));

        assertThat(response.totalAdded()).isEqualTo(1);
        assertThat(response.totalJobsFound()).isEqualTo(1);
        assertThat(response.discovered()).singleElement().satisfies(b -> {
            assertThat(b.slug()).isEqualTo("ibertech");
            assertThat(b.provider()).isEqualTo("recruitee");
            assertThat(b.jobCount()).isEqualTo(1);
        });

        assertThatThrownBy(() -> discovery.discoverFromUrl("javascript:alert(1)", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
