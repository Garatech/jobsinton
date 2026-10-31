package io.github.garatech.jobsinton.matching.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.JsonNode;

import static io.github.garatech.jobsinton.matching.MatchingInput.*;
import static io.github.garatech.jobsinton.matching.MatchingResult.*;

/** Validates the existing browser profile contract before normalizing its facts. */
final class SavedProfileAdapter {
    private SavedProfileAdapter() {}
    private static final Set<String> FIELDS = Set.of(
            "role", "technologies", "experienceYears", "country", "city", "workMode", "maxOnsiteDays");
    private static final Map<String, Role> ROLES = Map.of(
            "backend", Role.BACKEND, "frontend", Role.FRONTEND, "full-stack", Role.FULL_STACK,
            "sap", Role.SAP, "data", Role.DATA, "platform", Role.PLATFORM);
    private static final Map<String, WorkMode> MODES = Map.of(
            "remote", WorkMode.REMOTE, "hybrid", WorkMode.HYBRID, "onsite", WorkMode.ONSITE);
    private static final Map<String, String> COUNTRIES = Map.ofEntries(
            Map.entry("es", "ES"), Map.entry("spain", "ES"), Map.entry("españa", "ES"),
            Map.entry("de", "DE"), Map.entry("germany", "DE"), Map.entry("alemania", "DE"),
            Map.entry("fr", "FR"), Map.entry("france", "FR"), Map.entry("francia", "FR"),
            Map.entry("pt", "PT"), Map.entry("portugal", "PT"),
            Map.entry("it", "IT"), Map.entry("italy", "IT"), Map.entry("italia", "IT"),
            Map.entry("ie", "IE"), Map.entry("ireland", "IE"), Map.entry("irlanda", "IE"),
            Map.entry("gb", "GB"), Map.entry("uk", "GB"), Map.entry("united kingdom", "GB"), Map.entry("reino unido", "GB"));
    private static final Map<String, String> SPANISH_CITIES = Map.of(
            "madrid", "madrid", "valencia", "valencia", "valència", "valencia", "barcelona", "barcelona");

    record Adapted(Profile profile, List<ValidationError> errors) {}

    static Adapted read(JsonNode node) {
        var errors = new ArrayList<ValidationError>();
        if (node == null || !node.isObject() || node.size() != FIELDS.size()
                || !FIELDS.stream().allMatch(node::has)) {
            error(errors, "profile", "Provide exactly the seven saved-profile fields.");
            return new Adapted(null, List.copyOf(errors));
        }
        String role = text(node, "role", 40, errors);
        String mode = text(node, "workMode", 40, errors);
        String country = text(node, "country", 80, errors);
        String city = text(node, "city", 80, errors);
        Role target = ROLES.get(role);
        WorkMode preference = MODES.get(mode);
        if (target == null) error(errors, "profile.role", "Choose a recognized target role.");
        if (preference == null) error(errors, "profile.workMode", "Choose a recognized work mode.");
        Integer years = integer(node.get("experienceYears"), 0, 60);
        if (years == null) error(errors, "profile.experienceYears", "Use a whole number of years from 0 to 60.");

        Integer maximum = null;
        if (preference == WorkMode.REMOTE) {
            if (!node.get("maxOnsiteDays").isNull()) {
                error(errors, "profile.maxOnsiteDays", "Remote profiles preserve an unconfigured null maximum.");
            }
        } else if (preference != null) {
            maximum = integer(node.get("maxOnsiteDays"), 1, 5);
            if (maximum == null) error(errors, "profile.maxOnsiteDays", "Choose a maximum of 1 to 5 office days.");
        }

        var technologies = new ArrayList<String>();
        var seen = new HashSet<String>();
        var terms = node.get("technologies");
        if (!terms.isArray() || terms.size() < 1 || terms.size() > 20) {
            error(errors, "profile.technologies", "Supply 1–20 technology names.");
        } else {
            for (int i = 0; i < terms.size(); i++) {
                var term = terms.get(i);
                String value = term.isTextual() ? clean(term.asText()) : "";
                if (value.isEmpty() || value.length() > 40 || !seen.add(value.toLowerCase(Locale.ROOT))) {
                    error(errors, "profile.technologies[" + i + "]", "Use unique technology names of 1–40 characters.");
                } else technologies.add(value);
            }
        }
        if (!errors.isEmpty()) return new Adapted(null, List.copyOf(errors));
        String resolvedCountry = COUNTRIES.get(country.toLowerCase(Locale.ROOT));
        String resolvedCity = "ES".equals(resolvedCountry) ? SPANISH_CITIES.get(city.toLowerCase(Locale.ROOT)) : null;
        return new Adapted(new Profile(target, technologies, years,
                new Place(resolvedCountry, resolvedCity), preference, maximum), List.of());
    }

    private static String text(JsonNode node, String field, int max, List<ValidationError> errors) {
        var value = node.get(field);
        String text = value.isTextual() ? clean(value.asText()) : "";
        if (text.isEmpty() || text.length() > max) {
            error(errors, "profile." + field, "Supply text with 1–" + max + " characters.");
        }
        return text;
    }

    private static Integer integer(JsonNode value, int min, int max) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) return null;
        int number = value.intValue();
        return number >= min && number <= max ? number : null;
    }

    private static String clean(String value) { return value.replaceAll("(?U)\\s+", " ").trim(); }
    private static void error(List<ValidationError> errors, String field, String message) {
        errors.add(new ValidationError(field, ValidationCode.INVALID_VALUE, message));
    }
}
