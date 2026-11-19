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

import static org.assertj.core.api.Assertions.assertThat;

class AtsDiscoveryServiceTest {

    private static AtsHttpClient.HttpReply ok(String body) {
        return new AtsHttpClient.HttpReply(200, body.getBytes(StandardCharsets.UTF_8));
    }

    private static AtsHttpClient.HttpReply notFound() {
        return new AtsHttpClient.HttpReply(404, new byte[0]);
    }

    @Test
    void sniffsDirectAndEmbeddedCareerPageUrlsAndPersistsCustomBoards(@TempDir Path tempDir) {
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                String url = uri.toString();
                if (url.equals("https://www.acme-madrid.es/careers")) {
                    return ok("""
                            <html>
                              <body>
                                <h1>Work with us in Madrid</h1>
                                <iframe src="https://acmemadrid.recruitee.com/"></iframe>
                              </body>
                            </html>
                            """);
                }
                if (url.equals("https://acmemadrid.recruitee.com/api/offers/")) {
                    return ok("""
                            {
                              "offers": [
                                {
                                  "id": 501,
                                  "title": "Java Spring Boot Engineer",
                                  "published_at": "2026-11-18T08:00:00Z",
                                  "city": "Madrid",
                                  "country_code": "ES",
                                  "hybrid": true,
                                  "description": "<p>Java 21 &amp; Spring Boot</p>",
                                  "careers_apply_url": "https://acmemadrid.recruitee.com/o/java-engineer"
                                }
                              ]
                            }
                            """);
                }
                return notFound();
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                return notFound();
            }
        };

        Path customFile = tempDir.resolve("custom-companies.json");
        CompanyRegistry registry = new CompanyRegistry(List.of(), customFile);
        FirstSeenRegistry firstSeen = new FirstSeenRegistry(tempDir.resolve("seen.json"));
        RecruiteeAdapter recruitee = new RecruiteeAdapter(stub);
        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(stub, 0);
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

        AtsFeedService feedService = new AtsFeedService(
                registry, List.of(recruitee), sr, clock, Duration.ofMinutes(10), firstSeen, 4
        );
        // Initialize cache first so mergeDiscoveredBoard updates the live feed immediately.
        feedService.getFeed(false);

        AtsDiscoveryService discovery = new AtsDiscoveryService(stub, feedService);
        var result = discovery.discoverFromUrl("https://www.acme-madrid.es/careers");

        assertThat(result.discoveredCount()).isEqualTo(1);
        assertThat(result.discovered().getFirst().board().slug()).isEqualTo("acmemadrid");
        assertThat(result.discovered().getFirst().board().ats()).isEqualTo("recruitee");
        assertThat(result.discovered().getFirst().jobCount()).isEqualTo(1);
        assertThat(result.discovered().getFirst().newlyAdded()).isTrue();

        // Reload registry from disk to verify persistence in custom-companies.json.
        CompanyRegistry reloaded = new CompanyRegistry(List.of(), customFile);
        assertThat(reloaded.find(AtsProviderType.RECRUITEE, "acmemadrid")).isPresent();

        // Verify cached feed now contains the newly discovered job immediately.
        AtsFeedResponse updatedFeed = feedService.getFeed(false);
        assertThat(updatedFeed.jobs()).hasSize(1);
        assertThat(updatedFeed.jobs().getFirst().title()).isEqualTo("Java Spring Boot Engineer");
    }

    @Test
    void batchProbesCompanyNamesAcrossAtsAdapters(@TempDir Path tempDir) {
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                if (uri.toString().equals("https://api.ashbyhq.com/posting-api/job-board/iberiatech?includeCompensation=true")) {
                    return ok("""
                            {
                              "jobs": [
                                {
                                  "id": "ash-99",
                                  "title": "Junior SAP & Full Stack Engineer",
                                  "publishedAt": "2026-11-18T10:00:00Z",
                                  "location": "Madrid, Spain",
                                  "isRemote": true,
                                  "isListed": true,
                                  "descriptionPlain": "SAP S/4HANA, React, Node.js",
                                  "applyUrl": "https://jobs.ashbyhq.com/iberiatech/ash-99"
                                }
                              ]
                            }
                            """);
                }
                return notFound();
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                return notFound();
            }
        };

        CompanyRegistry registry = new CompanyRegistry(List.of(), tempDir.resolve("custom.json"));
        AshbyAdapter ashby = new AshbyAdapter(stub);
        GreenhouseAdapter gh = new GreenhouseAdapter(stub);
        SmartRecruitersAdapter sr = new SmartRecruitersAdapter(stub, 0);
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

        AtsFeedService feedService = new AtsFeedService(
                registry, List.of(ashby, gh), sr, clock, Duration.ofMinutes(10), new FirstSeenRegistry(), 4
        );
        AtsDiscoveryService discovery = new AtsDiscoveryService(stub, feedService);

        var response = discovery.probeCandidates(List.of("IberiaTech, nonexistent-firm"));
        assertThat(response.discoveredCount()).isEqualTo(1);
        assertThat(response.discovered().getFirst().board().ats()).isEqualTo("ashby");
        assertThat(response.discovered().getFirst().board().slug()).isEqualTo("iberiatech");
        assertThat(response.discovered().getFirst().jobCount()).isEqualTo(1);
    }
}
