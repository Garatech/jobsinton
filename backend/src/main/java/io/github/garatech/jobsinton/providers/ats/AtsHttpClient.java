package io.github.garatech.jobsinton.providers.ats;

import java.net.URI;

/** Transport abstraction for public ATS JSON endpoints so adapters are fully unit-testable offline. */
public interface AtsHttpClient {
    record HttpReply(int status, byte[] body) {}

    HttpReply get(URI uri);

    HttpReply postJson(URI uri, String jsonBody);
}
