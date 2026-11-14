package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;
import io.github.garatech.jobsinton.opportunities.OpportunityListing;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/** A short-lived signed snapshot. The server retains its key, not listings or profiles. */
final class AdzunaComparisonRefs {
    static final Duration LIFETIME = Duration.ofHours(2);
    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final JsonMapper json = JsonMapper.builder().build();
    private final SecretKeySpec key;
    private final Clock clock;

    AdzunaComparisonRefs(Clock clock) {
        this.clock = clock;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    record Snapshot(String id, Opportunity facts, long expiresAt) {}

    Optional<String> issue(OpportunityListing listing) {
        // Unusually long provider identifiers can still be displayed as search results.
        if (listing.id().length() > 256) return Optional.empty();
        var snapshot = new Snapshot(listing.id(), listing.matchingFacts(), clock.instant().plus(LIFETIME).getEpochSecond());
        byte[] payload = json.writeValueAsBytes(snapshot);
        return Optional.of(ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(sign(payload)));
    }

    Optional<Snapshot> verify(String token) {
        if (token.length() > MAX_TOKEN_LENGTH) return Optional.empty();
        int separator = token.indexOf('.');
        if (separator <= 0 || separator != token.lastIndexOf('.')) return Optional.empty();
        try {
            byte[] payload = DECODER.decode(token.substring(0, separator));
            byte[] signature = DECODER.decode(token.substring(separator + 1));
            if (!MessageDigest.isEqual(sign(payload), signature)) return Optional.empty();
            var snapshot = json.readValue(payload, Snapshot.class);
            return clock.instant().getEpochSecond() < snapshot.expiresAt() ? Optional.of(snapshot) : Optional.empty();
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(key.getAlgorithm());
            mac.init(key);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("Cannot sign local comparison references", unavailable);
        }
    }
}
