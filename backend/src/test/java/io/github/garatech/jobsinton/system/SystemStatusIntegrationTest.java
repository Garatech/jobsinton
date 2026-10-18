package io.github.garatech.jobsinton.system;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises real HTTP routing and serialization, rather than calling the controller directly. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemStatusIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void exposesTheDocumentedStatusContract() throws Exception {
        var response = get("/api/status");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse(""))
                .startsWith("application/json");
        var json = JsonMapper.builder().build();
        assertThat(json.readTree(response.body()))
                .isEqualTo(json.readTree("{\"name\":\"JobSinton\",\"status\":\"UP\"}"));
    }

    @Test
    void unknownApiRoutesAreNotReportedAsHealthy() throws Exception {
        assertThat(get("/api/unknown").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
