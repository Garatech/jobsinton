package io.github.garatech.jobsinton.matching;

import java.util.ArrayList;
import java.util.List;

import static io.github.garatech.jobsinton.matching.MatchingInput.*;
import static io.github.garatech.jobsinton.matching.MatchingResult.*;

/** Checks normalized facts without inventing replacements for invalid values. */
final class MatchingValidation {
    private MatchingValidation() {}

    static List<ValidationError> validate(MatchingInput input) {
        var errors = new ArrayList<ValidationError>();
        if (input == null) {
            error(errors, "input", ValidationCode.REQUIRED, "Provide profile and opportunity facts.");
            return errors;
        }
        var p = input.profile();
        var o = input.opportunity();
        if (p == null) error(errors, "profile", ValidationCode.REQUIRED, "Provide a profile snapshot.");
        else {
            terms(errors, "profile.technologies", p.technologies(), 20);
            range(errors, "profile.totalYears", p.totalYears(), 0, 60);
            range(errors, "profile.maxOfficeDays", p.maxOfficeDays(), 0, 5);
            place(errors, "profile.residence", p.residence());
        }
        if (o == null) error(errors, "opportunity", ValidationCode.REQUIRED, "Provide opportunity facts.");
        else {
            terms(errors, "opportunity.technologies", o.technologies(), 100);
            range(errors, "opportunity.officeDays", o.officeDays(), 0, 5);
            place(errors, "opportunity.office", o.office());
            if (o.mode() == WorkMode.REMOTE && o.officeDays() != null && o.officeDays() > 0) {
                error(errors, "opportunity.officeDays", ValidationCode.CONTRADICTORY,
                        "Remote work cannot also require regular weekly office attendance.");
            }
            if (o.experience() != null) {
                range(errors, "opportunity.experience.minimumYears", o.experience().minimumYears(), 0, 60);
                if (o.experience().subject() != null && (o.experience().subject().isBlank()
                        || o.experience().subject().length() > 80)) {
                    error(errors, "opportunity.experience.subject", ValidationCode.INVALID_VALUE,
                            "Use a nonblank experience subject of at most 80 characters, or null.");
                }
                if (o.experience().scope() == ExperienceScope.TOTAL && o.experience().subject() != null) {
                    error(errors, "opportunity.experience.subject", ValidationCode.CONTRADICTORY,
                            "Total experience must not carry a role or technology subject.");
                }
            }
            policy(errors, o.residencePolicy());
        }
        return errors;
    }

    private static void range(List<ValidationError> errors, String field, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            error(errors, field, ValidationCode.OUT_OF_RANGE,
                    "Use a value from " + min + " to " + max + ", or null when unavailable.");
        }
    }

    private static void terms(List<ValidationError> errors, String field, List<String> values, int max) {
        if (values == null) return;
        if (values.size() > max) {
            error(errors, field, ValidationCode.TOO_MANY_VALUES, "Use at most " + max + " terms.");
        }
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            if (value == null || value.replaceAll("(?U)\\s+", " ").isBlank() || value.length() > 80) {
                error(errors, field + "[" + i + "]", ValidationCode.INVALID_VALUE,
                        "Each supplied term must contain 1–80 characters and not be blank.");
            }
        }
    }

    private static void place(List<ValidationError> errors, String field, Place place) {
        if (place == null) return;
        country(errors, field + ".country", place.country());
        if (place.city() != null && (place.city().isBlank() || place.city().length() > 120)) {
            error(errors, field + ".city", ValidationCode.INVALID_VALUE,
                    "Use a resolved city identifier of 1–120 characters, or null.");
        }
    }

    private static void country(List<ValidationError> errors, String field, String value) {
        if (value != null && !value.matches("[A-Z]{2}")) {
            error(errors, field, ValidationCode.INVALID_VALUE,
                    "Use an uppercase two-letter country identifier, or null when unresolved.");
        }
    }

    private static void policy(List<ValidationError> errors, ResidencePolicy policy) {
        if (policy == null) return;
        var values = policy.countries();
        if (policy.scope() == ResidenceScope.COUNTRIES) {
            if (values == null || values.isEmpty()) {
                error(errors, "opportunity.residencePolicy.countries", ValidationCode.REQUIRED,
                        "An explicit restricted policy needs at least one country.");
                return;
            }
            if (values.size() > 250) {
                error(errors, "opportunity.residencePolicy.countries", ValidationCode.TOO_MANY_VALUES,
                        "Use at most 250 country identifiers.");
            }
            for (int i = 0; i < values.size(); i++) {
                String field = "opportunity.residencePolicy.countries[" + i + "]";
                if (values.get(i) == null) error(errors, field, ValidationCode.INVALID_VALUE,
                        "Country lists cannot contain null.");
                else country(errors, field, values.get(i));
            }
        } else if (values != null && !values.isEmpty()) {
            error(errors, "opportunity.residencePolicy", ValidationCode.CONTRADICTORY,
                    "A country list requires an explicit COUNTRIES policy.");
        }
    }

    private static void error(List<ValidationError> errors, String field, ValidationCode code, String message) {
        errors.add(new ValidationError(field, code, message));
    }
}
