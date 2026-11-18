package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmartRecruiters public Postings API adapter with prioritized bounded detail enrichment and on-demand hydration.
 * List:   GET https://api.smartrecruiters.com/v1/companies/{company_slug}/postings?limit=100
 * Detail: GET https://api.smartrecruiters.com/v1/companies/{company_slug}/postings/{posting_id}
 */
public final class SmartRecruitersAdapter implements AtsProviderAdapter {
    private static final String BASE_URL = "https://api.smartrecruiters.com/v1/companies/";
    private static final String PUBLIC_JOB_BASE = "https://jobs.smartrecruiters.com/";
    private static final int MAX_DETAIL_CACHE_ENTRIES = 500;

    private final AtsHttpClient http;
    private final int maxDetailFetchesPerBoard;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, DetailPayload> detailCache = new ConcurrentHashMap<>();

    private record DetailPayload(String description, String applyUrl) {}

    public SmartRecruitersAdapter(AtsHttpClient http, int maxDetailFetchesPerBoard) {
        this.http = http;
        this.maxDetailFetchesPerBoard = Math.max(0, maxDetailFetchesPerBoard);
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.SMARTRECRUITERS;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        String slug = AtsSupport.encodePathSegment(board.slug());
        URI listUri = URI.create(BASE_URL + slug + "/postings?limit=100");
        var reply = http.get(listUri);
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from SmartRecruiters");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            JsonNode content = root == null ? null : root.path("content");
            if (content == null || !content.isArray()) {
                throw new AtsFetchException("SmartRecruiters payload missing content array");
            }
            List<RawAtsJob> listed = new ArrayList<>(content.size());
            for (JsonNode item : content) {
                RawAtsJob job = mapListItem(board, item);
                if (job != null) {
                    listed.add(job);
                }
            }
            return enrichTopCandidates(board, listed);
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable JSON from SmartRecruiters", e);
        }
    }

    /** Hydrates a single SmartRecruiters posting on demand (cached in memory per posting). */
    public SmartRecruitersDetail fetchDetail(CompanyBoard board, String postingId) {
        String cacheKey = board.slug() + ":" + postingId;
        DetailPayload cached = detailCache.get(cacheKey);
        if (cached == null) {
            cached = requestDetail(board, postingId);
            if (detailCache.size() >= MAX_DETAIL_CACHE_ENTRIES) {
                detailCache.clear();
            }
            detailCache.put(cacheKey, cached);
        }
        String compositeId = AtsProviderType.SMARTRECRUITERS.id() + ":" + board.slug() + ":" + postingId;
        String desc = cached.description();
        return new SmartRecruitersDetail(
                compositeId,
                desc,
                HtmlTextCleaner.snippet(desc, 260),
                cached.applyUrl(),
                !desc.isEmpty()
        );
    }

    private RawAtsJob mapListItem(CompanyBoard board, JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String id = AtsSupport.idText(item.path("id"));
        String title = AtsSupport.text(item.path("name"));
        if (id == null || title == null) {
            return null;
        }
        Instant publishedAt = AtsSupport.parseTimestamp(item.path("releasedDate"));
        JsonNode locNode = item.path("location");
        boolean isRemote = AtsSupport.boolFlag(locNode.path("remote"));
        boolean isHybrid = AtsSupport.boolFlag(locNode.path("hybrid"));
        String location = formatLocation(locNode, isRemote);
        String workplaceType = isRemote ? "remote" : isHybrid ? "hybrid" : AtsSupport.inferWorkplaceFromLocation(location);
        String department = AtsSupport.text(item.path("department").path("label"));
        String defaultApplyUrl = PUBLIC_JOB_BASE
                + AtsSupport.encodePathSegment(board.slug()) + "/" + AtsSupport.encodePathSegment(id);

        // Apply already-cached detail immediately without extra upstream calls.
        DetailPayload cached = detailCache.get(board.slug() + ":" + id);
        String description = cached != null ? cached.description() : "";
        String applyUrl = cached != null && cached.applyUrl() != null ? cached.applyUrl() : defaultApplyUrl;

        return new RawAtsJob(
                id,
                AtsProviderType.SMARTRECRUITERS,
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

    /**
     * Enriches only a bounded slice of prioritized postings during feed refresh so startup never triggers
     * hundreds of detail calls. Remaining postings can still be hydrated on demand via fetchDetail().
     */
    private List<RawAtsJob> enrichTopCandidates(CompanyBoard board, List<RawAtsJob> listed) {
        if (maxDetailFetchesPerBoard <= 0 || listed.isEmpty()) {
            return listed;
        }
        List<Integer> indicesToEnrich = new ArrayList<>();
        for (int i = 0; i < listed.size(); i++) {
            if (!listed.get(i).hasFullDescription()) {
                indicesToEnrich.add(i);
            }
        }
        indicesToEnrich.sort(Comparator
                .comparingInt((Integer idx) -> priorityScore(listed.get(idx))).reversed()
                .thenComparing(idx -> listed.get(idx).publishedAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        List<RawAtsJob> enriched = new ArrayList<>(listed);
        int limit = Math.min(maxDetailFetchesPerBoard, indicesToEnrich.size());
        for (int i = 0; i < limit; i++) {
            int idx = indicesToEnrich.get(i);
            RawAtsJob candidate = enriched.get(idx);
            try {
                SmartRecruitersDetail detail = fetchDetail(board, candidate.sourceId());
                enriched.set(idx, new RawAtsJob(
                        candidate.sourceId(),
                        candidate.provider(),
                        candidate.companySlug(),
                        candidate.companyName(),
                        candidate.title(),
                        candidate.location(),
                        candidate.workplaceType(),
                        candidate.publishedAt(),
                        detail.description(),
                        detail.applyUrl() != null ? detail.applyUrl() : candidate.applyUrl(),
                        candidate.department(),
                        candidate.compensation(),
                        detail.hasFullDescription()
                ));
            } catch (AtsFetchException ignored) {
                // Keep list-level metadata intact if a single detail request fails.
            }
        }
        return enriched;
    }

    private DetailPayload requestDetail(CompanyBoard board, String postingId) {
        URI detailUri = URI.create(BASE_URL + AtsSupport.encodePathSegment(board.slug())
                + "/postings/" + AtsSupport.encodePathSegment(postingId));
        var reply = http.get(detailUri);
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from SmartRecruiters detail");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            JsonNode sections = root.path("jobAd").path("sections");
            List<String> parts = new ArrayList<>();
            appendSection(parts, sections.path("jobDescription"));
            appendSection(parts, sections.path("qualifications"));
            appendSection(parts, sections.path("additionalInformation"));
            if (parts.isEmpty()) {
                appendSection(parts, sections.path("companyDescription"));
            }
            String description = String.join("\n\n", parts);
            String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(root.path("applyUrl")));
            if (applyUrl == null) {
                applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(root.path("postingUrl")));
            }
            if (applyUrl == null) {
                applyUrl = PUBLIC_JOB_BASE
                        + AtsSupport.encodePathSegment(board.slug()) + "/" + AtsSupport.encodePathSegment(postingId);
            }
            return new DetailPayload(description, applyUrl);
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable SmartRecruiters detail JSON", e);
        }
    }

    private static void appendSection(List<String> parts, JsonNode section) {
        if (section == null || !section.isObject()) {
            return;
        }
        String text = HtmlTextCleaner.toPlainText(AtsSupport.text(section.path("text")));
        if (!text.isEmpty()) {
            parts.add(text);
        }
    }

    private static String formatLocation(JsonNode location, boolean isRemote) {
        if (location == null || !location.isObject()) {
            return isRemote ? "Remote" : null;
        }
        String city = AtsSupport.text(location.path("city"));
        String country = AtsSupport.countryDisplayName(AtsSupport.text(location.path("country")));
        List<String> parts = new ArrayList<>();
        if (city != null) {
            parts.add(city);
        }
        if (country != null) {
            parts.add(country);
        }
        if (parts.isEmpty()) {
            String full = AtsSupport.text(location.path("fullLocation"));
            if (full != null) {
                parts.add(full);
            }
        }
        if (isRemote && parts.stream().noneMatch(p -> p.toLowerCase(Locale.ROOT).contains("remote"))) {
            parts.add("Remote");
        }
        return AtsSupport.joinUniqueParts(parts, ", ");
    }

    private static int priorityScore(RawAtsJob job) {
        int score = 0;
        String loc = job.location() == null ? "" : job.location().toLowerCase(Locale.ROOT);
        if (loc.contains("madrid") || loc.contains("spain") || loc.contains("españa") || loc.contains("barcelona")) {
            score += 40;
        }
        if ("remote".equals(job.workplaceType()) || loc.contains("remote")) {
            score += 20;
        }
        String title = job.title().toLowerCase(Locale.ROOT);
        if (title.contains("sap") || title.contains("abap") || title.contains("java") || title.contains("full stack")
                || title.contains("fullstack") || title.contains("angular") || title.contains("react")
                || title.contains("node") || title.contains("software") || title.contains("developer")
                || title.contains("engineer") || title.contains("backend") || title.contains("frontend")) {
            score += 35;
        }
        return score;
    }
}
