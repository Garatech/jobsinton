package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared parsing, URL validation, date normalization, and workplace inference helpers for ATS adapters. */
final class AtsSupport {
    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("es", "Spain"),
            Map.entry("de", "Germany"),
            Map.entry("fr", "France"),
            Map.entry("gb", "United Kingdom"),
            Map.entry("uk", "United Kingdom"),
            Map.entry("pt", "Portugal"),
            Map.entry("it", "Italy"),
            Map.entry("nl", "Netherlands"),
            Map.entry("ie", "Ireland"),
            Map.entry("pl", "Poland"),
            Map.entry("us", "United States"),
            Map.entry("ch", "Switzerland"),
            Map.entry("be", "Belgium"),
            Map.entry("at", "Austria"),
            Map.entry("se", "Sweden")
    );

    private AtsSupport() {}

    static String text(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        String value = node.asString().strip();
        return value.isEmpty() ? null : value;
    }

    static String idText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isString()) {
            return text(node);
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        return null;
    }

    static boolean boolFlag(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isString()) {
            return "true".equalsIgnoreCase(node.asString().strip());
        }
        return false;
    }

    static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String validHttpsUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null) {
                return null;
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static Instant parseTimestamp(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            long value = node.asLong();
            if (value <= 0) {
                return null;
            }
            // Distinguish seconds vs milliseconds cleanly.
            return value > 100_000_000_000L ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
        }
        return parseTimestampString(text(node));
    }

    static Instant parseTimestampString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.strip();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Fall through to offset/local formats.
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through.
        }
        try {
            return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Fall through.
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through to RFC-1123 (used by Teamtailor RSS <pubDate>).
        }
        try {
            return java.time.ZonedDateTime.parse(trimmed, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    static String normalizeWorkplaceType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String lower = raw.strip().toLowerCase(Locale.ROOT);
        if ("fully".equals(lower) || "temporary".equals(lower)
                || lower.contains("remote") || lower.contains("telecommut") || lower.contains("remoto")
                || lower.contains("teletrabajo") || lower.contains("mobil")) {
            return lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid") ? "hybrid" : "remote";
        }
        if (lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid")) {
            return "hybrid";
        }
        if ("none".equals(lower) || lower.contains("onsite") || lower.contains("on-site") || lower.contains("on_site")
                || lower.contains("in-office") || lower.contains("office") || lower.contains("presencial")) {
            return "onsite";
        }
        return null;
    }

    static String inferWorkplaceFromLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String lower = location.toLowerCase(Locale.ROOT);
        if (lower.contains("remote") || lower.contains("remoto") || lower.contains("teletrabajo")) {
            return lower.contains("hybrid") || lower.contains("híbrid") ? "hybrid" : "remote";
        }
        if (lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid")) {
            return "hybrid";
        }
        return null;
    }

    static boolean isSpainOrEuLocation(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("madrid") || lower.contains("barcelona") || lower.contains("valencia")
                || lower.contains("málaga") || lower.contains("malaga") || lower.contains("sevilla")
                || lower.contains("bilbao") || lower.contains("zaragoza") || lower.contains("pozuelo")
                || lower.contains("las rozas") || lower.contains("alcobendas") || lower.contains("tres cantos")
                || lower.contains("getafe") || lower.contains("boadilla") || lower.contains("alicante")
                || lower.contains("coruña") || lower.contains("coruna") || lower.contains("vigo")
                || lower.contains("granada") || lower.contains("oviedo") || lower.contains("valladolid")
                || lower.contains("san sebastián") || lower.contains("san sebastian") || lower.contains("cartagena")
                || lower.contains("murcia") || lower.contains("castellón") || lower.contains("castellon")
                || lower.contains("spain") || lower.contains("españa") || lower.contains("espana")
                || lower.contains("iberia") || lower.contains("galicia") || lower.contains("asturias");
    }

    static boolean isNonEuFakeRemote(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("us only") || lower.contains("u.s. only") || lower.contains("united states")
                || lower.contains("remote - us") || lower.contains("remote (us)") || lower.contains("north america")
                || lower.contains("canada") || lower.contains("latam") || lower.contains("latin america")
                || lower.contains("apac") || lower.contains("india") || lower.contains("uk only")
                || lower.contains("united kingdom only") || lower.contains("australia")
                || lower.contains("brazil") || lower.contains("mexico");
    }

    static String countryDisplayName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.length() == 2) {
            return COUNTRY_NAMES.getOrDefault(trimmed.toLowerCase(Locale.ROOT), trimmed.toUpperCase(Locale.ROOT));
        }
        return trimmed;
    }

    static String joinUniqueParts(List<String> parts, String delimiter) {
        var unique = new LinkedHashSet<String>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                unique.add(part.strip());
            }
        }
        return unique.isEmpty() ? null : String.join(delimiter, unique);
    }
}
