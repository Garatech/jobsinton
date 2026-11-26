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
    private static final int MAX_DETAIL_CACHE_ENTRIES = 4_000;

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

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 3;
    private static final int MAX_SPAIN_PAGES = 2;

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        String slug = AtsSupport.encodePathSegment(board.slug());
        URI listUri = URI.create(BASE_URL + slug + "/postings?limit=" + PAGE_SIZE);
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
            Map<String, RawAtsJob> listedById = new java.util.LinkedHashMap<>();
            for (JsonNode item : content) {
                RawAtsJob job = mapListItem(board, item);
                if (job != null) {
                    listedById.putIfAbsent(job.sourceId(), job);
                }
            }
            boolean firstPageFull = content.size() == PAGE_SIZE;
            int lastPageCount = content.size();
            for (int page = 1; page < MAX_PAGES && lastPageCount == PAGE_SIZE; page++) {
                int offset = page * PAGE_SIZE;
                URI nextUri = URI.create(BASE_URL + slug + "/postings?limit=" + PAGE_SIZE + "&offset=" + offset);
                var nextReply = http.get(nextUri);
                if (nextReply.status() != 200 || nextReply.body() == null || nextReply.body().length == 0) {
                    break;
                }
                JsonNode nextRoot = json.readTree(nextReply.body());
                JsonNode nextContent = nextRoot == null ? null : nextRoot.path("content");
                if (nextContent == null || !nextContent.isArray() || nextContent.isEmpty()) {
                    break;
                }
                for (JsonNode item : nextContent) {
                    RawAtsJob job = mapListItem(board, item);
                    if (job != null) {
                        listedById.putIfAbsent(job.sourceId(), job);
                    }
                }
                lastPageCount = nextContent.size();
            }

            // Large global boards (e.g. Inetum, Sopra Steria, Bosch, SAP, Devoteam, Delivery Hero)
            // often have 500–5,000+ worldwide postings where Spanish roles sit past page 3.
            // Run a targeted country=es pass whenever page 1 hits the 100-item cap so zero Spanish roles are missed.
            if (firstPageFull) {
                int spainPageCount = PAGE_SIZE;
                for (int spainPage = 0; spainPage < MAX_SPAIN_PAGES && spainPageCount == PAGE_SIZE; spainPage++) {
                    int offset = spainPage * PAGE_SIZE;
                    String suffix = offset == 0
                            ? "/postings?limit=" + PAGE_SIZE + "&country=es"
                            : "/postings?limit=" + PAGE_SIZE + "&country=es&offset=" + offset;
                    URI spainUri = URI.create(BASE_URL + slug + suffix);
                    try {
                        var spainReply = http.get(spainUri);
                        if (spainReply.status() != 200 || spainReply.body() == null || spainReply.body().length == 0) {
                            break;
                        }
                        JsonNode spainRoot = json.readTree(spainReply.body());
                        JsonNode spainContent = spainRoot == null ? null : spainRoot.path("content");
                        if (spainContent == null || !spainContent.isArray() || spainContent.isEmpty()) {
                            break;
                        }
                        for (JsonNode item : spainContent) {
                            RawAtsJob job = mapListItem(board, item);
                            if (job != null) {
                                listedById.putIfAbsent(job.sourceId(), job);
                            }
                        }
                        spainPageCount = spainContent.size();
                    } catch (AtsFetchException | JacksonException ignored) {
                        break;
                    }
                }
            }

            return enrichTopCandidates(board, new ArrayList<>(listedById.values()));
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

        String listMetadata = formatListMetadata(item);

        // Apply already-cached detail immediately without extra upstream calls.
        DetailPayload cached = detailCache.get(board.slug() + ":" + id);
        boolean hasFullDesc = cached != null && !cached.description().isEmpty();
        String description;
        if (hasFullDesc) {
            description = listMetadata.isEmpty() || cached.description().contains(listMetadata)
                    ? cached.description()
                    : cached.description() + "\n\n" + listMetadata;
        } else {
            description = listMetadata;
        }
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
                hasFullDesc
        );
    }

    private static String formatListMetadata(JsonNode item) {
        List<String> meta = new ArrayList<>(2);
        JsonNode expNode = item.path("experienceLevel");
        String expLabel = firstNonNull(AtsSupport.text(expNode.path("label")), AtsSupport.text(expNode.path("id")));
        if (expLabel != null) {
            meta.add("Experience Level: " + expLabel.replace('_', ' '));
        }
        JsonNode empNode = item.path("typeOfEmployment");
        String empLabel = firstNonNull(AtsSupport.text(empNode.path("label")), AtsSupport.text(empNode.path("id")));
        if (empLabel != null) {
            meta.add("Employment: " + empLabel.replace('_', ' '));
        }
        return String.join(" · ", meta);
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
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
                String mergedDescription = detail.description();
                if (!candidate.description().isEmpty() && !mergedDescription.contains(candidate.description())) {
                    mergedDescription = mergedDescription.isEmpty()
                            ? candidate.description()
                            : mergedDescription + "\n\n" + candidate.description();
                }
                enriched.set(idx, new RawAtsJob(
                        candidate.sourceId(),
                        candidate.provider(),
                        candidate.companySlug(),
                        candidate.companyName(),
                        candidate.title(),
                        candidate.location(),
                        candidate.workplaceType(),
                        candidate.publishedAt(),
                        mergedDescription,
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
        String loc = job.location() == null ? "" : job.location();
        String lowerLoc = loc.toLowerCase(Locale.ROOT);
        if (AtsSupport.isSpainOrEuLocation(loc)) {
            score += 100;
            if (lowerLoc.contains("madrid") || lowerLoc.contains("barcelona")
                    || lowerLoc.contains("spain") || lowerLoc.contains("españa")) {
                score += 25;
            }
        } else if (AtsSupport.isNonEuFakeRemote(loc)) {
            score -= 80;
        } else if ("remote".equals(job.workplaceType()) || lowerLoc.contains("remote")
                || lowerLoc.contains("europe") || lowerLoc.contains("emea")) {
            score += 45;
        }
        String title = job.title().toLowerCase(Locale.ROOT);
        String meta = (job.description() == null ? "" : job.description()).toLowerCase(Locale.ROOT);
        if (title.contains("sap") || title.contains("abap") || title.contains("s/4hana")
                || title.contains("java") || title.contains("spring") || title.contains("full stack")
                || title.contains("fullstack") || title.contains("angular") || title.contains("react")
                || title.contains("node") || title.contains("typescript") || title.contains("software")
                || title.contains("developer") || title.contains("engineer") || title.contains("backend")
                || title.contains("frontend") || title.contains("desarrollador") || title.contains("programador")
                || title.contains("ingeniero") || title.contains("consultor") || title.contains("python")
                || title.contains("data") || title.contains("analyst") || title.contains("analista")
                || title.contains("cloud") || title.contains("devops") || title.contains("sistemas")
                || title.contains("ai") || title.contains("machine learning")
                || title.contains("cyber") || title.contains("security") || title.contains("ciberseguridad")
                || title.contains("qa") || title.contains("web")) {
            score += 35;
        }
        if (title.contains("junior") || title.contains("jr") || title.contains("intern")
                || title.contains("beca") || title.contains("prácticas") || title.contains("practicas")
                || title.contains("trainee") || title.contains("graduate") || title.contains("entry")
                || title.contains("associate") || title.contains("academy")
                || meta.contains("entry level") || meta.contains("internship") || meta.contains("intern")
                || meta.contains("associate")) {
            score += 55;
        }
        return score;
    }
}
