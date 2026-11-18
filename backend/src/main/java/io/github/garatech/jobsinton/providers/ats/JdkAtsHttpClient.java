package io.github.garatech.jobsinton.providers.ats;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Resilient JDK HttpClient with per-request deadlines, safe HTTPS redirect following, and streaming byte limits. */
public final class JdkAtsHttpClient implements AtsHttpClient {
    static final int MAX_BYTES = 12 * 1024 * 1024; // 12 MiB handles large Greenhouse boards safely.
    private static final String USER_AGENT = "JobSinton/1.0 (+https://github.com/Garatech/jobsinton)";

    private final HttpClient client;
    private final Duration requestTimeout;
    private final int maxBytes;

    public JdkAtsHttpClient(Duration requestTimeout) {
        this(requestTimeout, MAX_BYTES);
    }

    JdkAtsHttpClient(Duration requestTimeout, int maxBytes) {
        this.requestTimeout = requestTimeout;
        this.maxBytes = maxBytes;
        Duration connectTimeout = requestTimeout.compareTo(Duration.ofSeconds(4)) < 0
                ? requestTimeout : Duration.ofSeconds(4);
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                // NORMAL follows HTTPS->HTTPS redirects (needed by Workable widget API) while blocking HTTPS->HTTP downgrade.
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public HttpReply get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        return execute(request);
    }

    @Override
    public HttpReply postJson(URI uri, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return execute(request);
    }

    private HttpReply execute(HttpRequest request) {
        var future = client.sendAsync(request, info -> new BoundedBody(info.statusCode() >= 200 && info.statusCode() < 300, maxBytes));
        try {
            HttpResponse<byte[]> response = future.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return new HttpReply(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new AtsFetchException("Request interrupted", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AtsFetchException("Timed out after " + requestTimeout.toMillis() + "ms", e);
        } catch (ExecutionException e) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof AtsFetchException ats) {
                    throw ats;
                }
                if (cause instanceof HttpTimeoutException) {
                    throw new AtsFetchException("Timed out after " + requestTimeout.toMillis() + "ms", cause);
                }
            }
            throw new AtsFetchException("Network error contacting ATS board", e.getCause());
        }
    }

    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private final boolean read;
        private Flow.Subscription subscription;
        private int remaining;
        private boolean done;

        BoundedBody(boolean read, int maxBytes) {
            this.read = read;
            this.remaining = maxBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            this.subscription = s;
            if (read) {
                delegate.onSubscribe(s);
            } else {
                done = true;
                s.cancel();
                delegate.onComplete();
            }
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (done) {
                return;
            }
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > remaining) {
                    done = true;
                    delegate.onError(new AtsFetchException("ATS response exceeded maximum size limit"));
                    subscription.cancel();
                    return;
                }
                remaining -= buffer.remaining();
            }
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable error) {
            if (!done) {
                delegate.onError(error);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                delegate.onComplete();
            }
        }
    }
}
