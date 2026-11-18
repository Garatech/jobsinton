package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Loads and exposes the pre-seeded registry of public company ATS boards. */
public final class CompanyRegistry {
    public static final String DEFAULT_RESOURCE = "/ats/companies.json";

    private final List<CompanyBoard> boards;

    public CompanyRegistry(List<CompanyBoard> boards) {
        this.boards = List.copyOf(boards);
    }

    public static CompanyRegistry loadDefault() {
        return loadFromClasspath(DEFAULT_RESOURCE);
    }

    public static CompanyRegistry loadFromClasspath(String resourcePath) {
        try (InputStream stream = CompanyRegistry.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Company ATS registry resource not found: " + resourcePath);
            }
            JsonMapper mapper = JsonMapper.builder().build();
            JsonNode root = mapper.readTree(stream);
            if (root == null || !root.isArray()) {
                throw new IllegalStateException("Company ATS registry must be a JSON array");
            }
            List<CompanyBoard> loaded = new ArrayList<>();
            for (JsonNode entry : root) {
                String slug = AtsSupport.text(entry.path("slug"));
                String name = AtsSupport.text(entry.path("name"));
                String ats = AtsSupport.text(entry.path("ats"));
                String region = AtsSupport.text(entry.path("region"));
                String locationHint = AtsSupport.text(entry.path("locationHint"));
                loaded.add(new CompanyBoard(slug, name, ats, region, locationHint));
            }
            return new CompanyRegistry(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read company ATS registry: " + resourcePath, e);
        }
    }

    public List<CompanyBoard> boards() {
        return boards;
    }

    public Optional<CompanyBoard> find(AtsProviderType provider, String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String target = slug.strip();
        return boards.stream()
                .filter(b -> b.providerType() == provider && b.slug().equals(target))
                .findFirst();
    }
}
