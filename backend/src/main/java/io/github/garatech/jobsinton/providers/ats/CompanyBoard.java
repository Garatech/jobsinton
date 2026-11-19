package io.github.garatech.jobsinton.providers.ats;

import java.util.List;
import java.util.Locale;

/** Configured company career board hosted on a public ATS endpoint (JSON, XML, RSS, or HTML). */
public record CompanyBoard(
        String slug,
        String name,
        String ats,
        String region,
        String locationHint,
        String host,
        String careersUrl,
        String apiEndpoint,
        String fallbackEndpoint,
        List<String> locations,
        List<String> stackMatch,
        String priority
) {
    public CompanyBoard(
            String slug,
            String name,
            String ats,
            String region,
            String locationHint
    ) {
        this(slug, name, ats, region, locationHint, null, null, null, null, List.of(), List.of(), "medium");
    }

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
        host = clean(host);
        careersUrl = clean(careersUrl);
        apiEndpoint = clean(apiEndpoint);
        fallbackEndpoint = clean(fallbackEndpoint);
        locations = locations == null ? List.of() : List.copyOf(locations);
        stackMatch = stackMatch == null ? List.of() : List.copyOf(stackMatch);
        priority = clean(priority) == null ? "medium" : clean(priority).toLowerCase(Locale.ROOT);
        if (region == null || region.isBlank()) {
            if (host != null && (host.contains(".eu.") || host.endsWith(".eu") || host.endsWith(".es") || host.endsWith(".de"))) {
                region = "eu";
            } else {
                region = "global";
            }
        } else {
            region = region.strip().toLowerCase(Locale.ROOT);
        }
        if (locationHint == null || locationHint.isBlank()) {
            locationHint = locations.isEmpty() ? null : String.join(" / ", locations);
        } else {
            locationHint = locationHint.strip();
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public AtsProviderType providerType() {
        return AtsProviderType.fromId(ats);
    }

    public boolean isEuRegion() {
        return "eu".equals(region)
                || (host != null && host.toLowerCase(Locale.ROOT).contains("api.eu.lever.co"))
                || (apiEndpoint != null && apiEndpoint.toLowerCase(Locale.ROOT).contains("api.eu.lever.co"));
    }
}
