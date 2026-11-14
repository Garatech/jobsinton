package io.github.garatech.jobsinton.matching.api;

import java.util.LinkedHashMap;
import java.util.List;

import io.github.garatech.jobsinton.matching.MatchingInput;
import io.github.garatech.jobsinton.ranking.CompatibilityRanker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import static io.github.garatech.jobsinton.matching.api.ComparisonHttp.failure;
import static io.github.garatech.jobsinton.matching.api.ComparisonHttp.withBody;

/** A transient comparison: profile bodies are neither persisted nor logged. */
@RestController
public class MatchingController {
    private static final int MAX_BATCH_IDS = 100;
    private final CompatibilityRanker ranker = new CompatibilityRanker();

    @PostMapping(value = "/api/matching/evaluate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> evaluate(HttpServletRequest request) {
        return withBody(request, this::evaluateOffer);
    }

    @PostMapping(value = "/api/matching/rank", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> rank(HttpServletRequest request) {
        return withBody(request, this::rankOffers);
    }

    private ResponseEntity<?> evaluateOffer(JsonNode root) {
        if (!root.isObject() || root.size() != 2 || !root.has("opportunityId") || !root.has("profile")
                || !root.get("opportunityId").isTextual()) {
            return failure(422, "INVALID_REQUEST", "Provide an opportunityId and saved profile.", List.of());
        }
        String id = root.get("opportunityId").asText();
        if (id.isBlank() || id.length() > 80) {
            return failure(422, "INVALID_REQUEST", "Provide a valid opportunity identifier.", List.of());
        }
        return ComparisonHttp.evaluate(id, FictionalMatchingCatalog.find(id), root.get("profile"));
    }

    private ResponseEntity<?> rankOffers(JsonNode root) {
        if (!root.isObject() || root.size() != 2 || !root.has("profile") || !root.has("opportunityIds")
                || !root.get("opportunityIds").isArray() || root.get("opportunityIds").size() > MAX_BATCH_IDS) {
            return failure(422, "INVALID_REQUEST", "Provide a saved profile and up to 100 opportunity IDs.", List.of());
        }
        var adapted = SavedProfileAdapter.read(root.get("profile"));
        if (!adapted.errors().isEmpty()) {
            return failure(422, "INVALID_PROFILE", "Review and save a valid profile before ranking.", adapted.errors());
        }

        var offers = new LinkedHashMap<String, MatchingInput.Opportunity>();
        for (JsonNode item : root.get("opportunityIds")) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > 80
                    || offers.containsKey(item.asText())) {
                return failure(422, "INVALID_REQUEST", "Opportunity IDs must be unique, nonblank strings of at most 80 characters.", List.of());
            }
            String id = item.asText();
            // An unresolved ID remains part of the batch, with its own unavailable result.
            offers.put(id, FictionalMatchingCatalog.find(id));
        }
        var batch = (CompatibilityRanker.RankedBatch) ranker.rank(adapted.profile(), offers);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Ranking(
                "ranking-api-v1", batch.rankingVersion(), batch.matchingVersion(),
                batch.ranked().stream().map(CompatibilityRanker.RankedOffer::id).toList(),
                batch.unavailable().stream().map(CompatibilityRanker.UnavailableOffer::id).toList()));
    }

    public record Ranking(String apiVersion, String rankingVersion, String matchingVersion,
                          List<String> ranked, List<String> unavailable) {}
}
