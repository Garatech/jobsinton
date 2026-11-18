package io.github.garatech.jobsinton.providers.ats;

/** On-demand detail payload hydrated from the SmartRecruiters posting detail endpoint. */
public record SmartRecruitersDetail(
        String id,
        String description,
        String descriptionSnippet,
        String applyUrl,
        boolean hasFullDescription
) {}
