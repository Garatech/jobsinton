package io.github.garatech.jobsinton.matching.api;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingApiIntegrationTest {
    private static final String PROFILE = """
            {"role":"backend","technologies":["Java","Spring Boot"],"experienceYears":3,
             "country":"Spain","city":"Madrid","workMode":"remote","maxOnsiteDays":null}
            """;
    private final JsonMapper json = JsonMapper.builder().build();
    @LocalServerPort private int port;

    @Test
    void returnsOnlyTheRequestedIdsInDomainOrderWithUnavailableItemsAtTheEnd() throws Exception {
        var result = post(body("[\"z-missing\",\"frontend-react\",\"a-missing\",\"backend-java\"]", PROFILE));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.headers().firstValue("cache-control")).contains("no-store");
        assertThat(json.readTree(result.body())).isEqualTo(json.readTree("""
                {"apiVersion":"ranking-api-v1","matchingVersion":"matching-v1.0","rankingVersion":"ranking-v1.0",
                 "ranked":["backend-java","frontend-react"],"unavailable":["a-missing","z-missing"]}
                """));
        var permuted = post(body("[\"backend-java\",\"a-missing\",\"frontend-react\",\"z-missing\"]", PROFILE));
        assertThat(json.readTree(permuted.body())).isEqualTo(json.readTree(result.body()));
    }

    @Test
    void usesTheSuppliedSnapshotAndItsExplicitOfficeLimit() throws Exception {
        String data = PROFILE.replace("\"backend\"", "\"data\"").replace("\"Java\",\"Spring Boot\"", "\"Python\",\"SQL\"")
                .replace("\"Madrid\"", "\"Valencia\"").replace("\"remote\"", "\"hybrid\"");
        String ids = "[\"data-engineer\",\"backend-java\"]";
        var within = post(body(ids, data.replace("null", "2")));
        var exceeds = post(body(ids, data.replace("null", "1")));
        assertThat(json.readTree(within.body()).get("ranked")).isEqualTo(json.readTree(ids));
        assertThat(json.readTree(exceeds.body()).get("ranked")).isEqualTo(json.readTree("[\"backend-java\",\"data-engineer\"]"));
    }

    @Test
    void emptyBatchIsValidAndDoesNotExpandToTheWholeCatalog() throws Exception {
        var r = post(body("[]", PROFILE));
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(json.readTree(r.body()).get("ranked").size()).isZero();
        assertThat(json.readTree(r.body()).get("unavailable").size()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalid")
    void rejectsInvalidBatchesWithoutPartialOrders(String label, String payload, int status, String code) throws Exception {
        var r = post(payload);
        assertThat(r.statusCode()).isEqualTo(status);
        assertThat(r.headers().firstValue("cache-control")).contains("no-store");
        var result = json.readTree(r.body());
        assertThat(result.get("code").asText()).isEqualTo(code);
        assertThat(result.has("ranked")).isFalse();
    }

    static Stream<Arguments> invalid() {
        return Stream.of(
                Arguments.of("malformed", "{", 400, "INVALID_JSON"),
                Arguments.of("duplicate JSON field", body("[]", PROFILE).replace("\"profile\":", "\"profile\":null,\"profile\":"), 400, "INVALID_JSON"),
                Arguments.of("quantity limit", body("[\"" + String.join("\",\"", java.util.stream.IntStream.range(0, 101).mapToObj(i -> "id" + i).toList()) + "\"]", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("wrong shape", body("\"backend-java\"", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("number", body("[1]", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("duplicate ID", body("[\"missing\",\"missing\"]", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("blank ID", body("[\"   \"]", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("long ID", body("[\"" + "x".repeat(81) + "\"]", PROFILE), 422, "INVALID_REQUEST"),
                Arguments.of("missing profile", "{\"opportunityIds\":[]}", 422, "INVALID_REQUEST"),
                Arguments.of("null profile", body("[]", "null"), 422, "INVALID_PROFILE"),
                Arguments.of("invalid saved profile", body("[]", PROFILE.replace("\"experienceYears\":3", "\"experienceYears\":\"3\"")), 422, "INVALID_PROFILE"),
                Arguments.of("client weights", body("[]", PROFILE).replace("\"profile\":", "\"weights\":{},\"profile\":"), 422, "INVALID_REQUEST")
        );
    }

    @Test
    void rejectsAnOversizedStreamEvenWithoutDeclaredLength() throws Exception {
        byte[] bytes = (body("[]", PROFILE) + " ".repeat(16384)).getBytes(StandardCharsets.UTF_8);
        var response = send(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes)));
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("REQUEST_TOO_LARGE");
    }

    private static String body(String ids, String profile) {
        return "{\"opportunityIds\":" + ids + ",\"profile\":" + profile + "}";
    }
    private HttpResponse<String> post(String body) throws Exception {
        return send(HttpRequest.BodyPublishers.ofString(body));
    }
    private HttpResponse<String> send(HttpRequest.BodyPublisher body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/matching/rank"))
                    .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json").POST(body).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
