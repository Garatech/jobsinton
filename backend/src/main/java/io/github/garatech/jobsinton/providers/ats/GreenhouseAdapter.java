package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Greenhouse public Job Board API adapter with automatic EU instance fallback.
 * Primary:  GET https://boards-api.greenhouse.io/v1/boards/{company_slug}/jobs?content=true
 * Fallback: GET https://boards-api.eu.greenhouse.io/v1/boards/{company_slug}/jobs?content=true
 */
public final class GreenhouseAdapter implements AtsProviderAdapter {
    private static final String BASE_URL = "https://boards-api.greenhouse.io/v1/boards/";
    private static final String EU_BASE_URL = "https://boards-api.eu.greenhouse.io/v1/boards/";

    private final AtsHttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public GreenhouseAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.GREENHOUSE;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        String encodedSlug = AtsSupport.encodePathSegment(board.slug());
        URI primaryUri = URI.create(BASE_URL + encodedSlug + "/jobs?content=true");
        URI euUri = URI.create(EU_BASE_URL + encodedSlug + "/jobs?content=true");

        var reply = http.get(primaryUri);
        if (reply.status() == 404) {
            var euReply = http.get(euUri);
            if (euReply.status() == 200) {
                reply = euReply;
            }
        }
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from Greenhouse");
        }
        List<RawAtsJob> jobs = parseJobsPayload(board, reply.body());
        if (jobs.isEmpty()) {
            try {
                var euReply = http.get(euUri);
                if (euReply.status() == 200) {
                    List<RawAtsJob> euJobs = parseJobsPayload(board, euReply.body());
                    if (!euJobs.isEmpty()) {
                        return euJobs;
                    }
                }
            } catch (AtsFetchException ignored) {
                // Keep primary empty result if EU fallback fails.
            }
        }
        return jobs;
    }

    private List<RawAtsJob> parseJobsPayload(CompanyBoard board, byte[] body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode jobsNode = root == null ? null : root.path("jobs");
            if (jobsNode == null || !jobsNode.isArray()) {
                throw new AtsFetchException("Greenhouse payload missing jobs array");
            }
            List<RawAtsJob> jobs = new ArrayList<>(jobsNode.size());
            for (JsonNode job : jobsNode) {
                RawAtsJob parsed = mapJob(board, job);
                if (parsed != null) {
                    jobs.add(parsed);
                }
            }
            return jobs;
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable JSON from Greenhouse", e);
        }
    }

    private static RawAtsJob mapJob(CompanyBoard board, JsonNode job) {
        if (job == null || !job.isObject()) {
            return null;
        }
        String id = AtsSupport.idText(job.path("id"));
        String title = AtsSupport.text(job.path("title"));
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("absolute_url")));
        if (id == null || title == null || applyUrl == null) {
            return null;
        }
        // Prefer first_published over updated_at so minor typo edits don't reset job age.
        Instant publishedAt = AtsSupport.parseTimestamp(job.path("first_published"));
        if (publishedAt == null) {
            publishedAt = AtsSupport.parseTimestamp(job.path("updated_at"));
        }
        String location = AtsSupport.text(job.path("location").path("name"));
        String description = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("content")));
        String department = firstDepartment(job.path("departments"));
        String workplaceType = workplaceFromMetadata(job.path("metadata"));
        if (workplaceType == null) {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }
        return new RawAtsJob(
                id,
                AtsProviderType.GREENHOUSE,
                board.slug(),
                board.name(),
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                applyUrl,
                department,
                null,
                !description.isEmpty()
        );
    }

    private static String firstDepartment(JsonNode departments) {
        if (departments == null || !departments.isArray()) {
            return null;
        }
        for (JsonNode dept : departments) {
            String name = AtsSupport.text(dept.path("name"));
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    private static String workplaceFromMetadata(JsonNode metadata) {
        if (metadata == null || !metadata.isArray()) {
            return null;
        }
        for (JsonNode entry : metadata) {
            String name = AtsSupport.text(entry.path("name"));
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.contains("workplace") || lower.contains("work mode") || lower.contains("remote") || lower.contains("location type")) {
                String normalized = AtsSupport.normalizeWorkplaceType(AtsSupport.text(entry.path("value")));
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }
}
