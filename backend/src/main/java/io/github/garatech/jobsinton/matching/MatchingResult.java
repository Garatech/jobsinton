package io.github.garatech.jobsinton.matching;

import java.util.List;

/** A failed validation never carries a partial compatibility result. */
public sealed interface MatchingResult permits MatchingResult.Evaluation, MatchingResult.Invalid {
    enum Outcome { ALIGNED, DIFFERENT, UNKNOWN, NOT_ASSESSED }
    enum Kind { ADVISORY, CONSTRAINT }
    enum Dimension {
        ROLE, TECHNOLOGY, EXPERIENCE, WORK_MODE, ATTENDANCE, OFFICE_LOCATION,
        RESIDENCE, SALARY, OTHER_CONDITIONS
    }
    enum Summary { CONFLICT, NEEDS_INFORMATION, NO_KNOWN_CONFLICT, NOT_ASSESSED }
    enum Reason {
        ROLE_ALIGNED, ROLE_DIFFERENT, ROLE_UNKNOWN,
        TECHNOLOGY_CONFIRMED, TECHNOLOGY_UNCONFIRMED, TECHNOLOGIES_UNKNOWN,
        EXPERIENCE_SUFFICIENT, EXPERIENCE_BELOW_MINIMUM, EXPERIENCE_UNKNOWN,
        EXPERIENCE_SCOPE_UNCONFIRMED,
        MODE_ALIGNED, MODE_DIFFERENT, MODE_UNKNOWN,
        ATTENDANCE_WITHIN_LIMIT, ATTENDANCE_EXCEEDS_LIMIT, ATTENDANCE_UNKNOWN,
        ATTENDANCE_NOT_CONFIGURED,
        OFFICE_ALIGNED, OFFICE_DIFFERENT, OFFICE_UNKNOWN, OFFICE_NOT_APPLICABLE,
        RESIDENCE_ALLOWED, RESIDENCE_OUTSIDE_POLICY, RESIDENCE_UNRESTRICTED,
        RESIDENCE_POLICY_UNKNOWN, RESIDENCE_COUNTRY_UNKNOWN,
        SALARY_NOT_CONFIGURED, CONDITIONS_NOT_SUPPORTED
    }
    enum ValidationCode { REQUIRED, OUT_OF_RANGE, INVALID_VALUE, TOO_MANY_VALUES, CONTRADICTORY }

    record Evidence(List<String> profileValues, List<String> opportunityValues, String limitation) {
        public Evidence {
            profileValues = List.copyOf(profileValues);
            opportunityValues = List.copyOf(opportunityValues);
        }
    }

    record Assessment(
            String criterion, Dimension dimension, Kind kind, Outcome outcome,
            Reason reason, String explanation, Evidence evidence) {}

    record Evaluation(String rulesVersion, List<Assessment> assessments, Summary constraintSummary)
            implements MatchingResult {
        public Evaluation {
            assessments = List.copyOf(assessments);
        }
    }

    record ValidationError(String field, ValidationCode code, String message) {}

    record Invalid(List<ValidationError> errors) implements MatchingResult {
        public Invalid {
            errors = List.copyOf(errors);
            if (errors.isEmpty()) throw new IllegalArgumentException("Validation errors are required.");
        }
    }
}
