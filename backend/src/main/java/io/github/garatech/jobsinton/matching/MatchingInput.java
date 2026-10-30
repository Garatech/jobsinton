package io.github.garatech.jobsinton.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provider-independent facts. Null means unknown, except a null profile attendance
 * maximum means not configured. Place identifiers must already be resolved.
 */
public record MatchingInput(Profile profile, Opportunity opportunity) {
    public enum Role { BACKEND, FRONTEND, FULL_STACK, SAP, DATA, PLATFORM }
    public enum WorkMode { REMOTE, HYBRID, ONSITE }
    public enum ExperienceScope { TOTAL, ROLE, TECHNOLOGY, UNKNOWN }
    public enum ResidenceScope { UNKNOWN, UNRESTRICTED, COUNTRIES }

    public record Place(String country, String city) {}

    public record Profile(
            Role role, List<String> technologies, Integer totalYears, Place residence,
            WorkMode preferredMode, Integer maxOfficeDays) {
        public Profile {
            technologies = snapshot(technologies);
        }
    }

    public record Experience(Integer minimumYears, ExperienceScope scope, String subject) {}

    public record ResidencePolicy(ResidenceScope scope, List<String> countries) {
        public ResidencePolicy {
            countries = snapshot(countries);
        }
    }

    public record Opportunity(
            Role role, List<String> technologies, Experience experience, WorkMode mode,
            Integer officeDays, Place office, ResidencePolicy residencePolicy) {
        public Opportunity {
            technologies = snapshot(technologies);
        }
    }

    // Keep malformed elements for the validation boundary, rather than throwing
    // from a transport-independent input constructor.
    private static <T> List<T> snapshot(List<T> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
