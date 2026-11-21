package io.github.garatech.jobsinton.providers.ats;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Public Multi-ATS direct job board & auto-discovery endpoints; no API keys or user credentials required. */
@RestController
@RequestMapping("/api/ats")
public class AtsController {
    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    public record DiscoverUrlRequest(String url, String companyName) {}

    public record DiscoverProbeRequest(List<String> candidates) {}

    private final AtsFeedService feedService;
    private final AtsDiscoveryService discoveryService;

    public AtsController(AtsFeedService feedService, AtsDiscoveryService discoveryService) {
        this.feedService = feedService;
        this.discoveryService = discoveryService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<AtsFeedResponse> jobs(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok()
                .contentType(JSON_UTF8)
                .cacheControl(CacheControl.noStore())
                .body(feedService.getFeed(refresh));
    }

    @GetMapping("/boards")
    public ResponseEntity<List<CompanyBoard>> boards() {
        return ResponseEntity.ok()
                .contentType(JSON_UTF8)
                .cacheControl(CacheControl.noStore())
                .body(feedService.boards());
    }

    @PostMapping("/discover/url")
    public ResponseEntity<?> discoverFromUrl(@RequestBody(required = false) DiscoverUrlRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            return ResponseEntity.badRequest()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "INVALID_URL", "message", "Please enter a valid https:// career page or ATS URL."));
        }
        try {
            AtsDiscoveryService.DiscoveryResponse result = discoveryService.discoverFromUrl(request.url(), request.companyName());
            return ResponseEntity.ok()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "INVALID_URL", "message", e.getMessage()));
        }
    }

    @PostMapping("/discover/probe")
    public ResponseEntity<?> discoverFromProbe(@RequestBody(required = false) DiscoverProbeRequest request) {
        if (request == null || request.candidates() == null || request.candidates().isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "INVALID_CANDIDATES", "message", "Provide at least one company name or slug to probe."));
        }
        try {
            AtsDiscoveryService.DiscoveryResponse result = discoveryService.probeCandidates(request.candidates());
            return ResponseEntity.ok()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "INVALID_CANDIDATES", "message", e.getMessage()));
        }
    }

    @GetMapping("/smartrecruiters/{companySlug}/{postingId}")
    public ResponseEntity<?> smartRecruitersDetail(
            @PathVariable("companySlug") String companySlug,
            @PathVariable("postingId") String postingId
    ) {
        try {
            return feedService.hydrateSmartRecruitersDetail(companySlug, postingId)
                    .<ResponseEntity<?>>map(detail -> ResponseEntity.ok()
                            .contentType(JSON_UTF8)
                            .cacheControl(CacheControl.noStore())
                            .body(detail))
                    .orElseGet(() -> ResponseEntity.status(404)
                            .contentType(JSON_UTF8)
                            .cacheControl(CacheControl.noStore())
                            .body(Map.of("code", "UNKNOWN_BOARD", "message", "Unknown SmartRecruiters company board.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "INVALID_POSTING_ID", "message", "Invalid SmartRecruiters posting identifier."));
        } catch (AtsFetchException e) {
            return ResponseEntity.status(502)
                    .contentType(JSON_UTF8)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("code", "DETAIL_UNAVAILABLE", "message", "Could not load SmartRecruiters posting details."));
        }
    }
}
