package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 * Loads and exposes the pre-seeded registry of public company ATS boards,
 * merged with optional user-discovered custom boards persisted in {@code data/custom-companies.json}.
 */
public final class CompanyRegistry {
    public static final String DEFAULT_RESOURCE = "/ats/companies.json";
    public static final Path DEFAULT_CUSTOM_PATH = Path.of("data", "custom-companies.json");

    private final List<CompanyBoard> seedBoards;
    private final List<CompanyBoard> customBoards = new ArrayList<>();
    private final Path customBoardsPath;
    private final Object lock = new Object();

    public CompanyRegistry(List<CompanyBoard> boards) {
        this(boards, null);
    }

    public CompanyRegistry(List<CompanyBoard> boards, Path customBoardsPath) {
        this.seedBoards = List.copyOf(boards);
        this.customBoardsPath = customBoardsPath;
        if (customBoardsPath != null) {
            loadCustomFromDisk(customBoardsPath);
        }
    }

    public static CompanyRegistry loadDefault() {
        return loadFromClasspath(DEFAULT_RESOURCE, DEFAULT_CUSTOM_PATH);
    }

    public static CompanyRegistry loadFromClasspath(String resourcePath) {
        return loadFromClasspath(resourcePath, null);
    }

    public static CompanyRegistry loadFromClasspath(String resourcePath, Path customBoardsPath) {
        try (InputStream stream = CompanyRegistry.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Company ATS registry resource not found: " + resourcePath);
            }
            List<CompanyBoard> loaded = parseBoardsStream(stream);
            return new CompanyRegistry(loaded, customBoardsPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read company ATS registry: " + resourcePath, e);
        }
    }

    static List<CompanyBoard> parseBoardsStream(InputStream stream) throws IOException {
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
        return loaded;
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
        synchronized (lock) {
            Map<String, CompanyBoard> merged = new LinkedHashMap<>();
            for (CompanyBoard b : seedBoards) {
                merged.put(boardKey(b), b);
            }
            for (CompanyBoard b : customBoards) {
                merged.put(boardKey(b), b);
            }
            return List.copyOf(merged.values());
        }
    }

    public Optional<CompanyBoard> find(AtsProviderType provider, String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String target = slug.strip();
        return boards().stream()
                .filter(b -> b.providerType() == provider && b.slug().equalsIgnoreCase(target))
                .findFirst();
    }

    /**
     * Registers a discovered company board if not already present, persists custom boards to disk,
     * and returns {@code true} if the board was newly added.
     */
    public boolean registerCustomBoard(CompanyBoard board) {
        if (board == null) {
            return false;
        }
        String key = boardKey(board);
        synchronized (lock) {
            for (CompanyBoard existing : seedBoards) {
                if (boardKey(existing).equals(key)) {
                    return false;
                }
            }
            for (int i = 0; i < customBoards.size(); i++) {
                if (boardKey(customBoards.get(i)).equals(key)) {
                    customBoards.set(i, board);
                    persistCustomToDisk();
                    return false;
                }
            }
            customBoards.add(board);
            persistCustomToDisk();
            return true;
        }
    }

    private static String boardKey(CompanyBoard board) {
        return board.providerType().id() + ":" + board.slug().toLowerCase(Locale.ROOT);
    }

    private void loadCustomFromDisk(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (InputStream stream = Files.newInputStream(path)) {
            List<CompanyBoard> loaded = parseBoardsStream(stream);
            synchronized (lock) {
                customBoards.clear();
                customBoards.addAll(loaded);
            }
        } catch (RuntimeException | IOException ignored) {
            // Non-fatal: start cleanly if local custom file is missing or malformed
        }
    }

    private void persistCustomToDisk() {
        if (customBoardsPath == null) {
            return;
        }
        try {
            Path parent = customBoardsPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonMapper mapper = JsonMapper.builder().build();
            ArrayNode array = mapper.createArrayNode();
            for (CompanyBoard board : customBoards) {
                ObjectNode node = mapper.createObjectNode();
                node.put("slug", board.slug());
                node.put("name", board.name());
                node.put("ats", board.ats());
                if (board.region() != null) node.put("region", board.region());
                if (board.locationHint() != null) node.put("locationHint", board.locationHint());
                if (board.host() != null) node.put("host", board.host());
                if (board.careersUrl() != null) node.put("careersUrl", board.careersUrl());
                if (board.apiEndpoint() != null) node.put("apiEndpoint", board.apiEndpoint());
                if (board.fallbackEndpoint() != null) node.put("fallbackEndpoint", board.fallbackEndpoint());
                if (!board.locations().isEmpty()) {
                    ArrayNode locs = node.putArray("locations");
                    board.locations().forEach(locs::add);
                }
                if (!board.stackMatch().isEmpty()) {
                    ArrayNode stacks = node.putArray("stackMatch");
                    board.stackMatch().forEach(stacks::add);
                }
                if (board.priority() != null) node.put("priority", board.priority());
                array.add(node);
            }
            Path tmp = customBoardsPath.resolveSibling(customBoardsPath.getFileName().toString() + ".tmp");
            Files.writeString(tmp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array), StandardCharsets.UTF_8);
            Files.move(tmp, customBoardsPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (RuntimeException | IOException ignored) {
            // Non-fatal if running in a read-only container
        }
    }
}
