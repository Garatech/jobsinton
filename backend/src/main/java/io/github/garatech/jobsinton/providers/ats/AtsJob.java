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
        Long hoursSincePublished,
        String description,
        String descriptionSnippet,
        String applyUrl,
        String department,
        String compensation,
        boolean hasFullDescription
) {}
