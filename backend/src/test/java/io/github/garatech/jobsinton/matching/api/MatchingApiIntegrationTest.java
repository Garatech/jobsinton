package io.github.garatech.jobsinton.matching.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchingApiIntegrationTest {
    private static final String PROFILE = """
            {"role":"backend","technologies":["Java","Spring Boot"],"experienceYears":3,
             "country":"Spain","city":"Madrid","workMode":"remote","maxOnsiteDays":null}
            """;
    private final JsonMapper json = JsonMapper.builder().build();
    @LocalServerPort private int port;

    @Test
    void evaluatesTheSavedSnapshotAndReturnsVersionedEvidenceWithoutCaching() throws Exception {
        var response = post(body("backend-java", PROFILE));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        var root = json.readTree(response.body());
        assertThat(root.get("apiVersion").asText()).isEqualTo("matching-api-v1");
        assertThat(root.get("opportunityId").asText()).isEqualTo("backend-java");
        assertThat(root.get("evaluation").get("rulesVersion").asText()).isEqualTo("matching-v1.0");
        assertThat(root.get("evaluation").get("constraintSummary").asText()).isEqualTo("NEEDS_INFORMATION");
        assertThat(assessment(root, "technology:java").get("outcome").asText()).isEqualTo("ALIGNED");
        assertThat(assessment(root, "experience").get("reason").asText()).isEqualTo("EXPERIENCE_SCOPE_UNCONFIRMED");
        assertThat(assessment(root, "residence").get("reason").asText()).isEqualTo("RESIDENCE_POLICY_UNKNOWN");
        assertThat(assessment(root, "attendance").get("outcome").asText()).isEqualTo("NOT_ASSESSED");
        assertThat(root.has("profile")).isFalse();
        assertThat(response.body()).doesNotContain("\"score\"", "\"probability\"");
    }

    @Test
    void preservesAttendanceConflictAndResidenceUnknownWithNormalizedPlaces() throws Exception {
        String profile = PROFILE.replace("\"remote\"", "\"hybrid\"").replace("\"maxOnsiteDays\":null", "\"maxOnsiteDays\":1")
                .replace("\"Spain\"", "\" España \"").replace("\"Madrid\"", "\" València \"");
        var response = post(body("data-engineer", profile));
        assertThat(response.statusCode()).isEqualTo(200);
        var root = json.readTree(response.body());
        assertThat(root.get("evaluation").get("constraintSummary").asText()).isEqualTo("CONFLICT");
        assertThat(assessment(root, "attendance").get("reason").asText()).isEqualTo("ATTENDANCE_EXCEEDS_LIMIT");
        assertThat(assessment(root, "residence").get("outcome").asText()).isEqualTo("UNKNOWN");
        assertThat(assessment(root, "office-location").get("outcome").asText()).isEqualTo("ALIGNED");
    }

    @Test
    void leavesUnrecognizedPlaceNamesUnknownAndNeverGuessesResidencePolicies() throws Exception {
        String profile = PROFILE.replace("\"remote\"", "\"hybrid\"").replace("\"maxOnsiteDays\":null", "\"maxOnsiteDays\":2")
                .replace("\"Spain\"", "\"Unmapped country\"").replace("\"Madrid\"", "\"Unmapped city\"");
        var response = post(body("data-engineer", profile));
        assertThat(response.statusCode()).isEqualTo(200);
        var root = json.readTree(response.body());
        assertThat(assessment(root, "office-location").get("outcome").asText()).isEqualTo("UNKNOWN");
        assertThat(assessment(root, "residence").get("reason").asText()).isEqualTo("RESIDENCE_POLICY_UNKNOWN");
    }

    @Test
    void separateRequestsNeverReuseAnotherProfileOrOffer() throws Exception {
        var first = post(body("backend-java", PROFILE));
        var second = post(body("sap-developer", PROFILE.replace("[\"Java\",\"Spring Boot\"]", "[\"ABAP\"]")));
        assertThat(assessment(json.readTree(first.body()), "technology:java").get("outcome").asText()).isEqualTo("ALIGNED");
        assertThat(assessment(json.readTree(second.body()), "technology:abap").get("outcome").asText()).isEqualTo("ALIGNED");
        assertThat(second.body()).doesNotContain("technology:java");
        assertThat(post(body("backend-java", PROFILE)).body()).isEqualTo(first.body());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsWithoutReturningAnEvaluation(String label, String body, int status, String code) throws Exception {
        var response = post(body);
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        var error = json.readTree(response.body());
        assertThat(error.get("code").asText()).isEqualTo(code);
        assertThat(error.has("evaluation")).isFalse();
        assertThat(error.has("stackTrace")).isFalse();
        assertThat(error.get("errors").isArray()).isTrue();
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("malformed JSON", "{", 400, "INVALID_JSON"),
                Arguments.of("empty input", "", 400, "INVALID_JSON"),
                Arguments.of("trailing JSON", body("backend-java", PROFILE) + "{}", 400, "INVALID_JSON"),
                Arguments.of("duplicate keys", body("backend-java", PROFILE).replace("\"role\":\"backend\"", "\"role\":\"backend\",\"role\":\"sap\""), 400, "INVALID_JSON"),
                Arguments.of("missing envelope", "{}", 422, "INVALID_REQUEST"),
                Arguments.of("null profile", body("backend-java", "null"), 422, "INVALID_PROFILE"),
                Arguments.of("missing field", body("backend-java", PROFILE.replace("\"country\":\"Spain\",", "")), 422, "INVALID_PROFILE"),
                Arguments.of("extra profile field", body("backend-java", PROFILE.replace("\"role\":", "\"extra\":true,\"role\":")), 422, "INVALID_PROFILE"),
                Arguments.of("numeric string", body("backend-java", PROFILE.replace("\"experienceYears\":3", "\"experienceYears\":\"3\"")), 422, "INVALID_PROFILE"),
                Arguments.of("fractional experience", body("backend-java", PROFILE.replace("\"experienceYears\":3", "\"experienceYears\":3.5")), 422, "INVALID_PROFILE"),
                Arguments.of("overflow experience", body("backend-java", PROFILE.replace("\"experienceYears\":3", "\"experienceYears\":99999999999999999999")), 422, "INVALID_PROFILE"),
                Arguments.of("unknown role", body("backend-java", PROFILE.replace("\"role\":\"backend\"", "\"role\":\"wizard\"")), 422, "INVALID_PROFILE"),
                Arguments.of("missing technologies", body("backend-java", PROFILE.replace("[\"Java\",\"Spring Boot\"]", "[]")), 422, "INVALID_PROFILE"),
                Arguments.of("null technology", body("backend-java", PROFILE.replace("[\"Java\",\"Spring Boot\"]", "[null]")), 422, "INVALID_PROFILE"),
                Arguments.of("duplicate technologies", body("backend-java", PROFILE.replace("[\"Java\",\"Spring Boot\"]", "[\"Java\",\" java \"]")), 422, "INVALID_PROFILE"),
                Arguments.of("remote max remains null", body("backend-java", PROFILE.replace("\"maxOnsiteDays\":null", "\"maxOnsiteDays\":0")), 422, "INVALID_PROFILE"),
                Arguments.of("hybrid needs a maximum", body("backend-java", PROFILE.replace("\"remote\"", "\"hybrid\"")), 422, "INVALID_PROFILE"),
                Arguments.of("unknown offer", body("missing", PROFILE), 404, "OPPORTUNITY_NOT_FOUND"),
                Arguments.of("oversized request", body("backend-java", PROFILE) + " ".repeat(16384), 413, "REQUEST_TOO_LARGE")
        );
    }

    @Test
    void usesPostAndJsonOnly() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var wrongType = HttpRequest.newBuilder(uri()).header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(body("backend-java", PROFILE))).build();
            assertThat(client.send(wrongType, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(415);
            assertThat(client.send(HttpRequest.newBuilder(uri()).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(405);
        }
    }

    private static String body(String id, String profile) {
        return "{\"opportunityId\":\"" + id + "\",\"profile\":" + profile + "}";
    }

    private static JsonNode assessment(JsonNode root, String criterion) {
        for (var node : root.get("evaluation").get("assessments")) {
            if (criterion.equals(node.get("criterion").asText())) return node;
        }
        throw new AssertionError("Missing assessment: " + criterion);
    }

    private URI uri() { return URI.create("http://127.0.0.1:" + port + "/api/matching/evaluate"); }
    private HttpResponse<String> post(String body) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(uri()).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
