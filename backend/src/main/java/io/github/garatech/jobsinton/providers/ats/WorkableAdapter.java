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
 * Workable public Widget API adapter with graceful fallback to the v3 POST jobs endpoint.
 * Primary:  GET  https://apply.workable.com/api/v1/widget/accounts/{company_slug}?details=true
 * Fallback: POST https://apply.workable.com/api/v3/accounts/{company_slug}/jobs
 */
public final class WorkableAdapter implements AtsProviderAdapter {
    private static final String V1_WIDGET_BASE = "https://apply.workable.com/api/v1/widget/accounts/";
    private static final String V3_JOBS_BASE = "https://apply.workable.com/api/v3/accounts/";
    private static final String PUBLIC_JOB_BASE = "https://apply.workable.com/";
    private static final String V3_EMPTY_QUERY = "{\"query\":\"\",\"location\":[],\"department\":[],\"worktype\":[],\"remote\":[]}";

    private final AtsHttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public WorkableAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.WORKABLE;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        String slug = AtsSupport.encodePathSegment(board.slug());
        URI widgetUri = URI.create(V1_WIDGET_BASE + slug + "?details=true");
        var reply = http.get(widgetUri);
        if (reply.status() == 200) {
            try {
                JsonNode root = json.readTree(reply.body());
                JsonNode jobsNode = root == null ? null : root.path("jobs");
                if (jobsNode != null && jobsNode.isArray()) {
                    return parseArray(board, jobsNode, false);
                }
            } catch (JacksonException ignored) {
                // Fall through to v3 POST endpoint.
            }
        }
        return fetchV3Fallback(board, slug, reply.status());
    }

    private List<RawAtsJob> fetchV3Fallback(CompanyBoard board, String encodedSlug, int v1Status) {
        URI v3Uri = URI.create(V3_JOBS_BASE + encodedSlug + "/jobs");
        var reply = http.postJson(v3Uri, V3_EMPTY_QUERY);
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + v1Status + " (v1) / HTTP " + reply.status() + " (v3) from Workable");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            JsonNode results = root == null ? null : root.path("results");
            if (results == null || !results.isArray()) {
                throw new AtsFetchException("Workable v3 payload missing results array");
            }
            return parseArray(board, results, true);
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable JSON from Workable v3", e);
        }
    }

    private List<RawAtsJob> parseArray(CompanyBoard board, JsonNode array, boolean isV3) {
        List<RawAtsJob> jobs = new ArrayList<>(array.size());
        for (JsonNode item : array) {
            RawAtsJob parsed = mapJob(board, item, isV3);
            if (parsed != null) {
                jobs.add(parsed);
            }
        }
        return jobs;
    }

    private static RawAtsJob mapJob(CompanyBoard board, JsonNode job, boolean isV3) {
        if (job == null || !job.isObject()) {
            return null;
        }
        String shortcode = AtsSupport.idText(job.path("shortcode"));
        if (shortcode == null) {
            shortcode = AtsSupport.idText(job.path("id"));
        }
        String title = AtsSupport.text(job.path("title"));
        if (shortcode == null || title == null) {
            return null;
        }
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("url")));
        if (applyUrl == null) {
            applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(job.path("application_url")));
        }
        if (applyUrl == null) {
            applyUrl = PUBLIC_JOB_BASE + AtsSupport.encodePathSegment(board.slug())
                    + "/j/" + AtsSupport.encodePathSegment(shortcode) + "/";
        }
        Instant publishedAt = AtsSupport.parseTimestamp(job.path(isV3 ? "published" : "published_on"));
        if (publishedAt == null) {
            publishedAt = AtsSupport.parseTimestamp(job.path("published_on"));
        }
        if (publishedAt == null) {
            publishedAt = AtsSupport.parseTimestamp(job.path("published"));
        }
        boolean remoteFlag = AtsSupport.boolFlag(job.path("telecommuting")) || AtsSupport.boolFlag(job.path("remote"));
        String workplaceType = AtsSupport.normalizeWorkplaceType(AtsSupport.text(job.path("workplace")));
        if (workplaceType == null && remoteFlag) {
            workplaceType = "remote";
        }
        String location = buildLocation(job, remoteFlag);
        if (workplaceType == null) {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }
        String description = buildDescription(job);
        String department = extractDepartment(job.path("department"));
        return new RawAtsJob(
                shortcode,
                AtsProviderType.WORKABLE,
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

    private static String buildLocation(JsonNode job, boolean remoteFlag) {
        List<String> parts = new ArrayList<>();
        String city = AtsSupport.text(job.path("city"));
        String country = AtsSupport.text(job.path("country"));
        if (city != null) {
            parts.add(city);
        }
        if (country != null) {
            parts.add(country);
        }
        if (parts.isEmpty() && job.path("location").isObject()) {
            String locCity = AtsSupport.text(job.path("location").path("city"));
            String locCountry = AtsSupport.text(job.path("location").path("country"));
            if (locCity != null) {
                parts.add(locCity);
            }
            if (locCountry != null) {
                parts.add(locCountry);
            }
        }
        if (parts.isEmpty() && job.path("locations").isArray()) {
            for (JsonNode loc : job.path("locations")) {
                String locCity = AtsSupport.text(loc.path("city"));
                String locCountry = AtsSupport.text(loc.path("country"));
                String combined = AtsSupport.joinUniqueParts(
                        locCity == null && locCountry == null ? List.of() : List.of(locCity == null ? "" : locCity, locCountry == null ? "" : locCountry),
                        ", ");
                if (combined != null && parts.size() < 3) {
                    parts.add(combined);
                }
            }
        }
        if (remoteFlag && parts.stream().noneMatch(p -> p.toLowerCase(Locale.ROOT).contains("remote"))) {
            parts.add("Remote");
        }
        return AtsSupport.joinUniqueParts(parts, ", ");
    }

    private static String buildDescription(JsonNode job) {
        List<String> sections = new ArrayList<>();
        String desc = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("description")));
        if (desc.isEmpty()) {
            desc = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("full_description")));
        }
        if (!desc.isEmpty()) {
            sections.add(desc);
        }
        String requirements = HtmlTextCleaner.toPlainText(AtsSupport.text(job.path("requirements")));
        if (!requirements.isEmpty() && !desc.contains(requirements)) {
            sections.add(requirements);
        }
        return String.join("\n\n", sections);
    }

    private static String extractDepartment(JsonNode department) {
        if (department == null || department.isMissingNode() || department.isNull()) {
            return null;
        }
        if (department.isString()) {
            return AtsSupport.text(department);
        }
        if (department.isArray() && !department.isEmpty()) {
            return AtsSupport.text(department.get(0));
        }
        return null;
    }
}
