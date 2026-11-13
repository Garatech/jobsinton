package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.opportunities.OpportunityListing;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

/** One requested page; no automatic next page, retries, cross-request cache or profile data. */
final class AdzunaSearch {
    static final int PAGE_SIZE = 10, LAST_PAGE = 5;
    private final String appId, appKey;
    private final AdzunaTransport transport;
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final AdzunaListingMapper mapper = new AdzunaListingMapper();

    AdzunaSearch(String appId, String appKey, AdzunaTransport transport) {
        this.appId = appId; this.appKey = appKey; this.transport = transport;
    }

    enum Problem {
        INVALID_SEARCH(400), NOT_CONFIGURED(503), PROVIDER_ACCESS_DENIED(502), PROVIDER_RATE_LIMITED(429),
        PROVIDER_UNAVAILABLE(502), PROVIDER_TIMEOUT(504), INVALID_PROVIDER_RESPONSE(502);
        final int status;
        Problem(int status) { this.status = status; }
    }

    static final class Failure extends RuntimeException {
        final Problem problem;
        Failure(Problem problem) { super(problem.name()); this.problem = problem; }
    }

    record Page(String source, int page, int received, int rejected, int duplicates, List<OpportunityListing> listings) {}

    Page search(String what, String where, int page) {
        if (!term(what) || !term(where) || page < 1 || page > LAST_PAGE) throw new Failure(Problem.INVALID_SEARCH);
        if (appId.isBlank() || appKey.isBlank()) throw new Failure(Problem.NOT_CONFIGURED);
        URI uri = URI.create("https://api.adzuna.com/v1/api/jobs/es/search/" + page
                + "?what=" + encode(what.strip()) + "&where=" + encode(where.strip())
                + "&results_per_page=" + PAGE_SIZE + "&app_id=" + encode(appId) + "&app_key=" + encode(appKey));
        var reply = transport.get(uri);
        switch (reply.status()) {
            case 200 -> { }
            case 401, 403 -> throw new Failure(Problem.PROVIDER_ACCESS_DENIED);
            case 429 -> throw new Failure(Problem.PROVIDER_RATE_LIMITED);
            default -> throw new Failure(Problem.PROVIDER_UNAVAILABLE);
        }
        try {
            var root = json.readTree(reply.body());
            var rows = root == null ? null : root.path("results");
            if (rows == null || !rows.isArray() || rows.size() > PAGE_SIZE) throw new Failure(Problem.INVALID_PROVIDER_RESPONSE);
            var unique = new LinkedHashMap<String, OpportunityListing>();
            int rejected = 0, duplicates = 0;
            for (var row : rows) {
                if (mapper.map(row) instanceof AdzunaListingMapper.Accepted(var listing)) {
                    if (unique.putIfAbsent(listing.id(), listing) != null) duplicates++;
                } else rejected++;
            }
            return new Page("Adzuna", page, rows.size(), rejected, duplicates, List.copyOf(unique.values()));
        } catch (tools.jackson.core.JacksonException ex) {
            throw new Failure(Problem.INVALID_PROVIDER_RESPONSE);
        }
    }

    private static boolean term(String value) {
        return value != null && !value.isBlank() && value.length() <= 100 && value.chars().noneMatch(Character::isISOControl);
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
