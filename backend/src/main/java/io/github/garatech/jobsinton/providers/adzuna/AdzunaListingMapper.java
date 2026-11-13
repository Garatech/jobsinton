package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;
import io.github.garatech.jobsinton.opportunities.OpportunityListing;
import io.github.garatech.jobsinton.opportunities.OpportunityListing.Salary;
import io.github.garatech.jobsinton.opportunities.OpportunityListing.SalaryOrigin;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/** Maps one element of an Adzuna search results array. No transport, persistence or text inference. */
public final class AdzunaListingMapper {
    private static final Set<String> HOSTS = Set.of("adzuna.es", "adzuna.com.es", "adzuna.co.uk", "adzuna.com");

    public sealed interface Result permits Accepted, Rejected {}
    public record Accepted(OpportunityListing listing) implements Result {}
    public record Rejected(Reason reason) implements Result {}
    public enum Reason { INVALID_ROW, MISSING_ID, MISSING_TITLE, INVALID_REFERRAL }

    public Result map(JsonNode row) {
        if (row == null || !row.isObject()) return new Rejected(Reason.INVALID_ROW);
        JsonNode rawId = row.path("id");
        String id = rawId.isIntegralNumber() && rawId.bigIntegerValue().signum() >= 0
                ? rawId.bigIntegerValue().toString() : text(rawId);
        if (id == null) return new Rejected(Reason.MISSING_ID);
        String title = text(row.path("title"));
        if (title == null) return new Rejected(Reason.MISSING_TITLE);
        URI referral = referral(text(row.path("redirect_url")));
        if (referral == null) return new Rejected(Reason.INVALID_REFERRAL);

        // A keyword hit, contract schedule and display location establish none of these matching facts.
        Opportunity facts = new Opportunity(null, null, null, null, null, null, null);
        return new Accepted(new OpportunityListing("adzuna:" + id, title, referral,
                text(row.path("company").path("display_name")), location(row.path("location")),
                created(text(row.path("created"))), text(row.path("description")),
                text(row.path("category").path("label")), text(row.path("contract_type")),
                text(row.path("contract_time")), salary(row), facts));
    }

    private static String text(JsonNode node) {
        if (!node.isString()) return null;
        String value = node.asString().strip();
        return value.isEmpty() ? null : value;
    }

    private static String location(JsonNode node) {
        String label = text(node.path("display_name"));
        if (label != null) return label;
        JsonNode areas = node.path("area");
        if (!areas.isArray()) return null;
        var names = new ArrayList<String>();
        for (JsonNode area : areas) {
            String name = text(area);
            if (name != null) names.add(name);
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static Instant created(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private static URI referral(String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawPath().length() <= 1) return null;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (!HOSTS.contains(host.startsWith("www.") ? host.substring(4) : host)) return null;
            if (uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&")) {
                    String key = URLDecoder.decode(pair.split("=", 2)[0], StandardCharsets.UTF_8);
                    if ("app_key".equalsIgnoreCase(key) || "app_id".equalsIgnoreCase(key)) return null;
                }
            }
            return uri;
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private static Salary salary(JsonNode row) {
        JsonNode lo = row.path("salary_min"), hi = row.path("salary_max");
        BigDecimal min = amount(lo), max = amount(hi);
        if (invalidBound(lo, min) || invalidBound(hi, max) || (min == null && max == null)
                || (min != null && max != null && min.compareTo(max) > 0)) return null;
        JsonNode flag = row.path("salary_is_predicted");
        return new Salary(min, max, origin(flag));
    }

    private static SalaryOrigin origin(JsonNode flag) {
        if (flag.isBoolean()) return flag.asBoolean() ? SalaryOrigin.PREDICTED : SalaryOrigin.ADVERTISED;
        String value = flag.isIntegralNumber() ? flag.bigIntegerValue().toString() : text(flag);
        if ("1".equals(value)) return SalaryOrigin.PREDICTED;
        if ("0".equals(value)) return SalaryOrigin.ADVERTISED;
        return SalaryOrigin.UNSPECIFIED;
    }

    private static BigDecimal amount(JsonNode node) {
        if (!node.isNumber() || (node.isFloatingPointNumber() && !Double.isFinite(node.doubleValue()))) return null;
        return node.decimalValue();
    }

    private static boolean invalidBound(JsonNode node, BigDecimal value) {
        return !node.isMissingNode() && !node.isNull() && (value == null || value.signum() <= 0);
    }
}
