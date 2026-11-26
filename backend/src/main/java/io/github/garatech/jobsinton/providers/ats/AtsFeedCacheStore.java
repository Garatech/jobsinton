package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Local disk snapshot store ({@code data/ats-feed-cache.json}) persisting the last aggregated
 * Multi-ATS feed so cold starts and container restarts serve all cached opportunities in milliseconds
 * while a background virtual-thread refresh updates live boards asynchronously.
 */
public final class AtsFeedCacheStore {
    public static final Path DEFAULT_PATH = Path.of("data", "ats-feed-cache.json");
    private static final int SCHEMA_VERSION = 1;

    public record Snapshot(Instant fetchedAt, List<BoardStatus> boards, List<RawAtsJob> jobs) {}

    private final Path filePath;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Object writeLock = new Object();

    public AtsFeedCacheStore() {
        this(null);
    }

    public AtsFeedCacheStore(Path filePath) {
        this.filePath = filePath;
    }

    public static AtsFeedCacheStore loadDefault() {
        return new AtsFeedCacheStore(DEFAULT_PATH);
    }

    public Optional<Snapshot> load() {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return Optional.empty();
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            JsonNode root = mapper.readTree(in);
            if (root == null || !root.isObject()) {
                return Optional.empty();
            }
            if (root.path("version").asInt(0) != SCHEMA_VERSION) {
                return Optional.empty();
            }
            Instant fetchedAt = AtsSupport.parseTimestampString(AtsSupport.text(root.path("fetchedAt")));
            JsonNode boardsNode = root.path("boards");
            JsonNode jobsNode = root.path("jobs");
            if (fetchedAt == null || !boardsNode.isArray() || !jobsNode.isArray()) {
                return Optional.empty();
            }

            List<BoardStatus> boards = new ArrayList<>(boardsNode.size());
            for (JsonNode b : boardsNode) {
                BoardStatus parsed = parseBoardStatus(b);
                if (parsed != null) {
                    boards.add(parsed);
                }
            }

            List<RawAtsJob> jobs = new ArrayList<>(jobsNode.size());
            for (JsonNode j : jobsNode) {
                RawAtsJob parsed = parseRawJob(j);
                if (parsed != null) {
                    jobs.add(parsed);
                }
            }

            if (boards.isEmpty() && jobs.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Snapshot(fetchedAt, List.copyOf(boards), List.copyOf(jobs)));
        } catch (Exception ignored) {
            // Non-fatal: fall back to live fetch if local snapshot is unreadable or partial.
            return Optional.empty();
        }
    }

    public void save(Instant fetchedAt, List<BoardStatus> boards, List<RawAtsJob> jobs) {
        if (filePath == null || fetchedAt == null || boards == null || jobs == null) {
            return;
        }
        synchronized (writeLock) {
            try {
                Path parent = filePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                ObjectNode root = mapper.createObjectNode();
                root.put("version", SCHEMA_VERSION);
                root.put("fetchedAt", fetchedAt.toString());

                ArrayNode boardsArray = root.putArray("boards");
                for (BoardStatus b : boards) {
                    ObjectNode bn = boardsArray.addObject();
                    bn.put("companySlug", b.companySlug());
                    bn.put("companyName", b.companyName());
                    bn.put("provider", b.provider());
                    bn.put("providerLabel", b.providerLabel());
                    bn.put("ok", b.ok());
                    bn.put("jobCount", b.jobCount());
                    bn.put("durationMs", b.durationMs());
                    if (b.error() != null) {
                        bn.put("error", b.error());
                    } else {
                        bn.putNull("error");
                    }
                }

                ArrayNode jobsArray = root.putArray("jobs");
                for (RawAtsJob j : jobs) {
                    ObjectNode jn = jobsArray.addObject();
                    jn.put("sourceId", j.sourceId());
                    jn.put("provider", j.provider().id());
                    jn.put("companySlug", j.companySlug());
                    jn.put("companyName", j.companyName());
                    jn.put("title", j.title());
                    putNullable(jn, "location", j.location());
                    putNullable(jn, "workplaceType", j.workplaceType());
                    putNullable(jn, "publishedAt", j.publishedAt() != null ? j.publishedAt().toString() : null);
                    jn.put("description", j.description() != null ? j.description() : "");
                    jn.put("applyUrl", j.applyUrl());
                    putNullable(jn, "department", j.department());
                    putNullable(jn, "compensation", j.compensation());
                    jn.put("hasFullDescription", j.hasFullDescription());
                }

                Path tmp = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
                Files.write(tmp, mapper.writeValueAsBytes(root));
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
                // Non-fatal in read-only environments.
            }
        }
    }

    private static void putNullable(ObjectNode node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        } else {
            node.putNull(field);
        }
    }

    private static BoardStatus parseBoardStatus(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String slug = AtsSupport.text(node.path("companySlug"));
        String name = AtsSupport.text(node.path("companyName"));
        String providerRaw = AtsSupport.text(node.path("provider"));
        if (slug == null || name == null || providerRaw == null) {
            return null;
        }
        AtsProviderType type;
        try {
            type = AtsProviderType.fromId(providerRaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String providerLabel = AtsSupport.text(node.path("providerLabel"));
        boolean ok = node.path("ok").asBoolean(false);
        int jobCount = Math.max(0, node.path("jobCount").asInt(0));
        long durationMs = Math.max(0L, node.path("durationMs").asLong(0L));
        String error = AtsSupport.text(node.path("error"));
        return new BoardStatus(
                slug,
                name,
                type.id(),
                providerLabel != null ? providerLabel : type.displayName(),
                ok,
                jobCount,
                durationMs,
                error
        );
    }

    private static RawAtsJob parseRawJob(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String sourceId = AtsSupport.idText(node.path("sourceId"));
        String providerRaw = AtsSupport.text(node.path("provider"));
        String companySlug = AtsSupport.text(node.path("companySlug"));
        String companyName = AtsSupport.text(node.path("companyName"));
        String title = AtsSupport.text(node.path("title"));
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(node.path("applyUrl")));
        if (sourceId == null || providerRaw == null || companySlug == null || companyName == null
                || title == null || applyUrl == null) {
            return null;
        }
        AtsProviderType provider;
        try {
            provider = AtsProviderType.fromId(providerRaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String location = AtsSupport.text(node.path("location"));
        String workplaceType = AtsSupport.normalizeWorkplaceType(AtsSupport.text(node.path("workplaceType")));
        Instant publishedAt = AtsSupport.parseTimestampString(AtsSupport.text(node.path("publishedAt")));
        String description = node.path("description").isString() ? node.path("description").asString() : "";
        String department = AtsSupport.text(node.path("department"));
        String compensation = AtsSupport.text(node.path("compensation"));
        boolean hasFullDescription = node.path("hasFullDescription").asBoolean(!description.isEmpty());

        return new RawAtsJob(
                sourceId,
                provider,
                companySlug,
                companyName,
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                applyUrl,
                department,
                compensation,
                hasFullDescription
        );
        }
}
