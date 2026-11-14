package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.api.ComparisonHttp;
import io.github.garatech.jobsinton.opportunities.OpportunityListing;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@Profile("adzuna-local")
@RequestMapping("/api/providers/adzuna")
class AdzunaSearchController {
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    private static final Set<String> LOCAL = Set.of("localhost", "127.0.0.1", "::1", "[::1]", "0:0:0:0:0:0:0:1");
    private final AdzunaSearch search;
    private final AdzunaComparisonRefs refs;
    AdzunaSearchController(AdzunaSearch search, AdzunaComparisonRefs refs) {
        this.search = search;
        this.refs = refs;
    }

    record SearchResponse(String source, int page, int received, int rejected, int duplicates,
                          List<OpportunityListing> listings, Map<String, String> comparisonRefs) {}

    record Query(String what, String where, Integer page) {}

    // Only the same local Spring server that serves the UI may spend provider quota.
    // CLI requests still have no Origin. Do not trust forwarded host or origin headers.
    private static boolean localClient(HttpServletRequest request) {
        return LOCAL.contains(request.getRemoteAddr()) && LOCAL.contains(request.getServerName());
    }

    @GetMapping("/availability")
    ResponseEntity<?> availability(HttpServletRequest request) {
        return localClient(request) ? reply(200, Map.of("source", "Adzuna"))
                : reply(403, Map.of("code", "LOCAL_ONLY"));
    }

    @PostMapping(path = "/search", consumes = "application/json", produces = "application/json")
    ResponseEntity<?> search(@RequestBody Query query, HttpServletRequest request) {
        if (!localSubmission(request)) {
            return reply(403, Map.of("code", "LOCAL_ONLY"));
        }
        try {
            var page = search.search(query.what(), query.where(), query.page() == null ? 1 : query.page());
            var references = new LinkedHashMap<String, String>();
            for (var listing : page.listings()) {
                refs.issue(listing).ifPresent(token -> references.put(listing.id(), token));
            }
            return reply(200, new SearchResponse(page.source(), page.page(), page.received(), page.rejected(),
                    page.duplicates(), page.listings(), references));
        } catch (AdzunaSearch.Failure failure) {
            return reply(failure.problem.status, Map.of("code", failure.problem.name()));
        }
    }

    @PostMapping(path = "/compare", consumes = "application/json", produces = "application/json")
    ResponseEntity<?> compare(HttpServletRequest request) {
        if (!localSubmission(request)) {
            return ComparisonHttp.failure(403, "LOCAL_ONLY", "Open JobSinton on the local backend.", List.of());
        }
        return ComparisonHttp.withBody(request, body -> {
            if (!body.isObject() || body.size() != 2 || !body.has("profile") || !body.path("comparisonRef").isString()) {
                return ComparisonHttp.failure(422, "INVALID_REQUEST", "Provide a comparison reference and saved profile.", List.of());
            }
            var snapshot = refs.verify(body.get("comparisonRef").asString());
            if (snapshot.isEmpty()) {
                return ComparisonHttp.failure(422, "INVALID_COMPARISON_REF",
                        "This listing reference is no longer usable. Search again to compare it.", List.of());
            }
            var listing = snapshot.get();
            return ComparisonHttp.evaluate(listing.id(), listing.facts(), body.get("profile"));
        });
    }

    private static boolean localSubmission(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String localOrigin = "http://" + request.getServerName() + ":" + request.getLocalPort();
        return localClient(request) && (origin == null || origin.equals(localOrigin));
    }

    private static ResponseEntity<?> reply(int status, Object body) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).cacheControl(CacheControl.noStore()).body(body);
    }
}
