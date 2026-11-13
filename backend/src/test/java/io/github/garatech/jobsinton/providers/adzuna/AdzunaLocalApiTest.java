package io.github.garatech.jobsinton.providers.adzuna;

import java.net.URI;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("adzuna-local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ADZUNA_APP_ID=fixture-id","ADZUNA_APP_KEY=fixture-key"})
@Import(AdzunaLocalApiTest.Offline.class)
class AdzunaLocalApiTest {
    @LocalServerPort int port;
    @Autowired Stub stub;
    final JsonMapper json = JsonMapper.builder().build();
    final HttpClient client = HttpClient.newHttpClient();
    static final String QUERY = "{\"what\":\"Java\",\"where\":\"Madrid\"}";

    @TestConfiguration(proxyBeanMethods = false)
    static class Offline { @Bean @Primary Stub fakeAdzuna() { return new Stub(); } }
    static class Stub implements AdzunaTransport {
        int status = 200;
        final AtomicInteger calls = new AtomicInteger();
        public Reply get(URI uri) {
            calls.incrementAndGet();
            return AdzunaSearchTest.reply(status, status == 200
                    ? "{\"results\":[" + AdzunaSearchTest.ROW + "]}" : "internal upstream detail fixture-key");
        }
    }

    HttpResponse<String> post(String body, boolean browser) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/providers/adzuna/search"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (browser) req.header("Origin","http://example.invalid");
        return client.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void returnsNormalizedRowsWithSourceAndNoStore() throws Exception {
        stub.status = 200;
        var res = post(QUERY, false);
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Cache-Control")).contains("no-store");
        var body = json.readTree(res.body());
        assertThat(body.path("source").asString()).isEqualTo("Adzuna");
        assertThat(body.path("page").asInt()).isOne();
        assertThat(body.path("listings").get(0).path("id").asString()).isEqualTo("adzuna:one");
        assertThat(res.body()).doesNotContain("fixture-key","app_id");
    }
    @Test void rateLimitAuthenticationAndUnavailableAreNotEmptyPages() throws Exception {
        for (int status : new int[]{401,403,429,503}) {
            stub.status = status;
            int before = stub.calls.get();
            var res = post(QUERY,false);
            assertThat(res.statusCode()).isEqualTo(status == 429 ? 429 : 502);
            String expected = status == 429 ? "PROVIDER_RATE_LIMITED" : status == 503 ? "PROVIDER_UNAVAILABLE" : "PROVIDER_ACCESS_DENIED";
            assertThat(json.readTree(res.body()).path("code").asString()).isEqualTo(expected);
            assertThat(res.body()).doesNotContain("fixture-key","internal upstream","listings");
            assertThat(stub.calls.get()).isEqualTo(before + 1);
        }
    }
    @Test void browserOrInvalidInputDoesNotConsumeQuota() throws Exception {
        int before = stub.calls.get();
        assertThat(post(QUERY,true).statusCode()).isEqualTo(403);
        assertThat(post("{\"what\":\"Java\",\"where\":\"Madrid\",\"page\":99}",false).statusCode()).isEqualTo(400);
        assertThat(stub.calls.get()).isEqualTo(before);
    }
}
