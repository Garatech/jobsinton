package io.github.garatech.jobsinton.providers.adzuna;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static io.github.garatech.jobsinton.providers.adzuna.AdzunaSearch.*;

class AdzunaTransportTest {
    HttpServer server;
    URI uri;
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/search");
    }
    @AfterEach void stop() { server.stop(0); }

    @Test void actualGetReadsAnInMemoryPage() {
        server.createContext("/search", e -> {
            try (e) {
                boolean valid = e.getRequestMethod().equals("GET") && "application/json".equals(e.getRequestHeaders().getFirst("Accept"));
                e.sendResponseHeaders(valid ? 200 : 400, 0);
                e.getResponseBody().write("{\"results\":[]}".getBytes(StandardCharsets.UTF_8));
            }
        });
        var response = new JdkAdzunaTransport(Duration.ofSeconds(3)).get(uri);
        assertThat(response.status()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("{\"results\":[]}");
    }

    @Test void doesNotFollowRedirectOrExposeItsBody() {
        var calls = new AtomicInteger();
        server.createContext("/search", e -> { try (e) {
            calls.incrementAndGet();
            e.getResponseHeaders().set("Location", "/other");
            e.sendResponseHeaders(302, 0);
            e.getResponseBody().write("private provider details".getBytes(StandardCharsets.UTF_8));
        }});
        server.createContext("/other", e -> { try (e) { calls.incrementAndGet(); e.sendResponseHeaders(200,-1); }});
        var response = new JdkAdzunaTransport(Duration.ofSeconds(3)).get(uri);
        assertThat(response.status()).isEqualTo(302);
        assertThat(response.body()).isEmpty();
        assertThat(calls.get()).isOne();
    }

    @Test void rejectsChunkedResponsesBeyondTheByteLimit() {
        server.createContext("/search", e -> { try (e) {
            e.sendResponseHeaders(200, 0);
            e.getResponseBody().write(new byte[JdkAdzunaTransport.MAX_BYTES + 1]);
        }});
        AdzunaSearchTest.failure(() -> new JdkAdzunaTransport(Duration.ofSeconds(3)).get(uri), Problem.INVALID_PROVIDER_RESPONSE);
    }

    @Test void deadlineCoversAStalledBodyAfterHeaders() {
        var release = new CountDownLatch(1);
        server.createContext("/search", e -> { try (e) {
            e.sendResponseHeaders(200, 0);
            e.getResponseBody().write(123);
            e.getResponseBody().flush();
            try { release.await(4, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }});
        try {
            AdzunaSearchTest.failure(() -> new JdkAdzunaTransport(Duration.ofMillis(350)).get(uri), Problem.PROVIDER_TIMEOUT);
        } finally { release.countDown(); }
    }
}
