package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight local JSON registry (`data/ats-seen-jobs.json`) recording the earliest local sighting
 * timestamp (`firstSeenAt`) for every job ID. Prevents Greenhouse `updated_at` edits or missing dates
 * from faking fresh `< 24h` badges on older listings.
 */
public final class FirstSeenRegistry {
    public static final Path DEFAULT_PATH = Path.of("data", "ats-seen-jobs.json");

    private final Path filePath;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Instant> seenById = new ConcurrentHashMap<>();

    public FirstSeenRegistry() {
        this(null);
    }

    public FirstSeenRegistry(Path filePath) {
        this.filePath = filePath;
        if (filePath != null) {
            loadFromDisk();
        }
    }

    public static FirstSeenRegistry loadDefault() {
        return new FirstSeenRegistry(DEFAULT_PATH);
    }

    /**
     * Records or retrieves the first-seen timestamp for each job in {@code jobs} at {@code now}
     * and persists any newly seen IDs to disk once per batch.
     */
    public synchronized void recordBatch(Collection<RawAtsJob> jobs, Instant now) {
        if (jobs == null || jobs.isEmpty() || now == null) {
            return;
        }
        boolean dirty = false;
        for (RawAtsJob job : jobs) {
            String id = job.compositeId();
            Instant existing = seenById.get(id);
            if (existing == null) {
                seenById.put(id, now);
                dirty = true;
            }
        }
        if (dirty) {
            persistToDisk();
        }
    }

    public Instant firstSeenOrRecord(String compositeId, Instant now) {
        if (compositeId == null || now == null) {
            return now;
        }
        return seenById.computeIfAbsent(compositeId, k -> now);
    }

    public Instant getFirstSeen(String compositeId) {
        return compositeId == null ? null : seenById.get(compositeId);
    }

    public int size() {
        return seenById.size();
    }

    private void loadFromDisk() {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return;
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            JsonNode root = mapper.readTree(in);
            if (root != null && root.isObject()) {
                for (Map.Entry<String, JsonNode> entry : root.properties()) {
                    Instant parsed = AtsSupport.parseTimestampString(AtsSupport.text(entry.getValue()));
                    if (parsed != null) {
                        seenById.put(entry.getKey(), parsed);
                    }
                }
            }
        } catch (Exception ignored) {
            // Start with clean in-memory state if local file cannot be read.
        }
    }

    private void persistToDisk() {
        if (filePath == null) {
            return;
        }
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ObjectNode root = mapper.createObjectNode();
            seenById.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> root.put(e.getKey(), e.getValue().toString()));
            Path tmp = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
            Files.writeString(tmp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            // Non-fatal in read-only environments.
        }
    }
}
