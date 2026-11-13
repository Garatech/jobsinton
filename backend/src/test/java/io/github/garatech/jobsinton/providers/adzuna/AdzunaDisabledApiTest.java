package io.github.garatech.jobsinton.providers.adzuna;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdzunaDisabledApiTest {
    @LocalServerPort int port;
    @Test void regularBackendHasStatusButNoProviderRoute() throws Exception {
        var client = HttpClient.newHttpClient();
        String base = "http://127.0.0.1:" + port;
        var status = HttpRequest.newBuilder(URI.create(base + "/api/status")).GET().build();
        assertThat(client.send(status, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        var post = HttpRequest.newBuilder(URI.create(base + "/api/providers/adzuna/search"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertThat(client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
    }
}
