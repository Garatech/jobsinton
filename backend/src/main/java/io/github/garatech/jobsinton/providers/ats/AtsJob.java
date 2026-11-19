package io.github.garatech.jobsinton.providers.ats;

/** Unified job posting normalized from a public company ATS endpoint. */
public record AtsJob(
        String id,
        String sourceId,
        String provider,
        String providerLabel,
        String companySlug,
        String companyName,
        String title,
        String location,
        String workplaceType,
        String publishedAt,
        String firstSeenAt,
        Long hoursSincePublished,
        String description,
        String descriptionSnippet,
        String applyUrl,
        String department,
        String compensation,
        boolean hasFullDescription
) {
    public AtsJob(
            String id,
            String sourceId,
            String provider,
            String providerLabel,
            String companySlug,
            String companyName,
            String title,
            String location,
            String workplaceType,
            String publishedAt,
            Long hoursSincePublished,
            String description,
            String descriptionSnippet,
            String applyUrl,
            String department,
            String compensation,
            boolean hasFullDescription
    ) {
        this(
                id,
                sourceId,
                provider,
                providerLabel,
                companySlug,
                companyName,
                title,
                location,
                workplaceType,
                publishedAt,
                publishedAt,
                hoursSincePublished,
                description,
                descriptionSnippet,
                applyUrl,
                department,
                compensation,
                hasFullDescription
        );
    }
}
