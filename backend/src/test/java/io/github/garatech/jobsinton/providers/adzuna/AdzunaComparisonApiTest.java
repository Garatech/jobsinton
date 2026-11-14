package io.github.garatech.jobsinton.providers.adzuna;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("adzuna-local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ADZUNA_APP_ID=fixture-id","ADZUNA_APP_KEY=fixture-key"})
@Import(AdzunaLocalApiTest.Offline.class)
class AdzunaComparisonApiTest {
    private static final Map<String, Object> PROFILE = Map.of("role", "backend", "technologies", java.util.List.of("Java"),
            "experienceYears", 2, "country", "Spain", "city", "Madrid", "workMode", "hybrid", "maxOnsiteDays", 1);
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();
    @LocalServerPort int port;
    @Autowired AdzunaLocalApiTest.Stub provider;

    @Test void oneSearchSupportsConflictUnknownAndReevaluationAgainstANewSnapshot() throws Exception {
        int before = provider.calls.get();
        var refs = search();
        var explicit = compare(refs.get("adzuna:two").asString(), PROFILE);
        assertThat(explicit.path("apiVersion").asString()).isEqualTo("matching-api-v1");
        assertThat(explicit.path("opportunityId").asString()).isEqualTo("adzuna:two");
        assertThat(explicit.path("evaluation").path("constraintSummary").asString()).isEqualTo("CONFLICT");
        var attendance = assessment(explicit, "attendance");
        assertThat(attendance.path("reason").asString()).isEqualTo("ATTENDANCE_EXCEEDS_LIMIT");
        assertThat(attendance.path("evidence").path("profileValues").get(0).asString()).isEqualTo("1");
        assertThat(attendance.path("evidence").path("opportunityValues").get(0).asString()).isEqualTo("2");
        var updated = new java.util.HashMap<>(PROFILE);
        updated.put("maxOnsiteDays", 2);
        assertThat(assessment(compare(refs.get("adzuna:two").asString(), updated), "attendance")
                .path("reason").asString()).isEqualTo("ATTENDANCE_WITHIN_LIMIT");
        var sparse = compare(refs.get("adzuna:one").asString(), PROFILE);
        assertThat(sparse.path("opportunityId").asString()).isEqualTo("adzuna:one");
        assertThat(sparse.path("evaluation").path("constraintSummary").asString()).isEqualTo("NEEDS_INFORMATION");
        assertThat(assessment(sparse, "attendance").path("outcome").asString()).isEqualTo("UNKNOWN");
        assertThat(provider.calls.get()).isEqualTo(before + 1);
    }

    @Test void rejectsBadEnvelopesProfilesReferencesAndForeignOriginsWithoutFetching() throws Exception {
        String ref = search().get("adzuna:two").asString();
        int before = provider.calls.get();
        String valid = json.writeValueAsString(Map.of("comparisonRef", ref, "profile", PROFILE));
        error(post("compare", valid, "http://example.invalid"), 403, "LOCAL_ONLY");
        error(post("compare", valid + "{}", null), 400, "INVALID_JSON");
        error(post("compare", " ".repeat(16385), null), 413, "REQUEST_TOO_LARGE");
        error(post("compare", json.writeValueAsString(Map.of("comparisonRef", ref, "profile", PROFILE,
                "matchingFacts", Map.of("officeDays", 0))), null), 422, "INVALID_REQUEST");
        error(post("compare", json.writeValueAsString(Map.of("comparisonRef", ref, "profile", Map.of())), null),
                422, "INVALID_PROFILE");
        error(post("compare", json.writeValueAsString(Map.of("comparisonRef", "adzuna:two", "profile", PROFILE)), null),
                422, "INVALID_COMPARISON_REF");
        assertThat(provider.calls.get()).isEqualTo(before);
    }

    private JsonNode search() throws Exception {
        provider.status = 200;
        var response = post("search", AdzunaLocalApiTest.QUERY, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body()).get("comparisonRefs");
    }

    private JsonNode compare(String ref, Map<String, Object> profile) throws Exception {
        String body = json.writeValueAsString(Map.of("profile", profile, "comparisonRef", ref));
        var response = post("compare", body, "http://127.0.0.1:" + port);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readTree(response.body());
    }

    private static JsonNode assessment(JsonNode response, String criterion) {
        for (var item : response.path("evaluation").path("assessments")) {
            if (criterion.equals(item.path("criterion").asString())) return item;
        }
        throw new AssertionError("Missing criterion: " + criterion);
    }

    private void error(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        var body = json.readTree(response.body());
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.has("evaluation")).isFalse();
    }

    private HttpResponse<String> post(String path, String body, String origin) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/providers/adzuna/" + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (origin != null) request.header("Origin", origin);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
