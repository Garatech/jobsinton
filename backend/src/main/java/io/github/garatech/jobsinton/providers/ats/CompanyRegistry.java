package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and manages the pre-seeded registry of public company ATS boards alongside
 * user-discovered boards persisted in a local JSON file (`data/custom-companies.json`).
 */
public final class CompanyRegistry {
    public static final String DEFAULT_RESOURCE = "/ats/companies.json";
    public static final Path DEFAULT_CUSTOM_PATH = Path.of("data", "custom-companies.json");

    private final List<CompanyBoard> seedBoards;
    private final Path customFilePath;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final List<CompanyBoard> customBoards = new ArrayList<>();

    public CompanyRegistry(List<CompanyBoard> boards) {
        this(boards, null);
    }

    public CompanyRegistry(List<CompanyBoard> seedBoards, Path customFilePath) {
        this.seedBoards = List.copyOf(seedBoards);
        this.customFilePath = customFilePath;
        if (customFilePath != null) {
            loadCustomFromDisk();
        }
    }

    public static CompanyRegistry loadDefault() {
        return loadFromClasspath(DEFAULT_RESOURCE, DEFAULT_CUSTOM_PATH);
    }

    public static CompanyRegistry loadFromClasspath(String resourcePath) {
        return loadFromClasspath(resourcePath, null);
    }

    public static CompanyRegistry loadFromClasspath(String resourcePath, Path customFilePath) {
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
            return new CompanyRegistry(loaded, customFilePath);
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
        try {
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
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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

    public synchronized List<CompanyBoard> boards() {
        if (customBoards.isEmpty()) {
            return seedBoards;
        }
        Map<String, CompanyBoard> merged = new LinkedHashMap<>();
        for (CompanyBoard board : seedBoards) {
            merged.put(boardKey(board.providerType(), board.slug()), board);
        }
        for (CompanyBoard board : customBoards) {
            merged.put(boardKey(board.providerType(), board.slug()), board);
        }
        return List.copyOf(merged.values());
    }

    public synchronized Optional<CompanyBoard> find(AtsProviderType provider, String slug) {
        if (slug == null || slug.isBlank() || provider == null) {
            return Optional.empty();
        }
        String target = slug.strip();
        return boards().stream()
                .filter(b -> b.providerType() == provider && b.slug().equalsIgnoreCase(target))
                .findFirst();
    }

    /**
     * Registers a newly discovered company board if not already present, persists it to
     * `custom-companies.json` when configured, and returns whether it was newly added.
     */
    public synchronized boolean registerCustomBoard(CompanyBoard board) {
        if (board == null) {
            return false;
        }
        String key = boardKey(board.providerType(), board.slug());
        for (CompanyBoard existing : seedBoards) {
            if (boardKey(existing.providerType(), existing.slug()).equals(key)) {
                return false;
            }
        }
        for (int i = 0; i < customBoards.size(); i++) {
            CompanyBoard existing = customBoards.get(i);
            if (boardKey(existing.providerType(), existing.slug()).equals(key)) {
                customBoards.set(i, board);
                persistCustomToDisk();
                return false;
            }
        }
        customBoards.add(board);
        persistCustomToDisk();
        return true;
    }

    private static String boardKey(AtsProviderType type, String slug) {
        return type.id() + ":" + slug.strip().toLowerCase(Locale.ROOT);
    }

    private void loadCustomFromDisk() {
        if (customFilePath == null || !Files.isRegularFile(customFilePath)) {
            return;
        }
        try (InputStream in = Files.newInputStream(customFilePath)) {
            JsonNode root = mapper.readTree(in);
            if (root != null && root.isArray()) {
                for (JsonNode entry : root) {
                    CompanyBoard parsed = parseBoardNode(entry);
                    if (parsed != null) {
                        customBoards.add(parsed);
                    }
                }
            }
        } catch (Exception ignored) {
            // Keep seed catalog working if custom file is corrupt or unreadable.
        }
    }

    private void persistCustomToDisk() {
        if (customFilePath == null) {
            return;
        }
        try {
            Path parent = customFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ArrayNode array = mapper.createArrayNode();
            for (CompanyBoard board : customBoards) {
                ObjectNode obj = array.addObject();
                obj.put("slug", board.slug());
                obj.put("name", board.name());
                obj.put("ats", board.ats());
                obj.put("region", board.region());
                if (board.locationHint() != null) obj.put("locationHint", board.locationHint());
                if (board.host() != null) obj.put("host", board.host());
                if (board.careersUrl() != null) obj.put("careersUrl", board.careersUrl());
                if (board.apiEndpoint() != null) obj.put("apiEndpoint", board.apiEndpoint());
                if (board.fallbackEndpoint() != null) obj.put("fallbackEndpoint", board.fallbackEndpoint());
                ArrayNode locs = obj.putArray("locations");
                for (String loc : board.locations()) locs.add(loc);
                ArrayNode stack = obj.putArray("stackMatch");
                for (String tech : board.stackMatch()) stack.add(tech);
                obj.put("priority", board.priority());
            }
            Path tmp = customFilePath.resolveSibling(customFilePath.getFileName().toString() + ".tmp");
            Files.writeString(tmp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array));
            Files.move(tmp, customFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            // Non-fatal if disk write fails in read-only environments.
        }
    }
}
