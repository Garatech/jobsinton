package io.github.garatech.jobsinton.matching.api;

import java.io.IOException;
import java.util.List;

import io.github.garatech.jobsinton.matching.MatchingEngine;
import io.github.garatech.jobsinton.matching.MatchingInput;
import io.github.garatech.jobsinton.matching.MatchingResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A transient comparison: profile bodies are neither persisted nor logged. */
@RestController
public class MatchingController {
    private static final int MAX_BODY_BYTES = 16_384;
    private final MatchingEngine engine = new MatchingEngine();
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    @PostMapping(value = "/api/matching/evaluate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> evaluate(HttpServletRequest request) throws IOException {
        // Bound the read before JSON parsing, including requests without Content-Length.
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) return failure(413, "REQUEST_TOO_LARGE", "The comparison request is too large.", List.of());
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (RuntimeException malformed) {
            return failure(400, "INVALID_JSON", "Send one valid JSON object.", List.of());
        }
        if (root == null || root.isMissingNode()) return failure(400, "INVALID_JSON", "Send one valid JSON object.", List.of());
        if (!root.isObject() || root.size() != 2 || !root.has("opportunityId") || !root.has("profile")
                || !root.get("opportunityId").isTextual()) {
            return failure(422, "INVALID_REQUEST", "Provide an opportunityId and saved profile.", List.of());
        }
        String id = root.get("opportunityId").asText();
        if (id.isBlank() || id.length() > 80) {
            return failure(422, "INVALID_REQUEST", "Provide a valid opportunity identifier.", List.of());
        }
        var adapted = SavedProfileAdapter.read(root.get("profile"));
        if (!adapted.errors().isEmpty()) {
            return failure(422, "INVALID_PROFILE", "Review and save a valid profile before comparing.", adapted.errors());
        }
        var opportunity = FictionalMatchingCatalog.find(id);
        if (opportunity == null) return failure(404, "OPPORTUNITY_NOT_FOUND", "This opportunity is not available for comparison.", List.of());
        var result = engine.evaluate(new MatchingInput(adapted.profile(), opportunity));
        if (result instanceof MatchingResult.Invalid invalid) {
            return failure(422, "INVALID_FACTS", "The comparison facts could not be evaluated.", invalid.errors());
        }
        var evaluation = (MatchingResult.Evaluation) result;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Comparison("matching-api-v1", id, evaluation));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> unreadableBody() {
        return failure(400, "INVALID_JSON", "The comparison request could not be read.", List.of());
    }

    private ResponseEntity<?> failure(int status, String code, String message, List<MatchingResult.ValidationError> errors) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code, message, errors));
    }

    public record Comparison(String apiVersion, String opportunityId, MatchingResult.Evaluation evaluation) {}
    public record ApiError(String code, String message, List<MatchingResult.ValidationError> errors) {}
}
