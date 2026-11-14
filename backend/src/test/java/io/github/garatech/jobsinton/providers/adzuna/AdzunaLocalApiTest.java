package io.github.garatech.jobsinton.providers.adzuna;

import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.http.MediaType;
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

    static final String EXCERPT = "¡España! Certificación en A Coruña…";
    static final String PAGE = """
            {"results":[{"id":"one","title":"Java developer","redirect_url":"https://adzuna.es/details/1",
            "description":"%s"}]}
            """.formatted(EXCERPT);

    @TestConfiguration(proxyBeanMethods = false)
    static class Offline { @Bean @Primary Stub fakeAdzuna() { return new Stub(); } }
    static class Stub implements AdzunaTransport {
        int status = 200;
        final AtomicInteger calls = new AtomicInteger();
        public Reply get(URI uri) {
            calls.incrementAndGet();
            return AdzunaSearchTest.reply(status, status == 200
                    ? PAGE : "internal upstream detail fixture-key");
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
    @Test void declaresUtf8AndPreservesTheCompleteUnicodeExcerptOnTheWire() throws Exception {
        stub.status = 200;
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/providers/adzuna/search"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(QUERY)).build();
        var response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        var type = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
        assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(type.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        var decoded = type.getCharset().newDecoder().decode(ByteBuffer.wrap(response.body())).toString();
        assertThat(json.readTree(decoded).path("listings").get(0).path("excerpt").asString()).isEqualTo(EXCERPT);
    }

    @Test @EnabledOnOs(OS.WINDOWS)
    void windowsPowerShellReadsTheSameExcerptWithoutReencoding() throws Exception {
        stub.status = 200;
        // ASCII command and output: console encoding cannot mask an incorrect string in memory.
        String script = """
                $ErrorActionPreference = 'Stop'
                $query = @{ what = 'Java'; where = 'Madrid' } | ConvertTo-Json -Compress
                $r = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:%d/api/providers/adzuna/search' -ContentType 'application/json' -Body $query
                [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($r.listings[0].excerpt))
                """.formatted(port);
        var ps = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectErrorStream(true).start();
        try {
            assertThat(ps.waitFor(30, TimeUnit.SECONDS)).isTrue();
            String output = new String(ps.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).strip();
            assertThat(ps.exitValue()).as(output).isZero();
            assertThat(output).isEqualTo(java.util.Base64.getEncoder().encodeToString(EXCERPT.getBytes(StandardCharsets.UTF_8)));
        } finally { ps.destroyForcibly(); }
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
    @Test void localBrowserCanDiscoverTheSourceWithoutCallingAdzunaAndSubmitOnce() throws Exception {
        stub.status = 200;
        String origin = "http://127.0.0.1:" + port;
        var availability = HttpRequest.newBuilder(URI.create(origin + "/api/providers/adzuna/availability")).build();
        int before = stub.calls.get();
        assertThat(client.send(availability, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(stub.calls.get()).isEqualTo(before);
        var local = HttpRequest.newBuilder(URI.create(origin + "/api/providers/adzuna/search"))
                .header("Origin", origin).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(QUERY)).build();
        assertThat(client.send(local, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(stub.calls.get()).isEqualTo(before + 1);
        var otherPort = HttpRequest.newBuilder(local, (name, value) -> !name.equalsIgnoreCase("Origin"))
                .header("Origin", "http://127.0.0.1:5173").build();
        assertThat(client.send(otherPort, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        assertThat(stub.calls.get()).isEqualTo(before + 1);
    }

    @Test void foreignBrowserOrInvalidInputDoesNotConsumeQuota() throws Exception {
        int before = stub.calls.get();
        assertThat(post(QUERY,true).statusCode()).isEqualTo(403);
        assertThat(post("{\"what\":\"Java\",\"where\":\"Madrid\",\"page\":99}",false).statusCode()).isEqualTo(400);
        assertThat(stub.calls.get()).isEqualTo(before);
    }
}
