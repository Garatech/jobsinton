package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ashby public Posting API adapter.
 * Endpoint: GET https://api.ashbyhq.com/posting-api/job-board/{company_slug}?includeCompensation=true
 */
public final class AshbyAdapter implements AtsProviderAdapter {
    private static final String BASE_URL = "https://api.ashbyhq.com/posting-api/job-board/";

    private final AtsHttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public AshbyAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.ASHBY;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        URI uri = URI.create(BASE_URL + AtsSupport.encodePathSegment(board.slug()) + "?includeCompensation=true");
        var reply = http.get(uri);
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from Ashby");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            JsonNode jobsNode = root == null ? null : root.path("jobs");
            if (jobsNode == null || !jobsNode.isArray()) {
                throw new AtsFetchException("Ashby payload missing jobs array");
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
            throw new AtsFetchException("Unreadable JSON from Ashby", e);
        }
    }

    private static RawAtsJob mapJob(CompanyBoard board, JsonNode job) {
        if (job == null || !job.isObject()) {
            return null;
        }
        JsonNode isListed = job.path("isListed");
        if (isListed.isBoolean() && !isListed.asBoolean()) {
            return null;
        }
        String id = AtsSupport.idText(job.path("id"));
        String title = AtsSupport.text(job.path("title"));
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("applyUrl")));
        if (applyUrl == null) {
            applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("jobUrl")));
        }
        if (id == null || title == null || applyUrl == null) {
            return null;
        }
        Instant publishedAt = AtsSupport.parseTimestamp(job.path("publishedAt"));
        String location = buildLocation(job);
        String workplaceType = AtsSupport.normalizeWorkplaceType(AtsSupport.text(job.path("workplaceType")));
        if (workplaceType == null && AtsSupport.boolFlag(job.path("isRemote"))) {
            workplaceType = "remote";
        }
        if (workplaceType == null) {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }
        String description = AtsSupport.text(job.path("descriptionPlain"));
        if (description == null) {
            description = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("descriptionHtml")));
        }
        String department = AtsSupport.text(job.path("department"));
        if (department == null) {
            department = AtsSupport.text(job.path("team"));
        }
        String compensation = extractCompensation(job.path("compensation"));
        return new RawAtsJob(
                id,
                AtsProviderType.ASHBY,
                board.slug(),
                board.name(),
                title,
                location,
                workplaceType,
                publishedAt,
                description == null ? "" : description,
                applyUrl,
                department,
                compensation,
                description != null && !description.isEmpty()
        );
    }

    private static String buildLocation(JsonNode job) {
        List<String> parts = new ArrayList<>();
        String primary = AtsSupport.text(job.path("location"));
        if (primary != null) {
            parts.add(primary);
        }
        JsonNode secondary = job.path("secondaryLocations");
        if (secondary.isArray()) {
            for (JsonNode entry : secondary) {
                String loc = AtsSupport.text(entry.path("location"));
                if (loc != null && parts.size() < 4) {
                    parts.add(loc);
                }
            }
        }
        return AtsSupport.joinUniqueParts(parts, " · ");
    }

    private static String extractCompensation(JsonNode compensation) {
        if (compensation == null || !compensation.isObject()) {
            return null;
        }
        String tierSummary = AtsSupport.text(compensation.path("compensationTierSummary"));
        if (tierSummary != null) {
            return tierSummary;
        }
        return AtsSupport.text(compensation.path("scrapeableCompensationSalarySummary"));
    }
}
