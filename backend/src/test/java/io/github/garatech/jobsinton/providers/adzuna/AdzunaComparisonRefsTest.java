package io.github.garatech.jobsinton.providers.adzuna;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdzunaComparisonRefsTest {
    @Test void alteredPayloadRestartAndExpiryInvalidateTheReference() {
        var clock = mock(Clock.class);
        var start = Instant.parse("2026-11-14T12:00:00Z");
        when(clock.instant()).thenReturn(start);
        var refs = new AdzunaComparisonRefs(clock);
        var row = JsonMapper.builder().build().readTree(AdzunaSearchTest.ROW);
        var listing = ((AdzunaListingMapper.Accepted) new AdzunaListingMapper().map(row)).listing();
        String token = refs.issue(listing).orElseThrow();
        assertThat(refs.verify(token).orElseThrow().facts()).isEqualTo(listing.matchingFacts());
        String[] parts = token.split("\\.");
        String other = new String(Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8)
                .replace("adzuna:one", "adzuna:two");
        String altered = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(other.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + parts[1];
        assertThat(refs.verify(altered)).isEmpty();
        assertThat(new AdzunaComparisonRefs(clock).verify(token)).isEmpty();
        when(clock.instant()).thenReturn(start.plus(AdzunaComparisonRefs.LIFETIME));
        assertThat(refs.verify(token)).isEmpty();
    }
}
