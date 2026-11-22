package io.github.garatech.jobsinton.providers.ats;

import java.net.URI;
import java.text.Normalizer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated ATS Discovery Engine ("Career URL Auto-Sniffer & Batch Slug Probe").
 *
 * <p>Supports two complementary discovery mechanisms across all 9 ATS platforms:
 * <ol>
 *   <li><b>Career Page URL Auto-Sniffer ({@link #discoverFromUrl(String, String)}):</b> inspects a direct ATS URL
 *       or fetches a corporate career page's HTML and scans embedded scripts, iframes, RSS links, and redirects
 *       for Personio, Teamtailor, Recruitee, Factorial HR, Greenhouse, Lever, SmartRecruiters, Workable, and Ashby
 *       board signatures, then verifies the board live and merges its postings into the active feed.</li>
 *   <li><b>Batch Company Name / Slug Probe ({@link #probeCandidates(List)}):</b> normalizes company names into
 *       candidate slugs and probes all 9 ATS platforms in parallel using Java 21 virtual threads, auto-registering
 *       every verified board.</li>
 * </ol>
 */
public final class AtsDiscoveryService {
    private static final int MAX_PROBE_CANDIDATES = 12;

    private static final Pattern PERSONIO_URL_PATTERN = Pattern.compile(
            "https?://([a-z0-9][a-z0-9-]{1,62})\\.jobs\\.personio\\.(com|de)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RECRUITEE_URL_PATTERN = Pattern.compile(
            "https?://([a-z0-9][a-z0-9-]{1,62})\\.recruitee\\.com\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TEAMTAILOR_HOST_PATTERN = Pattern.compile(
            "https?://(([a-z0-9][a-z0-9-]{1,62})\\.teamtailor\\.com)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TEAMTAILOR_RSS_LINK_PATTERN = Pattern.compile(
            "https?://([a-z0-9.-]+\\.[a-z]{2,})/jobs\\.rss\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TEAMTAILOR_MARKER_PATTERN = Pattern.compile(
            "(?:teamtailor\\.com|tt:remoteStatus|data-teamtailor)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FACTORIAL_URL_PATTERN = Pattern.compile(
            "https?://([a-z0-9][a-z0-9-]{1,62})\\.factorialhr\\.(es|com)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GREENHOUSE_URL_PATTERN = Pattern.compile(
            "https?://(?:boards(?:-api)?(?:\\.eu)?\\.greenhouse\\.io|job-boards(?:\\.eu)?\\.greenhouse\\.io)"
                    + "/(?:v1/boards/|embed/job_board\\?for=)?([A-Za-z0-9_-]{2,64})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LEVER_URL_PATTERN = Pattern.compile(
            "https?://(?:jobs|api)\\.(eu\\.)?lever\\.co/(?:v0/postings/)?([A-Za-z0-9_-]{2,64})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SMARTRECRUITERS_URL_PATTERN = Pattern.compile(
            "https?://(?:jobs|api|careers)\\.smartrecruiters\\.com/(?:v1/companies/)?([A-Za-z0-9_-]{2,64})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WORKABLE_URL_PATTERN = Pattern.compile(
            "https?://apply\\.workable\\.com/(?:api/v[13]/(?:widget/)?accounts/)?([A-Za-z0-9_-]{2,64})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ASHBY_URL_PATTERN = Pattern.compile(
            "https?://(?:jobs|api)\\.ashbyhq\\.com/(?:posting-api/job-board/)?([A-Za-z0-9._-]{2,64})\\b",
            Pattern.CASE_INSENSITIVE
    );

    public record DiscoveredBoardResult(
            String slug,
            String name,
            String provider,
            String providerLabel,
            String apiEndpoint,
            int jobCount,
            boolean newlyRegistered
    ) {}

    public record DiscoveryResponse(
            List<DiscoveredBoardResult> discovered,
            int totalAdded,
            int totalJobsFound,
            String message
    ) {}

    private record CandidateBoard(CompanyBoard board, String detectedEndpoint) {}

    private final AtsHttpClient http;
    private final AtsFeedService feedService;

    public AtsDiscoveryService(AtsHttpClient http, AtsFeedService feedService) {
        this.http = http;
        this.feedService = feedService;
    }

    /**
     * Inspects a direct ATS URL or a corporate career page URL, detects all embedded ATS signatures,
     * verifies the detected board(s) against live ATS endpoints, registers them in {@link CompanyRegistry},
     * and merges their jobs into the active feed cache.
     */
    public DiscoveryResponse discoverFromUrl(String rawUrl, String companyNameOverride) {
        String normalizedUrl = normalizeHttpsUrl(rawUrl);
        if (normalizedUrl == null) {
            throw new IllegalArgumentException("Please enter a valid https:// career page or ATS URL.");
        }

        URI uri = URI.create(normalizedUrl);
        String fallbackName = (companyNameOverride != null && !companyNameOverride.isBlank())
                ? companyNameOverride.strip()
                : humanizeHost(uri.getHost());

        List<CandidateBoard> candidates = new ArrayList<>(extractCandidatesFromText(normalizedUrl, fallbackName));

        // If the URL itself isn't already a known direct ATS URL, fetch the career page HTML and sniff signatures.
        if (candidates.isEmpty()) {
            try {
                AtsHttpClient.HttpReply reply = http.get(uri);
                if (reply.status() >= 200 && reply.status() < 400 && reply.body() != null) {
                    String html = new String(reply.body(), java.nio.charset.StandardCharsets.UTF_8);
                    candidates.addAll(extractCandidatesFromText(html, fallbackName));

                    // Check if the custom domain itself is a Teamtailor custom domain (has /jobs.rss or Teamtailor markers).
                    if (candidates.isEmpty() && TEAMTAILOR_MARKER_PATTERN.matcher(html).find()) {
                        String host = uri.getHost();
                        String slug = slugFromHost(host);
                        CompanyBoard ttBoard = new CompanyBoard(
                                slug,
                                fallbackName,
                                "teamtailor",
                                "eu",
                                "Spain / EU",
                                host,
                                "https://" + host + "/jobs",
                                "https://" + host + "/jobs.rss",
                                null,
                                List.of("Spain"),
                                List.of("ALL"),
                                "HIGH"
                        );
                        candidates.add(new CandidateBoard(ttBoard, "https://" + host + "/jobs.rss"));
                    }
                }
            } catch (RuntimeException ignored) {
                // Handled below if no candidates could be verified
            }
        }

        List<DiscoveredBoardResult> verified = verifyAndRegisterCandidates(candidates, true);
        int added = (int) verified.stream().filter(DiscoveredBoardResult::newlyRegistered).count();
        int jobsFound = verified.stream().mapToInt(DiscoveredBoardResult::jobCount).sum();
        String msg = verified.isEmpty()
                ? "No supported public ATS signature was detected on that URL."
                : "Detected " + verified.size() + " public ATS board(s) with " + jobsFound + " open role(s).";
        return new DiscoveryResponse(verified, added, jobsFound, msg);
    }

    /**
     * Takes a list of company names or slugs, generates normalized slug variations, and probes all 9 ATS
     * platforms in parallel using Java 21 virtual threads.
     */
    public DiscoveryResponse probeCandidates(List<String> rawInputs) {
        if (rawInputs == null || rawInputs.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one company name or slug to probe.");
        }
        List<String> cleaned = rawInputs.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .distinct()
                .limit(MAX_PROBE_CANDIDATES)
                .toList();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one company name or slug to probe.");
        }

        List<CandidateBoard> allProbes = new ArrayList<>();
        for (String input : cleaned) {
            String displayName = humanizeName(input);
            Set<String> slugs = generateCandidateSlugs(input);
            for (String slug : slugs) {
                allProbes.addAll(buildNineAtsProbes(slug, displayName));
            }
        }

        List<DiscoveredBoardResult> verified = verifyAndRegisterCandidates(allProbes, false);
        int added = (int) verified.stream().filter(DiscoveredBoardResult::newlyRegistered).count();
        int jobsFound = verified.stream().mapToInt(DiscoveredBoardResult::jobCount).sum();
        String msg = verified.isEmpty()
                ? "No active public boards responded with open roles for the probed slugs."
                : "Discovered " + verified.size() + " active ATS board(s) with " + jobsFound + " open role(s).";
        return new DiscoveryResponse(verified, added, jobsFound, msg);
    }

    private List<DiscoveredBoardResult> verifyAndRegisterCandidates(
            List<CandidateBoard> candidates,
            boolean allowZeroJobsBoard
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<String, CandidateBoard> unique = new LinkedHashMap<>();
        for (CandidateBoard candidate : candidates) {
            CompanyBoard b = candidate.board();
            String key = b.providerType().id() + ":" + b.slug().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, candidate);
        }

        List<DiscoveredBoardResult> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DiscoveredBoardResult>> futures = unique.values().stream()
                    .map(candidate -> CompletableFuture.supplyAsync(
                            () -> verifySingleCandidate(candidate, allowZeroJobsBoard),
                            executor
                    ))
                    .toList();
            results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> r != null)
                    .toList();
        }
        return results;
    }

    private DiscoveredBoardResult verifySingleCandidate(CandidateBoard candidate, boolean allowZeroJobsBoard) {
        CompanyBoard board = candidate.board();
        AtsProviderType type = board.providerType();
        AtsProviderAdapter adapter = feedService.adapterFor(type);
        if (adapter == null) {
            return null;
        }
        long startNanos = System.nanoTime();
        try {
            List<RawAtsJob> jobs = adapter.fetch(board);
            if (jobs.isEmpty() && !allowZeroJobsBoard) {
                return null;
            }
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            boolean newlyAdded = feedService.registry().registerCustomBoard(board);
            feedService.mergeDiscoveredBoard(board, jobs, durationMs);
            return new DiscoveredBoardResult(
                    board.slug(),
                    board.name(),
                    type.id(),
                    type.displayName(),
                    candidate.detectedEndpoint(),
                    jobs.size(),
                    newlyAdded
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static List<CandidateBoard> extractCandidatesFromText(String text, String fallbackCompanyName) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<CandidateBoard> found = new ArrayList<>();

        Matcher personio = PERSONIO_URL_PATTERN.matcher(text);
        while (personio.find()) {
            String slug = personio.group(1).toLowerCase(Locale.ROOT);
            String tld = personio.group(2).toLowerCase(Locale.ROOT);
            String endpoint = "https://" + slug + ".jobs.personio." + tld + "/xml?language=es";
            String fallback = "https://" + slug + ".jobs.personio.de/xml?language=en";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "personio",
                    "eu",
                    "Spain / EU",
                    null,
                    "https://" + slug + ".jobs.personio." + tld,
                    endpoint,
                    fallback,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher recruitee = RECRUITEE_URL_PATTERN.matcher(text);
        while (recruitee.find()) {
            String slug = recruitee.group(1).toLowerCase(Locale.ROOT);
            if (slug.equals("api") || slug.equals("www") || slug.equals("app")) continue;
            String endpoint = "https://" + slug + ".recruitee.com/api/offers/";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "recruitee",
                    "eu",
                    "Spain / EU",
                    null,
                    "https://" + slug + ".recruitee.com",
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher ttHost = TEAMTAILOR_HOST_PATTERN.matcher(text);
        while (ttHost.find()) {
            String host = ttHost.group(1).toLowerCase(Locale.ROOT);
            String slug = ttHost.group(2).toLowerCase(Locale.ROOT);
            if (slug.equals("www") || slug.equals("app") || slug.equals("career")) continue;
            String endpoint = "https://" + host + "/jobs.rss";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "teamtailor",
                    "eu",
                    "Spain / EU",
                    host,
                    "https://" + host + "/jobs",
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher ttRss = TEAMTAILOR_RSS_LINK_PATTERN.matcher(text);
        while (ttRss.find()) {
            String host = ttRss.group(1).toLowerCase(Locale.ROOT);
            String slug = slugFromHost(host);
            String endpoint = "https://" + host + "/jobs.rss";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "teamtailor",
                    "eu",
                    "Spain / EU",
                    host,
                    "https://" + host + "/jobs",
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher factorial = FACTORIAL_URL_PATTERN.matcher(text);
        while (factorial.find()) {
            String slug = factorial.group(1).toLowerCase(Locale.ROOT);
            if (slug.equals("www") || slug.equals("api") || slug.equals("app")) continue;
            String tld = factorial.group(2).toLowerCase(Locale.ROOT);
            String endpoint = "https://" + slug + ".factorialhr." + tld;
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "factorial",
                    "eu",
                    "Spain",
                    null,
                    endpoint,
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher greenhouse = GREENHOUSE_URL_PATTERN.matcher(text);
        while (greenhouse.find()) {
            String slug = greenhouse.group(1);
            if (isReservedPathToken(slug)) continue;
            String endpoint = "https://boards-api.greenhouse.io/v1/boards/" + slug + "/jobs?content=true";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "greenhouse",
                    "global",
                    "Spain / EU",
                    null,
                    "https://boards.greenhouse.io/" + slug,
                    endpoint,
                    "https://boards-api.eu.greenhouse.io/v1/boards/" + slug + "/jobs?content=true",
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher lever = LEVER_URL_PATTERN.matcher(text);
        while (lever.find()) {
            boolean isEu = lever.group(1) != null;
            String slug = lever.group(2);
            if (isReservedPathToken(slug)) continue;
            String region = isEu ? "eu" : "global";
            String apiHost = isEu ? "api.eu.lever.co" : "api.lever.co";
            String endpoint = "https://" + apiHost + "/v0/postings/" + slug + "?mode=json";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "lever",
                    region,
                    "Spain / EU",
                    null,
                    "https://" + (isEu ? "jobs.eu.lever.co/" : "jobs.lever.co/") + slug,
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher smartRecruiters = SMARTRECRUITERS_URL_PATTERN.matcher(text);
        while (smartRecruiters.find()) {
            String slug = smartRecruiters.group(1);
            if (isReservedPathToken(slug)) continue;
            String endpoint = "https://api.smartrecruiters.com/v1/companies/" + slug + "/postings?limit=100";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "smartrecruiters",
                    "global",
                    "Spain / EU",
                    null,
                    "https://careers.smartrecruiters.com/" + slug,
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher workable = WORKABLE_URL_PATTERN.matcher(text);
        while (workable.find()) {
            String slug = workable.group(1);
            if (isReservedPathToken(slug)) continue;
            String endpoint = "https://apply.workable.com/api/v1/widget/accounts/" + slug + "?details=true";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "workable",
                    "global",
                    "Spain / EU",
                    null,
                    "https://apply.workable.com/" + slug + "/",
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        Matcher ashby = ASHBY_URL_PATTERN.matcher(text);
        while (ashby.find()) {
            String slug = ashby.group(1);
            if (isReservedPathToken(slug)) continue;
            String endpoint = "https://api.ashbyhq.com/posting-api/job-board/" + slug + "?includeCompensation=true";
            CompanyBoard board = new CompanyBoard(
                    slug,
                    resolveCompanyName(fallbackCompanyName, slug),
                    "ashby",
                    "global",
                    "Spain / EU",
                    null,
                    "https://jobs.ashbyhq.com/" + slug,
                    endpoint,
                    null,
                    List.of("Spain"),
                    List.of("ALL"),
                    "HIGH"
            );
            found.add(new CandidateBoard(board, endpoint));
        }

        return found;
    }

    private static List<CandidateBoard> buildNineAtsProbes(String slug, String displayName) {
        return List.of(
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "personio", "eu", "Spain / EU"),
                        "https://" + slug + ".jobs.personio.com/xml?language=es"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "teamtailor", "eu", "Spain / EU"),
                        "https://" + slug + ".teamtailor.com/jobs.rss"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "recruitee", "eu", "Spain / EU"),
                        "https://" + slug + ".recruitee.com/api/offers/"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "factorial", "eu", "Spain"),
                        "https://" + slug + ".factorialhr.es"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "greenhouse", "global", "Spain / EU"),
                        "https://boards-api.greenhouse.io/v1/boards/" + slug + "/jobs?content=true"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "lever", "eu", "Spain / EU"),
                        "https://api.eu.lever.co/v0/postings/" + slug + "?mode=json"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "smartrecruiters", "global", "Spain / EU"),
                        "https://api.smartrecruiters.com/v1/companies/" + slug + "/postings?limit=100"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "workable", "global", "Spain / EU"),
                        "https://apply.workable.com/api/v1/widget/accounts/" + slug + "?details=true"
                ),
                new CandidateBoard(
                        new CompanyBoard(slug, displayName, "ashby", "global", "Spain / EU"),
                        "https://api.ashbyhq.com/posting-api/job-board/" + slug + "?includeCompensation=true"
                )
        );
    }

    static Set<String> generateCandidateSlugs(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(rawName.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("(?i)(?:^|\\s+)(?:s\\.?l\\.?u\\.?|s\\.?l\\.?|s\\.?a\\.?|gmbh|inc\\.?|ltd\\.?|corp\\.?|spain|espana|iberia|group|consulting|technologies|technology|digital|solutions)(?=\\s|$)", " ")
                .strip();
        if (normalized.isEmpty()) {
            normalized = Normalizer.normalize(rawName.strip(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .strip();
        }
        String hyphenated = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        String compact = normalized.replaceAll("[^a-z0-9]+", "");

        // Also keep full compact/hyphenated forms before stripping suffix words like "digital"/"consulting"
        String fullCleaned = Normalizer.normalize(rawName.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("(?i)(?:^|\\s+)(?:s\\.?l\\.?u\\.?|s\\.?l\\.?|s\\.?a\\.?|gmbh|inc\\.?|ltd\\.?|corp\\.?)(?=\\s|$)", " ")
                .strip();
        String fullCompact = fullCleaned.replaceAll("[^a-z0-9]+", "");
        String fullHyphenated = fullCleaned.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");

        Set<String> slugs = new LinkedHashSet<>();
        if (fullCompact.length() >= 2) slugs.add(fullCompact);
        if (fullHyphenated.length() >= 2) slugs.add(fullHyphenated);
        if (compact.length() >= 2) slugs.add(compact);
        if (hyphenated.length() >= 2) slugs.add(hyphenated);
        return slugs;
    }

    private static String normalizeAlnum(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private static String defaultEndpointForBoard(CompanyBoard board) {
        String slug = board.slug();
        return switch (board.providerType()) {
            case PERSONIO -> "https://" + slug + ".jobs.personio.com/xml?language=es";
            case TEAMTAILOR -> "https://" + (board.host() != null ? board.host() : slug + ".teamtailor.com") + "/jobs.rss";
            case RECRUITEE -> "https://" + slug + ".recruitee.com/api/offers/";
            case FACTORIAL -> "https://" + slug + ".factorialhr.es";
            case GREENHOUSE -> "https://boards-api.greenhouse.io/v1/boards/" + slug + "/jobs?content=true";
            case LEVER -> "https://api.eu.lever.co/v0/postings/" + slug + "?mode=json";
            case SMARTRECRUITERS -> "https://api.smartrecruiters.com/v1/companies/" + slug + "/postings?limit=100";
            case WORKABLE -> "https://apply.workable.com/api/v1/widget/accounts/" + slug + "?details=true";
            case ASHBY -> "https://api.ashbyhq.com/posting-api/job-board/" + slug + "?includeCompensation=true";
        };
    }

    private static boolean isReservedPathToken(String slug) {
        if (slug == null) return true;
        String lower = slug.toLowerCase(Locale.ROOT);
        return lower.equals("v1")
                || lower.equals("v0")
                || lower.equals("v3")
                || lower.equals("api")
                || lower.equals("boards")
                || lower.equals("postings")
                || lower.equals("companies")
                || lower.equals("embed")
                || lower.equals("posting-api")
                || lower.equals("job-board")
                || lower.equals("widget")
                || lower.equals("accounts");
    }

    private static String normalizeHttpsUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String candidate = rawUrl.strip();
        if (!candidate.startsWith("https://") && !candidate.startsWith("http://")) {
            candidate = "https://" + candidate;
        }
        return AtsSupport.validHttpsUrl(candidate);
    }

    private static String slugFromHost(String host) {
        if (host == null || host.isBlank()) {
            return "custom";
        }
        String[] parts = host.toLowerCase(Locale.ROOT).split("\\.");
        for (String part : parts) {
            if (!part.equals("www")
                    && !part.equals("careers")
                    && !part.equals("career")
                    && !part.equals("jobs")
                    && !part.equals("empleo")
                    && !part.equals("talent")
                    && !part.equals("join")
                    && part.length() >= 2) {
                return part.replaceAll("[^a-z0-9-]", "");
            }
        }
        return parts[0].replaceAll("[^a-z0-9-]", "");
    }

    private static String humanizeHost(String host) {
        return humanizeName(slugFromHost(host));
    }

    private static String resolveCompanyName(String fallbackCompanyName, String slug) {
        if (fallbackCompanyName != null && !fallbackCompanyName.isBlank()
                && !fallbackCompanyName.equalsIgnoreCase("Greenhouse")
                && !fallbackCompanyName.equalsIgnoreCase("Lever")
                && !fallbackCompanyName.equalsIgnoreCase("Personio")
                && !fallbackCompanyName.equalsIgnoreCase("Recruitee")
                && !fallbackCompanyName.equalsIgnoreCase("Teamtailor")
                && !fallbackCompanyName.equalsIgnoreCase("Factorialhr")
                && !fallbackCompanyName.equalsIgnoreCase("Workable")
                && !fallbackCompanyName.equalsIgnoreCase("Smartrecruiters")
                && !fallbackCompanyName.equalsIgnoreCase("Ashbyhq")) {
            return fallbackCompanyName;
        }
        return humanizeName(slug);
    }

    private static String humanizeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Company";
        }
        String[] parts = raw.strip().split("[\\s_-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.isEmpty() ? raw.strip() : sb.toString();
    }
}
