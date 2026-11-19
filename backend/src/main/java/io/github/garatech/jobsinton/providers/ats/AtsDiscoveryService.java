package io.github.garatech.jobsinton.providers.ats;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "Blip" ATS Auto-Discovery Engine:
 * 1) Career URL Auto-Sniffer: detects any of the 9 ATS platforms directly from a URL or by scanning
 *    corporate "Work with us" page HTML (links, iframes, scripts, meta tags, or Teamtailor RSS probe).
 * 2) Batch Company Name / Slug Probe: normalizes company names into clean slugs and probes all 9 ATS
 *    platforms in parallel, persisting verified boards to {@code data/custom-companies.json} and
 *    merging their jobs into the live feed cache.
 */
public final class AtsDiscoveryService {
    private static final int MAX_PROBE_CANDIDATES = 20;
    private static final int MAX_PROBE_CONCURRENCY = 16;

    private static final List<SignatureRule> SIGNATURE_RULES = List.of(
            // Personio: https://{slug}.jobs.personio.de or .com
            new SignatureRule(
                    AtsProviderType.PERSONIO,
                    Pattern.compile("https?://([a-z0-9][a-z0-9-]{1,62})\\.jobs\\.personio\\.(com|de)(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> new DetectedCandidate(AtsProviderType.PERSONIO, m.group(1).toLowerCase(Locale.ROOT), "jobs.personio." + m.group(2).toLowerCase(Locale.ROOT))
            ),
            // Recruitee: https://{slug}.recruitee.com
            new SignatureRule(
                    AtsProviderType.RECRUITEE,
                    Pattern.compile("https?://([a-z0-9][a-z0-9-]{1,62})\\.recruitee\\.com(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> new DetectedCandidate(AtsProviderType.RECRUITEE, m.group(1).toLowerCase(Locale.ROOT), m.group(1).toLowerCase(Locale.ROOT) + ".recruitee.com")
            ),
            // Teamtailor subdomain: https://{slug}.teamtailor.com
            new SignatureRule(
                    AtsProviderType.TEAMTAILOR,
                    Pattern.compile("https?://([a-z0-9][a-z0-9-]{1,62})\\.teamtailor\\.com(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        String s = m.group(1).toLowerCase(Locale.ROOT);
                        if (s.equals("www") || s.equals("app") || s.equals("cdn") || s.equals("assets")) return null;
                        return new DetectedCandidate(AtsProviderType.TEAMTAILOR, s, s + ".teamtailor.com");
                    }
            ),
            // Factorial HR: https://{slug}.factorialhr.es or .com
            new SignatureRule(
                    AtsProviderType.FACTORIAL,
                    Pattern.compile("https?://([a-z0-9][a-z0-9-]{1,62})\\.factorialhr\\.(es|com)(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        String s = m.group(1).toLowerCase(Locale.ROOT);
                        if (s.equals("www") || s.equals("api") || s.equals("app")) return null;
                        String tld = m.group(2).toLowerCase(Locale.ROOT);
                        return new DetectedCandidate(AtsProviderType.FACTORIAL, s, s + ".factorialhr." + tld);
                    }
            ),
            // Greenhouse: boards.greenhouse.io/{slug}, job-boards.greenhouse.io/{slug}, boards-api(.eu).greenhouse.io/v1/boards/{slug}
            new SignatureRule(
                    AtsProviderType.GREENHOUSE,
                    Pattern.compile("https?://(?:job-)?boards(?:-api(?:\\.eu)?)?\\.greenhouse\\.io/(?:v1/boards/|embed/job_board\\?for=)?([a-z0-9][a-z0-9_-]{1,62})(?:[/\"'\\s?#&]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        String s = m.group(1);
                        if (s.equalsIgnoreCase("embed") || s.equalsIgnoreCase("v1")) return null;
                        return new DetectedCandidate(AtsProviderType.GREENHOUSE, s, "boards-api.greenhouse.io");
                    }
            ),
            // Lever: jobs(.eu).lever.co/{slug} or api(.eu).lever.co/v0/postings/{slug}
            new SignatureRule(
                    AtsProviderType.LEVER,
                    Pattern.compile("https?://(?:jobs|api)(\\.eu)?\\.lever\\.co/(?:v0/postings/)?([a-z0-9][a-z0-9_-]{1,62})(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        boolean eu = m.group(1) != null && !m.group(1).isBlank();
                        return new DetectedCandidate(AtsProviderType.LEVER, m.group(2).toLowerCase(Locale.ROOT), eu ? "api.eu.lever.co" : "api.lever.co");
                    }
            ),
            // SmartRecruiters: jobs.smartrecruiters.com/{Slug} or api.smartrecruiters.com/v1/companies/{Slug}
            new SignatureRule(
                    AtsProviderType.SMARTRECRUITERS,
                    Pattern.compile("https?://(?:jobs|api)\\.smartrecruiters\\.com/(?:v1/companies/)?([A-Za-z0-9][A-Za-z0-9_-]{1,62})(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> new DetectedCandidate(AtsProviderType.SMARTRECRUITERS, m.group(1), "api.smartrecruiters.com")
            ),
            // Workable: apply.workable.com/{slug}
            new SignatureRule(
                    AtsProviderType.WORKABLE,
                    Pattern.compile("https?://apply\\.workable\\.com/(?:api/v[13]/(?:widget/)?accounts/)?([a-z0-9][a-z0-9_-]{1,62})(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        String s = m.group(1).toLowerCase(Locale.ROOT);
                        if (s.equals("api") || s.equals("j")) return null;
                        return new DetectedCandidate(AtsProviderType.WORKABLE, s, "apply.workable.com");
                    }
            ),
            // Ashby: jobs.ashbyhq.com/{slug} or api.ashbyhq.com/posting-api/job-board/{slug}
            new SignatureRule(
                    AtsProviderType.ASHBY,
                    Pattern.compile("https?://(?:jobs|api)\\.ashbyhq\\.com/(?:posting-api/job-board/)?([a-z0-9][a-z0-9._-]{1,62})(?:[/\"'\\s?#]|$)", Pattern.CASE_INSENSITIVE),
                    m -> {
                        String s = m.group(1);
                        if (s.equalsIgnoreCase("posting-api")) return null;
                        return new DetectedCandidate(AtsProviderType.ASHBY, s, "api.ashbyhq.com");
                    }
            )
    );

    public record DiscoveredBoardResult(
            CompanyBoard board,
            int jobCount,
            boolean newlyAdded,
            List<AtsJob> sampleJobs
    ) {}

    public record DiscoveryResponse(
            int discoveredCount,
            List<DiscoveredBoardResult> discovered,
            String message
    ) {}

    private record DetectedCandidate(AtsProviderType provider, String slug, String host) {}

    @FunctionalInterface
    private interface CandidateExtractor {
        DetectedCandidate extract(Matcher matcher);
    }

    private record SignatureRule(AtsProviderType provider, Pattern pattern, CandidateExtractor extractor) {}

    private final AtsHttpClient http;
    private final AtsFeedService feedService;

    public AtsDiscoveryService(AtsHttpClient http, AtsFeedService feedService) {
        this.http = http;
        this.feedService = feedService;
    }

    /**
     * Sniffs a direct ATS URL, job posting link, or corporate "Work with us" page URL,
     * detects all referenced ATS boards, live-verifies them, persists new boards, and merges their jobs.
     */
    public DiscoveryResponse discoverFromUrl(String rawUrl) {
        String validUrl = normalizeInputUrl(rawUrl);
        if (validUrl == null) {
            throw new IllegalArgumentException("Enter a valid https:// career or job board URL.");
        }

        Set<DetectedCandidate> candidates = new LinkedHashSet<>(extractCandidatesFromText(validUrl));

        // If the URL is not a direct ATS URL, download its HTML and sniff embedded links/iframes/scripts.
        if (candidates.isEmpty()) {
            try {
                var reply = http.get(URI.create(validUrl));
                if (reply.status() == 200) {
                    String html = new String(reply.body(), StandardCharsets.UTF_8);
                    candidates.addAll(extractCandidatesFromText(html));
                    // Check if the corporate domain itself is a custom Teamtailor portal (e.g. empleo.civitatis.com).
                    URI uri = URI.create(validUrl);
                    if (uri.getHost() != null && (html.contains("teamtailor") || html.contains("/jobs.rss"))) {
                        candidates.add(new DetectedCandidate(AtsProviderType.TEAMTAILOR, uri.getHost(), uri.getHost()));
                    }
                }
            } catch (AtsFetchException e) {
                throw new AtsFetchException("Could not reach the provided career page URL: " + e.getMessage(), e);
            }
        }

        // Also try custom Teamtailor /jobs.rss on the provided host if no signature matched in HTML.
        if (candidates.isEmpty()) {
            URI uri = URI.create(validUrl);
            if (uri.getHost() != null) {
                candidates.add(new DetectedCandidate(AtsProviderType.TEAMTAILOR, uri.getHost(), uri.getHost()));
            }
        }

        List<DiscoveredBoardResult> verified = new ArrayList<>();
        for (DetectedCandidate candidate : candidates) {
            DiscoveredBoardResult result = verifyAndRegisterCandidate(candidate, null);
            if (result != null) {
                verified.add(result);
            }
        }

        String msg = verified.isEmpty()
                ? "No public ATS board with active postings was detected at that URL."
                : "Discovered " + verified.size() + " live ATS board(s).";
        return new DiscoveryResponse(verified.size(), verified, msg);
    }

    /**
     * Normalizes candidate company names or slugs and probes all 9 public ATS platforms in parallel.
     */
    public DiscoveryResponse probeCandidates(List<String> rawCandidates) {
        if (rawCandidates == null || rawCandidates.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one company name or slug to probe.");
        }

        Map<String, String> slugToDisplayName = new LinkedHashMap<>();
        for (String raw : rawCandidates) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String token : raw.split("[,\\n;]+")) {
                String trimmed = token.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String slug = normalizeSlug(trimmed);
                if (!slug.isEmpty() && slugToDisplayName.size() < MAX_PROBE_CANDIDATES) {
                    slugToDisplayName.putIfAbsent(slug, humanizeCompanyName(trimmed, slug));
                }
            }
        }

        if (slugToDisplayName.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one valid company name or slug.");
        }

        List<ProbeTask> tasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : slugToDisplayName.entrySet()) {
            String slug = entry.getKey();
            String displayName = entry.getValue();
            String srSlug = capitalizeSlug(displayName, slug);
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.PERSONIO, slug, "jobs.personio.com"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.RECRUITEE, slug, slug + ".recruitee.com"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.TEAMTAILOR, slug, slug + ".teamtailor.com"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.FACTORIAL, slug, slug + ".factorialhr.es"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.GREENHOUSE, slug, "boards-api.greenhouse.io"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.LEVER, slug, "api.eu.lever.co"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.WORKABLE, slug, "apply.workable.com"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.ASHBY, slug, "api.ashbyhq.com"), displayName));
            tasks.add(new ProbeTask(new DetectedCandidate(AtsProviderType.SMARTRECRUITERS, srSlug, "api.smartrecruiters.com"), displayName));
        }

        Semaphore permits = new Semaphore(MAX_PROBE_CONCURRENCY);
        List<DiscoveredBoardResult> discovered;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DiscoveredBoardResult>> futures = tasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(() -> {
                        boolean acquired = false;
                        try {
                            permits.acquire();
                            acquired = true;
                            return verifyAndRegisterCandidate(task.candidate(), task.displayName());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        } finally {
                            if (acquired) {
                                permits.release();
                            }
                        }
                    }, executor))
                    .toList();
            discovered = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> r != null && r.jobCount() > 0)
                    .toList();
        }

        String msg = discovered.isEmpty()
                ? "No active public ATS boards matched the probed company slugs."
                : "Found " + discovered.size() + " active ATS board(s) with live job postings.";
        return new DiscoveryResponse(discovered.size(), discovered, msg);
    }

    private record ProbeTask(DetectedCandidate candidate, String displayName) {}

    private DiscoveredBoardResult verifyAndRegisterCandidate(DetectedCandidate candidate, String preferredName) {
        AtsProviderAdapter adapter = feedService.adapterFor(candidate.provider());
        if (adapter == null) {
            return null;
        }
        String companyName = preferredName != null && !preferredName.isBlank()
                ? preferredName
                : humanizeCompanyName(candidate.slug(), candidate.slug());
        String region = (candidate.host() != null && candidate.host().contains("api.eu.lever.co")) ? "eu" : "global";
        CompanyBoard probeBoard = new CompanyBoard(
                candidate.slug(),
                companyName,
                candidate.provider().id(),
                region,
                "Spain / EU",
                candidate.host(),
                null,
                null,
                null,
                List.of("Spain", "Remote"),
                List.of(),
                "high"
        );

        long startNanos = System.nanoTime();
        try {
            List<RawAtsJob> jobs = adapter.fetch(probeBoard);
            if (jobs == null || jobs.isEmpty()) {
                return null;
            }
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            boolean newlyAdded = feedService.registry().registerCustomBoard(probeBoard);
            feedService.mergeDiscoveredBoard(probeBoard, jobs, elapsedMs);
            List<AtsJob> sampleJobs = jobs.stream()
                    .limit(5)
                    .map(j -> j.toApiJob(java.time.Instant.now()))
                    .toList();
            return new DiscoveredBoardResult(probeBoard, jobs.size(), newlyAdded, sampleJobs);
        } catch (Exception ignored) {
            return null;
        }
    }

    static List<DetectedCandidate> extractCandidatesFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<DetectedCandidate> found = new LinkedHashSet<>();
        for (SignatureRule rule : SIGNATURE_RULES) {
            Matcher matcher = rule.pattern().matcher(text);
            while (matcher.find()) {
                DetectedCandidate candidate = rule.extractor().extract(matcher);
                if (candidate != null) {
                    found.add(candidate);
                }
            }
        }
        return List.copyOf(found);
    }

    static String normalizeInputUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String trimmed = rawUrl.strip();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            trimmed = "https://" + trimmed;
        } else if (trimmed.startsWith("http://")) {
            trimmed = "https://" + trimmed.substring("http://".length());
        }
        return AtsSupport.validHttpsUrl(trimmed);
    }

    static String normalizeSlug(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replace("ñ", "n")
                .replaceAll("[^a-z0-9.-]", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }

    private static String capitalizeSlug(String displayName, String fallbackSlug) {
        String alphaNum = displayName.replaceAll("[^A-Za-z0-9]", "");
        if (!alphaNum.isEmpty()) {
            return Character.toUpperCase(alphaNum.charAt(0)) + alphaNum.substring(1);
        }
        return fallbackSlug;
    }

    private static String humanizeCompanyName(String rawInput, String slug) {
        String trimmed = rawInput == null ? "" : rawInput.strip();
        if (!trimmed.isEmpty() && !trimmed.equals(trimmed.toLowerCase(Locale.ROOT))) {
            return trimmed;
        }
        String base = slug.contains(".") ? slug.substring(0, slug.indexOf('.')) : slug;
        String[] parts = base.split("[-_]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? slug : sb.toString();
    }
}
