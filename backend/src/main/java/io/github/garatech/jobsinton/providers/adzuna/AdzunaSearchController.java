package io.github.garatech.jobsinton.providers.adzuna;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@Profile("adzuna-local")
@RequestMapping("/api/providers/adzuna")
class AdzunaSearchController {
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    private static final Set<String> LOCAL = Set.of("localhost", "127.0.0.1", "::1", "[::1]", "0:0:0:0:0:0:0:1");
    private final AdzunaSearch search;
    AdzunaSearchController(AdzunaSearch search) { this.search = search; }

    record Query(String what, String where, Integer page) {}

    @PostMapping(path = "/search", consumes = "application/json", produces = "application/json")
    ResponseEntity<?> search(@RequestBody Query query, HttpServletRequest request) {
        if (!LOCAL.contains(request.getRemoteAddr()) || !LOCAL.contains(request.getServerName())
                || request.getHeader("Origin") != null) {
            return reply(403, Map.of("code", "LOCAL_ONLY"));
        }
        try {
            return reply(200, search.search(query.what(), query.where(), query.page() == null ? 1 : query.page()));
        } catch (AdzunaSearch.Failure failure) {
            return reply(failure.problem.status, Map.of("code", failure.problem.name()));
        }
    }

    private static ResponseEntity<?> reply(int status, Object body) {
        return ResponseEntity.status(status).contentType(JSON_UTF8).cacheControl(CacheControl.noStore()).body(body);
    }
}
