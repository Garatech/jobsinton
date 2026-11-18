package io.github.garatech.jobsinton.providers.ats;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkAtsHttpClientTest {

    @Test
    void executesGetPostAndFollowsRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/target");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/target", ex -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.createContext("/post", ex -> {
            byte[] received = ex.getRequestBody().readAllBytes();
            ex.sendResponseHeaders(200, received.length);
            ex.getResponseBody().write(received);
            ex.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            var client = new JdkAtsHttpClient(Duration.ofSeconds(3));

            var getReply = client.get(URI.create("http://127.0.0.1:" + port + "/redirect"));
            assertThat(getReply.status()).isEqualTo(200);
            assertThat(new String(getReply.body(), StandardCharsets.UTF_8)).isEqualTo("{\"ok\":true}");

            var postReply = client.postJson(URI.create("http://127.0.0.1:" + port + "/post"), "{\"query\":\"\"}");
            assertThat(postReply.status()).isEqualTo(200);
            assertThat(new String(postReply.body(), StandardCharsets.UTF_8)).isEqualTo("{\"query\":\"\"}");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void enforcesStreamingSizeLimitAndPerRequestTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/oversized", ex -> {
            byte[] big = new byte[4096];
            ex.sendResponseHeaders(200, big.length);
            ex.getResponseBody().write(big);
            ex.close();
        });
        server.createContext("/slow", ex -> {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            var smallLimitClient = new JdkAtsHttpClient(Duration.ofSeconds(3), 1024);
            assertThatThrownBy(() -> smallLimitClient.get(URI.create("http://127.0.0.1:" + port + "/oversized")))
                    .isInstanceOf(AtsFetchException.class)
                    .hasMessageContaining("size limit");

            var fastTimeoutClient = new JdkAtsHttpClient(Duration.ofMillis(250));
            assertThatThrownBy(() -> fastTimeoutClient.get(URI.create("http://127.0.0.1:" + port + "/slow")))
                    .isInstanceOf(AtsFetchException.class)
                    .hasMessageContaining("Timed out");
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
