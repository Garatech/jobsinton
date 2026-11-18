package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jobsinton.ats.cache-ttl-seconds=300",
                "jobsinton.ats.smartrecruiters-detail-cap=0"
        }
)
@Import(AtsControllerIntegrationTest.OfflineAtsConfig.class)
class AtsControllerIntegrationTest {

    @LocalServerPort
    int port;

    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @TestConfiguration(proxyBeanMethods = false)
    static class OfflineAtsConfig {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        @Primary
        CompanyRegistry testRegistry() {
            return new CompanyRegistry(List.of(
                    new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid"),
                    new CompanyBoard("Celonis", "Celonis", "smartrecruiters", null, "Madrid")
            ));
        }

        @Bean
        @Primary
        AtsHttpClient testHttpClient() {
            return new AtsHttpClient() {
                @Override
                public HttpReply get(URI uri) {
                    String url = uri.toString();
                    if (url.contains("boards-api.greenhouse.io/v1/boards/cabify")) {
                        return reply(200, """
                                {"jobs":[{"id":101,"title":"Java Spring Boot Engineer","updated_at":"2026-11-17T08:00:00Z",
                                "location":{"name":"Madrid, Spain"},"absolute_url":"https://boards.greenhouse.io/cabify/jobs/101",
                                "content":"<p>Java, Spring Boot, PostgreSQL y Docker en España.</p>"}]}
                                """);
                    }
                    if (url.endsWith("/Celonis/postings?limit=100")) {
                        return reply(200, """
                                {"content":[{"id":"sr-42","name":"SAP RAP Consultant","releasedDate":"2026-11-16T12:00:00Z",
                                "location":{"city":"Madrid","country":"es","remote":true}}]}
                                """);
                    }
                    if (url.endsWith("/Celonis/postings/sr-42")) {
                        return reply(200, """
                                {"applyUrl":"https://jobs.smartrecruiters.com/Celonis/sr-42/apply",
                                "jobAd":{"sections":{"jobDescription":{"text":"<p>SAP S/4HANA, ABAP OO, RAP and OData v4.</p>"}}}}
                                """);
                    }
                    return reply(404, "");
                }

                @Override
                public HttpReply postJson(URI uri, String jsonBody) {
                    return reply(404, "");
                }

                private HttpReply reply(int status, String body) {
                    return new HttpReply(status, body.getBytes(StandardCharsets.UTF_8));
                }
            };
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void servesBoardsAggregatedJobsCacheAndSmartRecruitersDetailHydration() throws Exception {
        var boardsRes = get("/api/ats/boards");
        assertThat(boardsRes.statusCode()).isEqualTo(200);
        assertThat(json.readTree(boardsRes.body())).hasSize(2);

        var feedRes = get("/api/ats/jobs");
        assertThat(feedRes.statusCode()).isEqualTo(200);
        assertThat(feedRes.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(feedRes.headers().firstValue("Content-Type").orElse("")).containsIgnoringCase("utf-8");

        JsonNode feed = json.readTree(feedRes.body());
        assertThat(feed.path("cached").asBoolean()).isFalse();
        assertThat(feed.path("totalBoards").asInt()).isEqualTo(2);
        assertThat(feed.path("succeededBoards").asInt()).isEqualTo(2);
        assertThat(feed.path("jobs")).hasSize(2);
        assertThat(feed.path("jobs").get(0).path("title").asString()).isEqualTo("Java Spring Boot Engineer");
        assertThat(feed.path("jobs").get(0).path("hoursSincePublished").asLong()).isEqualTo(4L);
        assertThat(feed.path("jobs").get(0).path("description").asString()).contains("España");
        assertThat(feed.path("jobs").get(1).path("hasFullDescription").asBoolean()).isFalse();

        // Hydrate SmartRecruiters detail on demand.
        var detailRes = get("/api/ats/smartrecruiters/Celonis/sr-42");
        assertThat(detailRes.statusCode()).isEqualTo(200);
        JsonNode detail = json.readTree(detailRes.body());
        assertThat(detail.path("hasFullDescription").asBoolean()).isTrue();
        assertThat(detail.path("description").asString()).contains("SAP S/4HANA, ABAP OO, RAP and OData v4.");

        // Subsequent cached feed reflects hydrated description without upstream re-fetch.
        var cachedFeedRes = get("/api/ats/jobs");
        JsonNode cachedFeed = json.readTree(cachedFeedRes.body());
        assertThat(cachedFeed.path("cached").asBoolean()).isTrue();
        assertThat(cachedFeed.path("jobs").get(1).path("hasFullDescription").asBoolean()).isTrue();

        // Reject unregistered SmartRecruiters slug and malformed posting IDs.
        assertThat(get("/api/ats/smartrecruiters/unregistered-company/sr-42").statusCode()).isEqualTo(404);
        assertThat(get("/api/ats/smartrecruiters/Celonis/bad$id").statusCode()).isEqualTo(400);
    }
}
