package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lever public Postings API adapter supporting Global and EU instances with automatic 404 EU fallback.
 * Global: GET https://api.lever.co/v0/postings/{company_slug}?mode=json
 * EU:     GET https://api.eu.lever.co/v0/postings/{company_slug}?mode=json
 */
public final class LeverAdapter implements AtsProviderAdapter {
    private static final String GLOBAL_BASE = "https://api.lever.co/v0/postings/";
    private static final String EU_BASE = "https://api.eu.lever.co/v0/postings/";

    private final AtsHttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public LeverAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.LEVER;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        String slug = AtsSupport.encodePathSegment(board.slug());
        boolean preferEu = board.isEuRegion();
        URI primaryUri = URI.create((preferEu ? EU_BASE : GLOBAL_BASE) + slug + "?mode=json");
        var reply = http.get(primaryUri);
        if (reply.status() == 404) {
            // Auto-fallback to the alternate Lever cluster (Global <-> EU) when primary returns 404.
            URI fallbackUri = URI.create((preferEu ? GLOBAL_BASE : EU_BASE) + slug + "?mode=json");
            reply = http.get(fallbackUri);
        }
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from Lever");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            if (root == null || !root.isArray()) {
                throw new AtsFetchException("Lever payload was not a JSON array");
            }
            List<RawAtsJob> jobs = new ArrayList<>(root.size());
            for (JsonNode posting : root) {
                RawAtsJob parsed = mapPosting(board, posting);
                if (parsed != null) {
                    jobs.add(parsed);
                }
            }
            return jobs;
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable JSON from Lever", e);
        }
    }

    private static RawAtsJob mapPosting(CompanyBoard board, JsonNode job) {
        if (job == null || !job.isObject()) {
            return null;
        }
        String id = AtsSupport.idText(job.path("id"));
        String title = AtsSupport.text(job.path("text"));
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("applyUrl")));
        if (applyUrl == null) {
            applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("hostedUrl")));
        }
        if (id == null || title == null || applyUrl == null) {
            return null;
        }
        Instant publishedAt = AtsSupport.parseTimestamp(job.path("createdAt"));
        JsonNode categories = job.path("categories");
        String location = buildLocation(categories);
        String workplaceType = AtsSupport.normalizeWorkplaceType(AtsSupport.text(job.path("workplaceType")));
        if (workplaceType == null) {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }
        String department = AtsSupport.text(categories.path("department"));
        if (department == null) {
            department = AtsSupport.text(categories.path("team"));
        }
        String description = buildDescription(job);
        return new RawAtsJob(
                id,
                AtsProviderType.LEVER,
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

    private static String buildLocation(JsonNode categories) {
        List<String> parts = new ArrayList<>();
        String primary = AtsSupport.text(categories.path("location"));
        if (primary != null) {
            parts.add(primary);
        }
        JsonNode allLocations = categories.path("allLocations");
        if (allLocations.isArray()) {
            for (JsonNode loc : allLocations) {
                String value = AtsSupport.text(loc);
                if (value != null && parts.size() < 4) {
                    parts.add(value);
                }
            }
        }
        return AtsSupport.joinUniqueParts(parts, " · ");
    }

    private static String buildDescription(JsonNode job) {
        List<String> sections = new ArrayList<>();
        String plain = AtsSupport.text(job.path("descriptionPlain"));
        if (plain == null) {
            plain = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("description")));
        }
        if (plain != null && !plain.isBlank()) {
            sections.add(plain.strip());
        }
        JsonNode lists = job.path("lists");
        if (lists.isArray()) {
            for (JsonNode list : lists) {
                String heading = AtsSupport.text(list.path("text"));
                String content = HtmlTextCleaner.toPlainText(AtsSupport.text(list.path("content")));
                if (heading != null && !content.isEmpty()) {
                    sections.add(heading + "\n" + content);
                } else if (!content.isEmpty()) {
                    sections.add(content);
                }
            }
        }
        String additional = AtsSupport.text(job.path("additionalPlain"));
        if (additional == null) {
            additional = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("additional")));
        }
        if (additional != null && !additional.isBlank()) {
            sections.add(additional.strip());
        }
        return String.join("\n\n", sections);
    }
}
