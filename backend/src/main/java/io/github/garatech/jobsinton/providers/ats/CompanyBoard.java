package io.github.garatech.jobsinton.providers.ats;

import java.util.Locale;

/** Configured company career board hosted on a public ATS JSON endpoint. */
public record CompanyBoard(
        String slug,
        String name,
        String ats,
        String region,
        String locationHint
) {
    public CompanyBoard {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Company board slug must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company board name must not be blank");
        }
        slug = slug.strip();
        name = name.strip();
        ats = AtsProviderType.fromId(ats).id();
        region = region == null || region.isBlank() ? "global" : region.strip().toLowerCase(Locale.ROOT);
        locationHint = locationHint == null || locationHint.isBlank() ? null : locationHint.strip();
    }

    public AtsProviderType providerType() {
        return AtsProviderType.fromId(ats);
    }

    public boolean isEuRegion() {
        return "eu".equals(region);
    }
}
