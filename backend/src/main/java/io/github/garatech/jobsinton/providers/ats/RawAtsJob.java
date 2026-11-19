package io.github.garatech.jobsinton.providers.ats;

import java.time.Duration;
import java.time.Instant;

/** Internal normalized posting produced by an ATS adapter before feed-level freshness calculation. */
public record RawAtsJob(
        String sourceId,
        AtsProviderType provider,
        String companySlug,
        String companyName,
        String title,
        String location,
        String workplaceType,
        Instant publishedAt,
        String description,
        String applyUrl,
        String department,
        String compensation,
        boolean hasFullDescription
) {
    public String compositeId() {
        return provider.id() + ":" + companySlug + ":" + sourceId;
    }

    public AtsJob toApiJob(Instant now) {
        return toApiJob(now, null);
    }

    public AtsJob toApiJob(Instant now, Instant firstSeenAt) {
        Instant effectiveOrigin = publishedAt;
        if (firstSeenAt != null && (effectiveOrigin == null || firstSeenAt.isBefore(effectiveOrigin))) {
            effectiveOrigin = firstSeenAt;
        }
        Long hours = null;
        if (effectiveOrigin != null) {
            long diff = Duration.between(effectiveOrigin, now).toHours();
            hours = Math.max(0L, diff);
        }
        String cleanDesc = description == null ? "" : description;
        return new AtsJob(
                compositeId(),
                sourceId,
                provider.id(),
                provider.displayName(),
                companySlug,
                companyName,
                title,
                location,
                workplaceType,
                publishedAt == null ? null : publishedAt.toString(),
                firstSeenAt == null ? (publishedAt == null ? null : publishedAt.toString()) : firstSeenAt.toString(),
                hours,
                cleanDesc,
                HtmlTextCleaner.snippet(cleanDesc, 260),
                applyUrl,
                department,
                compensation,
                hasFullDescription
        );
    }
}
