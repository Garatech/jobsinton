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
            JsonNode arrayNode = root != null && root.isObject() && root.path("companies").isArray()
                    ? root.path("companies")
                    : root;
            if (arrayNode == null || !arrayNode.isArray()) {
                throw new IllegalStateException("Company ATS registry must be a JSON array");
            }
            List<CompanyBoard> loaded = new ArrayList<>();
            for (JsonNode entry : arrayNode) {
                CompanyBoard board = parseBoardNode(entry);
                if (board != null) {
                    loaded.add(board);
                }
            }
            return new CompanyRegistry(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read company ATS registry: " + resourcePath, e);
        }
    }

    static CompanyBoard parseBoardNode(JsonNode entry) {
        if (entry == null || !entry.isObject()) {
            return null;
        }
        String slug = AtsSupport.text(entry.path("slug"));
        String name = AtsSupport.text(entry.path("name"));
        String ats = AtsSupport.text(entry.path("ats"));
        if (slug == null || name == null || ats == null) {
            return null;
        }
        String region = AtsSupport.text(entry.path("region"));
        String locationHint = AtsSupport.text(entry.path("locationHint"));
        String host = AtsSupport.text(entry.path("host"));
        String careersUrl = firstNonNull(AtsSupport.text(entry.path("careersUrl")), AtsSupport.text(entry.path("careers_url")));
        String apiEndpoint = firstNonNull(AtsSupport.text(entry.path("apiEndpoint")), AtsSupport.text(entry.path("api_endpoint")));
        String fallbackEndpoint = firstNonNull(AtsSupport.text(entry.path("fallbackEndpoint")), AtsSupport.text(entry.path("fallback_endpoint")));
        List<String> locations = readStringArray(entry.path("locations"));
        List<String> stackMatch = readStringArray(entry.path("stackMatch").isArray() ? entry.path("stackMatch") : entry.path("stack_match"));
        String priority = AtsSupport.text(entry.path("priority"));
        return new CompanyBoard(
                slug,
                name,
                ats,
                region,
                locationHint,
                host,
                careersUrl,
                apiEndpoint,
                fallbackEndpoint,
                locations,
                stackMatch,
                priority
        );
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> items = new ArrayList<>(node.size());
        for (JsonNode child : node) {
            String value = AtsSupport.text(child);
            if (value != null) {
                items.add(value);
            }
        }
        return items;
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
                .filter(b -> b.providerType() == provider && b.slug().equalsIgnoreCase(target))
                .findFirst();
    }
}
