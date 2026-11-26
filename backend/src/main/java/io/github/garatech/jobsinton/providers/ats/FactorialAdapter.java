package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factorial HR public career portal adapter (`*.factorialhr.es` and `*.factorialhr.com`).
 * Extracts postings from JSON-LD (`JobPosting`) when embedded, and parses public `/job_posting/{id}`
 * cards with bounded detail page enrichment for Spain/EU eligible vacancies.
 */
public final class FactorialAdapter implements AtsProviderAdapter {
    private static final int DEFAULT_DETAIL_CAP = 12;
    private static final Pattern LD_JSON_SCRIPT = Pattern.compile(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_POSTED_JSON = Pattern.compile(
            "\"datePosted\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ANCHOR_JOB_POSTING = Pattern.compile(
            "<a\\b[^>]*\\bhref=[\"']([^\"']*?/job_posting/([^\"'#?]+))[\"'][^>]*>([\\s\\S]*?)</a>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern H3_HEADING = Pattern.compile(
            "<h3\\b[^>]*>([\\s\\S]*?)</h3>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TRAILING_NUMERIC_ID = Pattern.compile("(\\d+)/?$");

    private final AtsHttpClient http;
    private final int maxDetailFetchesPerBoard;
    private final JsonMapper json = JsonMapper.builder().build();

    public FactorialAdapter(AtsHttpClient http) {
        this(http, DEFAULT_DETAIL_CAP);
    }

    public FactorialAdapter(AtsHttpClient http, int maxDetailFetchesPerBoard) {
        this.http = http;
        this.maxDetailFetchesPerBoard = Math.max(0, maxDetailFetchesPerBoard);
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.FACTORIAL;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        List<String> urls = buildCandidateUrls(board);
        var reply = http.get(URI.create(urls.get(0)));
        String usedUrl = urls.get(0);
        if (reply.status() != 200) {
            boolean recovered = false;
            for (int i = 1; i < urls.size(); i++) {
                var fallback = http.get(URI.create(urls.get(i)));
                if (fallback.status() == 200) {
                    reply = fallback;
                    usedUrl = urls.get(i);
                    recovered = true;
                    break;
                }
            }
            if (!recovered) {
                throw new AtsFetchException("HTTP " + reply.status() + " from Factorial HR");
            }
        }
        String html = new String(reply.body(), StandardCharsets.UTF_8);
        String baseOrigin = originOf(usedUrl);
        return parsePortalHtml(board, html, baseOrigin);
    }

    private static List<String> buildCandidateUrls(CompanyBoard board) {
        Set<String> urls = new LinkedHashSet<>();
        if (board.apiEndpoint() != null && AtsSupport.validHttpsUrl(board.apiEndpoint()) != null) {
            urls.add(board.apiEndpoint());
        }
        if (board.fallbackEndpoint() != null && AtsSupport.validHttpsUrl(board.fallbackEndpoint()) != null) {
            urls.add(board.fallbackEndpoint());
        }
        if (board.host() != null && !board.host().isBlank()) {
            String host = board.host().strip();
            urls.add("https://" + host + "/");
            urls.add("https://" + host + "/jobs");
        }
        String slug = AtsSupport.encodePathSegment(board.slug());
        urls.add("https://" + slug + ".factorialhr.es/");
        urls.add("https://" + slug + ".factorialhr.com/");
        urls.add("https://" + slug + ".factorialhr.es/jobs");
        urls.add("https://" + slug + ".factorialhr.com/jobs");
        return List.copyOf(urls);
    }

    private static String originOf(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            return "https://" + uri.getHost();
        } catch (IllegalArgumentException e) {
            return rawUrl;
        }
    }

    private List<RawAtsJob> parsePortalHtml(CompanyBoard board, String html, String baseOrigin) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Map<String, RawAtsJob> jobsById = new LinkedHashMap<>();

        // 1. Check JSON-LD JobPosting blocks if present.
        Matcher ldMatcher = LD_JSON_SCRIPT.matcher(html);
        while (ldMatcher.find()) {
            String payload = ldMatcher.group(1).strip();
            parseJsonLdBlock(board, payload, baseOrigin, jobsById);
        }

        // 2. Scan DOM anchors matching /job_posting/{slug_and_id} (ignoring spontaneous /apply links).
        List<SectionHeader> headers = findSectionHeaders(html);
        Matcher anchorMatcher = ANCHOR_JOB_POSTING.matcher(html);
        while (anchorMatcher.find()) {
            String rawHref = anchorMatcher.group(1).strip();
            String pathTail = anchorMatcher.group(2).strip();
            if (rawHref.endsWith("/apply") || pathTail.endsWith("/apply")) {
                continue;
            }
            String applyUrl = resolveHref(baseOrigin, rawHref);
            if (applyUrl == null) {
                continue;
            }
            String sourceId = extractPostingId(pathTail);
            if (jobsById.containsKey(sourceId)) {
                continue;
            }

            String innerHtml = anchorMatcher.group(3);
            int anchorStart = anchorMatcher.start();
            int contextStart = Math.max(0, anchorStart - 450);
            int contextEnd = Math.min(html.length(), anchorMatcher.end() + 450);
            String surrounding = html.substring(contextStart, contextEnd);

            String title = extractTitle(innerHtml, surrounding);
            if (title == null || title.isBlank()) {
                continue;
            }

            String sectionHeading = nearestHeadingBefore(headers, anchorStart);
            String location = extractLocation(sectionHeading, innerHtml, surrounding, board);
            String workplaceType = inferFactorialWorkplace(innerHtml + " " + surrounding + " " + (location == null ? "" : location));
            String department = sectionHeading != null && !AtsSupport.isSpainOrEuLocation(sectionHeading) ? sectionHeading : null;

            jobsById.put(sourceId, new RawAtsJob(
                    sourceId,
                    AtsProviderType.FACTORIAL,
                    board.slug(),
                    board.name(),
                    title,
                    location,
                    workplaceType,
                    null,
                    "",
                    applyUrl,
                    department,
                    null,
                    false
            ));
        }

        // 3. Prioritize tech & junior/entry Spain/EU postings before spending bounded detail fetches
        // so top-of-page Sales/HR/Marketing cards never starve engineering or junior vacancies.
        List<RawAtsJob> list = new ArrayList<>(jobsById.values());
        List<Integer> candidateIndices = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            RawAtsJob candidate = list.get(i);
            if (!candidate.hasFullDescription() && isEligibleForDetailFetch(candidate)) {
                candidateIndices.add(i);
            }
        }
        candidateIndices.sort(java.util.Comparator
                .comparingInt((Integer idx) -> factorialPriorityScore(list.get(idx))).reversed()
                .thenComparingInt(Integer::intValue));

        int limit = Math.min(maxDetailFetchesPerBoard, candidateIndices.size());
        for (int k = 0; k < limit; k++) {
            int idx = candidateIndices.get(k);
            RawAtsJob candidate = list.get(idx);
            try {
                var detailReply = http.get(URI.create(candidate.applyUrl()));
                if (detailReply.status() == 200) {
                    String detailHtml = new String(detailReply.body(), StandardCharsets.UTF_8);
                    String detailDesc = extractDetailDescription(detailHtml);
                    Instant resolvedPublished = candidate.publishedAt();
                    if (resolvedPublished == null) {
                        Matcher dm = DATE_POSTED_JSON.matcher(detailHtml);
                        if (dm.find()) {
                            resolvedPublished = AtsSupport.parseTimestampString(dm.group(1));
                        }
                    }
                    if (!detailDesc.isEmpty() || resolvedPublished != null) {
                        list.set(idx, new RawAtsJob(
                                candidate.sourceId(),
                                candidate.provider(),
                                candidate.companySlug(),
                                candidate.companyName(),
                                candidate.title(),
                                candidate.location(),
                                candidate.workplaceType() != null ? candidate.workplaceType() : inferFactorialWorkplace(detailDesc),
                                resolvedPublished,
                                detailDesc,
                                candidate.applyUrl(),
                                candidate.department(),
                                candidate.compensation(),
                                !detailDesc.isEmpty()
                        ));
                    }
                }
            } catch (Exception ignored) {
                // Keep card metadata if individual detail page request fails.
            }
        }
        return list;
    }

    private static int factorialPriorityScore(RawAtsJob job) {
        int score = 0;
        String text = ((job.title() == null ? "" : job.title()) + " "
                + (job.department() == null ? "" : job.department())).toLowerCase(Locale.ROOT);
        if (text.contains("junior") || text.contains("jr") || text.contains("beca")
                || text.contains("prácticas") || text.contains("practicas") || text.contains("trainee")
                || text.contains("intern") || text.contains("graduate") || text.contains("sin experiencia")
                || text.contains("novel") || text.contains("working student")) {
            score += 60;
        }
        if (text.contains("developer") || text.contains("engineer") || text.contains("desarrollador")
                || text.contains("programador") || text.contains("ingeniero") || text.contains("software")
                || text.contains("full stack") || text.contains("full-stack") || text.contains("fullstack")
                || text.contains("backend") || text.contains("frontend") || text.contains("web")
                || text.contains("java") || text.contains("python") || text.contains("react")
                || text.contains("angular") || text.contains("node") || text.contains("typescript")
                || text.contains("javascript") || text.contains("php") || text.contains(".net")
                || text.contains("data") || text.contains("analista") || text.contains("ai")
                || text.contains("cloud") || text.contains("devops") || text.contains("sistemas")
                || text.contains("qa") || text.contains("cyber") || text.contains("ciberseguridad")
                || text.contains("sap") || text.contains("erp")) {
            score += 45;
        }
        if (job.location() != null && AtsSupport.isSpainOrEuLocation(job.location())) {
            score += 25;
        }
        return score;
    }

    private void parseJsonLdBlock(CompanyBoard board, String jsonText, String baseOrigin, Map<String, RawAtsJob> target) {
        if (jsonText.isEmpty()) {
            return;
        }
        try {
            JsonNode root = json.readTree(jsonText);
            if (root == null) {
                return;
            }
            if (root.isArray()) {
                for (JsonNode item : root) {
                    parseJsonLdObject(board, item, baseOrigin, target);
                }
            } else if (root.isObject()) {
                if (root.path("@graph").isArray()) {
                    for (JsonNode item : root.path("@graph")) {
                        parseJsonLdObject(board, item, baseOrigin, target);
                    }
                } else {
                    parseJsonLdObject(board, root, baseOrigin, target);
                }
            }
        } catch (Exception ignored) {
            // Ignore non-JobPosting or malformed JSON-LD blocks.
        }
    }

    private static void parseJsonLdObject(CompanyBoard board, JsonNode node, String baseOrigin, Map<String, RawAtsJob> target) {
        if (node == null || !node.isObject()) {
            return;
        }
        String type = AtsSupport.text(node.path("@type"));
        if (type == null || !type.toLowerCase(Locale.ROOT).contains("jobposting")) {
            return;
        }
        String title = AtsSupport.text(node.path("title"));
        String rawUrl = AtsSupport.text(node.path("url"));
        if (rawUrl == null) {
            rawUrl = AtsSupport.text(node.path("sameAs"));
        }
        String applyUrl = resolveHref(baseOrigin, rawUrl);
        if (title == null || applyUrl == null) {
            return;
        }
        String id = AtsSupport.idText(node.path("identifier").path("value"));
        if (id == null) {
            id = AtsSupport.idText(node.path("identifier"));
        }
        if (id == null) {
            id = extractPostingId(applyUrl);
        }
        Instant publishedAt = AtsSupport.parseTimestampString(AtsSupport.text(node.path("datePosted")));
        String description = HtmlTextCleaner.toPlainText(AtsSupport.text(node.path("description")));
        String location = extractJsonLdLocation(node.path("jobLocation"));
        if (location == null) {
            location = board.locationHint();
        }
        String jobLocType = AtsSupport.text(node.path("jobLocationType"));
        String workplaceType = (jobLocType != null && jobLocType.toLowerCase(Locale.ROOT).contains("telecommute"))
                ? "remote"
                : inferFactorialWorkplace((location == null ? "" : location) + " " + description);

        target.put(id, new RawAtsJob(
                id,
                AtsProviderType.FACTORIAL,
                board.slug(),
                board.name(),
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                applyUrl,
                null,
                null,
                !description.isEmpty()
        ));
    }

    private static String extractJsonLdLocation(JsonNode jobLocation) {
        if (jobLocation == null || jobLocation.isMissingNode() || jobLocation.isNull()) {
            return null;
        }
        JsonNode locNode = jobLocation.isArray() && !jobLocation.isEmpty() ? jobLocation.get(0) : jobLocation;
        JsonNode address = locNode.path("address");
        String city = AtsSupport.text(address.path("addressLocality"));
        String region = AtsSupport.text(address.path("addressRegion"));
        String country = AtsSupport.countryDisplayName(AtsSupport.text(address.path("addressCountry")));
        List<String> parts = new ArrayList<>();
        if (city != null) parts.add(city);
        else if (region != null) parts.add(region);
        if (country != null) parts.add(country);
        return AtsSupport.joinUniqueParts(parts, ", ");
    }

    private record SectionHeader(int position, String text) {}

    private static List<SectionHeader> findSectionHeaders(String html) {
        List<SectionHeader> list = new ArrayList<>();
        Matcher m = H3_HEADING.matcher(html);
        while (m.find()) {
            String text = HtmlTextCleaner.toPlainText(m.group(1));
            if (!text.isEmpty() && text.length() <= 100) {
                list.add(new SectionHeader(m.start(), text));
            }
        }
        return list;
    }

    private static String nearestHeadingBefore(List<SectionHeader> headers, int index) {
        String nearest = null;
        for (SectionHeader header : headers) {
            if (header.position() < index) {
                nearest = header.text();
            } else {
                break;
            }
        }
        return nearest;
    }

    private static String resolveHref(String baseOrigin, String rawHref) {
        if (rawHref == null || rawHref.isBlank()) {
            return null;
        }
        String trimmed = rawHref.strip();
        if (trimmed.startsWith("https://")) {
            return AtsSupport.validHttpsUrl(trimmed);
        }
        if (trimmed.startsWith("//")) {
            return AtsSupport.validHttpsUrl("https:" + trimmed);
        }
        if (trimmed.startsWith("/")) {
            return AtsSupport.validHttpsUrl(baseOrigin + trimmed);
        }
        return AtsSupport.validHttpsUrl(baseOrigin + "/" + trimmed);
    }

    private static String extractPostingId(String pathTail) {
        Matcher m = TRAILING_NUMERIC_ID.matcher(pathTail);
        if (m.find()) {
            return m.group(1);
        }
        String cleaned = pathTail.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return cleaned.isEmpty() ? "job" : cleaned;
    }

    private static String extractTitle(String innerHtml, String surrounding) {
        String text = HtmlTextCleaner.toPlainText(innerHtml);
        if (!text.isEmpty()) {
            String firstLine = text.split("\\n")[0].strip();
            if (!isGenericActionLabel(firstLine)) {
                return firstLine;
            }
        }
        Matcher h4 = Pattern.compile("<h[2-5][^>]*>([\\s\\S]*?)</h[2-5]>", Pattern.CASE_INSENSITIVE).matcher(surrounding);
        while (h4.find()) {
            String candidate = HtmlTextCleaner.toPlainText(h4.group(1));
            if (!candidate.isEmpty() && !isGenericActionLabel(candidate)) {
                return candidate;
            }
        }
        return text.isEmpty() ? null : text;
    }

    private static boolean isGenericActionLabel(String text) {
        String lower = text.toLowerCase(Locale.ROOT).strip();
        return lower.equals("ver oferta") || lower.equals("view job") || lower.equals("apply")
                || lower.equals("inscribirme") || lower.equals("ver más") || lower.equals("read more");
    }

    private static String extractLocation(String sectionHeading, String innerHtml, String surrounding, CompanyBoard board) {
        String combined = HtmlTextCleaner.toPlainText(innerHtml + "\n" + surrounding);
        List<String> cities = List.of(
                "Madrid", "Barcelona", "Valencia", "Málaga", "Sevilla", "Bilbao", "Zaragoza",
                "Alicante", "A Coruña", "Valladolid", "San Sebastián", "Murcia", "Castellón", "Cartagena"
        );
        List<String> detected = new ArrayList<>();
        for (String city : cities) {
            if (combined.toLowerCase(Locale.ROOT).contains(city.toLowerCase(Locale.ROOT))) {
                detected.add(city);
            }
        }
        if (sectionHeading != null && AtsSupport.isSpainOrEuLocation(sectionHeading)) {
            detected.add(sectionHeading);
        }
        if (combined.toLowerCase(Locale.ROOT).contains("remote") || combined.toLowerCase(Locale.ROOT).contains("remoto")
                || combined.toLowerCase(Locale.ROOT).contains("teletrabajo")) {
            detected.add("Remote");
        }
        if (!detected.isEmpty()) {
            return AtsSupport.joinUniqueParts(detected, ", ");
        }
        return board.locationHint() != null ? board.locationHint() : "Spain";
    }

    private static String inferFactorialWorkplace(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("híbrid") || lower.contains("hibrid") || lower.contains("hybrid")) {
            return "hybrid";
        }
        if (lower.contains("remoto") || lower.contains("remote") || lower.contains("teletrabajo")) {
            return "remote";
        }
        if (lower.contains("presencial") || lower.contains("on-site") || lower.contains("onsite")) {
            return "onsite";
        }
        return null;
    }

    private static boolean isEligibleForDetailFetch(RawAtsJob job) {
        if (job.location() == null) {
            return true;
        }
        return AtsSupport.isSpainOrEuLocation(job.location())
                || "remote".equals(job.workplaceType())
                || !AtsSupport.isNonEuFakeRemote(job.location());
    }

    private static String extractDetailDescription(String detailHtml) {
        if (detailHtml == null || detailHtml.isBlank()) {
            return "";
        }
        String strippedScripts = detailHtml
                .replaceAll("(?is)<script\\b[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<nav\\b[^>]*>.*?</nav>", " ")
                .replaceAll("(?is)<footer\\b[^>]*>.*?</footer>", " ");
        Matcher mainMatcher = Pattern.compile("(?is)<main\\b[^>]*>([\\s\\S]*?)</main>").matcher(strippedScripts);
        String targetHtml = mainMatcher.find() ? mainMatcher.group(1) : strippedScripts;
        return HtmlTextCleaner.toPlainText(targetHtml);
    }
}
