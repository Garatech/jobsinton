package io.github.garatech.jobsinton.providers.adzuna;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.garatech.jobsinton.providers.adzuna.AdzunaSearch.*;

interface AdzunaTransport {
    record Reply(int status, byte[] body) {}
    Reply get(URI uri);
}

final class JdkAdzunaTransport implements AdzunaTransport {
    static final int MAX_BYTES = 512 * 1024;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final Duration deadline;

    JdkAdzunaTransport(Duration deadline) { this.deadline = deadline; }

    @Override public Reply get(URI uri) {
        var request = HttpRequest.newBuilder(uri).timeout(deadline).header("Accept", "application/json").GET().build();
        var future = client.sendAsync(request, info -> new BoundedBody(info.statusCode() == 200));
        try {
            var response = future.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
            return new Reply(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new Failure(Problem.PROVIDER_UNAVAILABLE);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new Failure(Problem.PROVIDER_TIMEOUT);
        } catch (ExecutionException e) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof Failure failure) throw failure;
                if (cause instanceof java.net.http.HttpTimeoutException) throw new Failure(Problem.PROVIDER_TIMEOUT);
            }
            throw new Failure(Problem.PROVIDER_UNAVAILABLE);
        }
    }

    /** Stop non-success bodies at headers and enforce a streaming byte limit before buffering successful bodies. */
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> bytes = HttpResponse.BodySubscribers.ofByteArray();
        private final boolean read;
        private Flow.Subscription subscription;
        private int remaining = MAX_BYTES;
        private boolean done;
        BoundedBody(boolean read) { this.read = read; }
        public CompletionStage<byte[]> getBody() { return bytes.getBody(); }
        public void onSubscribe(Flow.Subscription s) {
            subscription = s;
            if (read) bytes.onSubscribe(s);
            else { done = true; s.cancel(); bytes.onComplete(); }
        }
        public void onNext(List<ByteBuffer> buffers) {
            if (done) return;
            for (var buffer : buffers) {
                if (buffer.remaining() > remaining) {
                    done = true;
                    bytes.onError(new Failure(Problem.INVALID_PROVIDER_RESPONSE));
                    subscription.cancel(); return;
                }
                remaining -= buffer.remaining();
            }
            bytes.onNext(buffers);
        }
        public void onError(Throwable error) { if (!done) bytes.onError(error); }
        public void onComplete() { if (!done) bytes.onComplete(); }
    }
}
