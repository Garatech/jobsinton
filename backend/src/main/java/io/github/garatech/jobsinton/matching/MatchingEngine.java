package io.github.garatech.jobsinton.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static io.github.garatech.jobsinton.matching.MatchingInput.*;
import static io.github.garatech.jobsinton.matching.MatchingResult.*;

/** Stateless domain logic: no Spring, I/O, persistence, scoring or provider formats. */
public final class MatchingEngine {
    public static final String RULES_VERSION = "matching-v1.0";

    public MatchingResult evaluate(MatchingInput input) {
        var errors = MatchingValidation.validate(input);
        if (!errors.isEmpty()) return new Invalid(errors);

        var p = input.profile();
        var o = input.opportunity();
        var assessments = new ArrayList<Assessment>();
        assessments.add(role(p, o));
        assessments.addAll(technologies(p, o));
        assessments.add(experience(p, o));
        assessments.add(mode(p, o));
        assessments.add(attendance(p, o));
        assessments.add(office(p, o));
        assessments.add(residence(p, o));
        assessments.add(assessment("salary", Dimension.SALARY, Kind.ADVISORY, Outcome.NOT_ASSESSED,
                Reason.SALARY_NOT_CONFIGURED, "No salary preference is configured.",
                evidence(null, null, "Salary amounts are not part of this comparison version.")));
        assessments.add(assessment("other-conditions", Dimension.OTHER_CONDITIONS, Kind.ADVISORY,
                Outcome.NOT_ASSESSED, Reason.CONDITIONS_NOT_SUPPORTED,
                "Additional employment conditions are not evaluated in this version.",
                evidence(null, null,
                        "Languages, education, contract, hours, travel, on-call duties, relocation and work authorization remain unevaluated.")));
        return new Evaluation(RULES_VERSION, assessments, summarize(assessments));
    }

    static Summary summarize(Collection<Assessment> assessments) {
        boolean evaluated = false;
        boolean unknown = false;
        for (var a : assessments) {
            if (a.kind() != Kind.CONSTRAINT || a.outcome() == Outcome.NOT_ASSESSED) continue;
            if (a.outcome() == Outcome.DIFFERENT) return Summary.CONFLICT;
            evaluated = true;
            unknown |= a.outcome() == Outcome.UNKNOWN;
        }
        if (unknown) return Summary.NEEDS_INFORMATION;
        return evaluated ? Summary.NO_KNOWN_CONFLICT : Summary.NOT_ASSESSED;
    }

    private Assessment role(Profile p, Opportunity o) {
        boolean known = p.role() != null && o.role() != null;
        boolean same = known && p.role() == o.role();
        return assessment("role", Dimension.ROLE, Kind.ADVISORY,
                !known ? Outcome.UNKNOWN : same ? Outcome.ALIGNED : Outcome.DIFFERENT,
                !known ? Reason.ROLE_UNKNOWN : same ? Reason.ROLE_ALIGNED : Reason.ROLE_DIFFERENT,
                !known ? "Role information is incomplete."
                        : same ? "The opportunity matches the target role." : "The opportunity has a different role.",
                evidence(p.role(), o.role(), "Role is a preference; related roles are not automatically equivalent."));
    }

    private List<Assessment> technologies(Profile p, Opportunity o) {
        var profile = terms(p.technologies());
        var listing = terms(o.technologies());
        if (listing.isEmpty()) {
            return List.of(assessment("technologies", Dimension.TECHNOLOGY, Kind.ADVISORY,
                    Outcome.UNKNOWN, Reason.TECHNOLOGIES_UNKNOWN,
                    "The opportunity does not supply technology terms.",
                    new Evidence(List.copyOf(profile.values()), List.of(),
                            "No technology overlap can be confirmed without listing terms.")));
        }
        var result = new ArrayList<Assessment>();
        for (var term : listing.entrySet()) {
            boolean present = profile.containsKey(term.getKey());
            result.add(assessment("technology:" + term.getKey(), Dimension.TECHNOLOGY, Kind.ADVISORY,
                    present ? Outcome.ALIGNED : Outcome.UNKNOWN,
                    present ? Reason.TECHNOLOGY_CONFIRMED : Reason.TECHNOLOGY_UNCONFIRMED,
                    present ? "This technology is listed in both the profile and opportunity."
                            : "This listing technology is not confirmed by the profile.",
                    new Evidence(present ? List.of(profile.get(term.getKey())) : List.copyOf(profile.values()),
                            List.of(term.getValue()),
                            "Terms do not establish proficiency, years of use or mandatory requirement status.")));
        }
        return result;
    }

    private Assessment experience(Profile p, Opportunity o) {
        var requirement = o.experience();
        var facts = new Evidence(values(p.totalYears()), requirement == null ? List.of() : List.of(
                "minimumYears=" + value(requirement.minimumYears()),
                "scope=" + value(requirement.scope()),
                "subject=" + value(requirement.subject())),
                "Profile years describe total experience, not time in a role or technology.");
        Outcome outcome;
        Reason reason;
        String explanation;
        if (requirement == null || requirement.minimumYears() == null || requirement.scope() == null
                || requirement.scope() == ExperienceScope.UNKNOWN || p.totalYears() == null) {
            outcome = Outcome.UNKNOWN;
            reason = Reason.EXPERIENCE_UNKNOWN;
            explanation = "Comparable experience information is incomplete.";
        } else if (requirement.scope() != ExperienceScope.TOTAL) {
            outcome = Outcome.UNKNOWN;
            reason = Reason.EXPERIENCE_SCOPE_UNCONFIRMED;
            explanation = "Total profile years cannot confirm the scoped experience requirement.";
        } else {
            boolean enough = p.totalYears() >= requirement.minimumYears();
            outcome = enough ? Outcome.ALIGNED : Outcome.DIFFERENT;
            reason = enough ? Reason.EXPERIENCE_SUFFICIENT : Reason.EXPERIENCE_BELOW_MINIMUM;
            explanation = enough ? "Declared total experience meets the advertised total minimum."
                    : "Declared total experience is below the advertised total minimum.";
        }
        return assessment("experience", Dimension.EXPERIENCE, Kind.ADVISORY, outcome, reason, explanation, facts);
    }

    private Assessment mode(Profile p, Opportunity o) {
        boolean known = p.preferredMode() != null && o.mode() != null;
        boolean same = known && p.preferredMode() == o.mode();
        return assessment("work-mode", Dimension.WORK_MODE, Kind.ADVISORY,
                !known ? Outcome.UNKNOWN : same ? Outcome.ALIGNED : Outcome.DIFFERENT,
                !known ? Reason.MODE_UNKNOWN : same ? Reason.MODE_ALIGNED : Reason.MODE_DIFFERENT,
                !known ? "Work-mode information is incomplete."
                        : same ? "The work mode matches the preference." : "The work mode differs from the preference.",
                evidence(p.preferredMode(), o.mode(), "Mode preference does not imply an attendance limit or geographic permission."));
    }

    private Assessment attendance(Profile p, Opportunity o) {
        Outcome outcome;
        Reason reason;
        String explanation;
        if (p.maxOfficeDays() == null) {
            outcome = Outcome.NOT_ASSESSED;
            reason = Reason.ATTENDANCE_NOT_CONFIGURED;
            explanation = "No explicit weekly office-day maximum is configured.";
        } else if (o.officeDays() == null) {
            outcome = Outcome.UNKNOWN;
            reason = Reason.ATTENDANCE_UNKNOWN;
            explanation = "The opportunity does not specify regular weekly office days.";
        } else {
            boolean within = o.officeDays() <= p.maxOfficeDays();
            outcome = within ? Outcome.ALIGNED : Outcome.DIFFERENT;
            reason = within ? Reason.ATTENDANCE_WITHIN_LIMIT : Reason.ATTENDANCE_EXCEEDS_LIMIT;
            explanation = within ? "Required office days are within the configured maximum."
                    : "Required office days exceed the configured maximum.";
        }
        return assessment("attendance", Dimension.ATTENDANCE, Kind.CONSTRAINT, outcome, reason, explanation,
                evidence(p.maxOfficeDays(), o.officeDays(), "Regular weekly attendance only; occasional travel is not evaluated."));
    }

    private Assessment office(Profile p, Opportunity o) {
        Outcome outcome;
        Reason reason;
        String explanation;
        if (o.mode() == WorkMode.REMOTE) {
            outcome = Outcome.NOT_ASSESSED;
            reason = Reason.OFFICE_NOT_APPLICABLE;
            explanation = "Office location is not compared for remote work.";
        } else if (o.mode() == null || !complete(p.residence()) || !complete(o.office())) {
            outcome = Outcome.UNKNOWN;
            reason = Reason.OFFICE_UNKNOWN;
            explanation = "Work mode or resolved office-location information is incomplete.";
        } else {
            boolean same = p.residence().equals(o.office());
            outcome = same ? Outcome.ALIGNED : Outcome.DIFFERENT;
            reason = same ? Reason.OFFICE_ALIGNED : Reason.OFFICE_DIFFERENT;
            explanation = same ? "The office matches the profile city and country."
                    : "The office is in a different city or country.";
        }
        return assessment("office-location", Dimension.OFFICE_LOCATION, Kind.ADVISORY, outcome, reason,
                explanation, new Evidence(placeValues(p.residence()), placeValues(o.office()),
                        "Resolved identifiers only; commuting and relocation are not inferred."));
    }

    private Assessment residence(Profile p, Opportunity o) {
        var policy = o.residencePolicy();
        String country = p.residence() == null ? null : p.residence().country();
        Outcome outcome;
        Reason reason;
        String explanation;
        List<String> policyValues = List.of();
        if (policy == null || policy.scope() == null || policy.scope() == ResidenceScope.UNKNOWN) {
            outcome = Outcome.UNKNOWN;
            reason = Reason.RESIDENCE_POLICY_UNKNOWN;
            explanation = "The opportunity does not specify an accepted-residence policy.";
        } else if (policy.scope() == ResidenceScope.UNRESTRICTED) {
            outcome = Outcome.ALIGNED;
            reason = Reason.RESIDENCE_UNRESTRICTED;
            explanation = "The listing explicitly states no country-of-residence restriction.";
            policyValues = List.of("UNRESTRICTED");
        } else {
            policyValues = policy.countries().stream().distinct().sorted().toList();
            if (country == null) {
                outcome = Outcome.UNKNOWN;
                reason = Reason.RESIDENCE_COUNTRY_UNKNOWN;
                explanation = "The profile country has not been resolved.";
            } else {
                boolean allowed = policy.countries().contains(country);
                outcome = allowed ? Outcome.ALIGNED : Outcome.DIFFERENT;
                reason = allowed ? Reason.RESIDENCE_ALLOWED : Reason.RESIDENCE_OUTSIDE_POLICY;
                explanation = allowed ? "The profile country is included in the advertised residence policy."
                        : "The profile country is outside the advertised residence policy.";
            }
        }
        return assessment("residence", Dimension.RESIDENCE, Kind.CONSTRAINT, outcome, reason, explanation,
                new Evidence(values(country), policyValues, "Residence does not establish nationality or work authorization."));
    }

    private static Map<String, String> terms(List<String> values) {
        var terms = new TreeMap<String, String>();
        if (values == null) return terms;
        for (String raw : values) {
            String term = raw.replaceAll("(?U)\\s+", " ").trim();
            String key = term.toLowerCase(Locale.ROOT);
            // A stable representative preserves supplied spelling without depending on order.
            terms.merge(key, term, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
        return terms;
    }

    private static boolean complete(Place place) {
        return place != null && place.country() != null && place.city() != null;
    }

    private static List<String> placeValues(Place place) {
        return place == null ? List.of() : List.of("country=" + value(place.country()), "city=" + value(place.city()));
    }

    private static Evidence evidence(Object profile, Object listing, String limitation) {
        return new Evidence(values(profile), values(listing), limitation);
    }

    private static List<String> values(Object value) {
        return value == null ? List.of() : List.of(value.toString());
    }

    private static String value(Object value) {
        return value == null ? "unknown" : value.toString();
    }

    private static Assessment assessment(String criterion, Dimension dimension, Kind kind, Outcome outcome,
                                         Reason reason, String explanation, Evidence evidence) {
        return new Assessment(criterion, dimension, kind, outcome, reason, explanation, evidence);
    }
}
