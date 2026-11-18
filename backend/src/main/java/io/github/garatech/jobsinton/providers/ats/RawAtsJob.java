package io.github.garatech.jobsinton.providers.ats;

import java.time.Instant;
import java.time.Duration;

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
        Long hours = null;
        if (publishedAt != null) {
            long diff = Duration.between(publishedAt, now).toHours();
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
