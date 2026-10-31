package io.github.garatech.jobsinton.matching.api;

import java.util.List;
import java.util.Map;

import static io.github.garatech.jobsinton.matching.MatchingInput.*;

/** Explicit fictional facts; display locations do not establish residence policies. */
final class FictionalMatchingCatalog {
    private FictionalMatchingCatalog() {}
    private static final Map<String, Opportunity> OFFERS = Map.of(
            "backend-java", offer(Role.BACKEND, WorkMode.REMOTE, 0, "madrid",
                    List.of("Java", "Spring Boot", "PostgreSQL"), 2, ExperienceScope.TECHNOLOGY, "Java"),
            "frontend-react", offer(Role.FRONTEND, WorkMode.REMOTE, 0, "madrid",
                    List.of("React", "TypeScript", "CSS"), 2, ExperienceScope.ROLE, "Frontend development"),
            "data-engineer", offer(Role.DATA, WorkMode.HYBRID, 2, "valencia",
                    List.of("Python", "SQL", "dbt"), 2, ExperienceScope.UNKNOWN, null),
            "fullstack", offer(Role.FULL_STACK, WorkMode.HYBRID, 2, "valencia",
                    List.of("TypeScript", "React", "Node.js"), 2, ExperienceScope.UNKNOWN, null),
            "platform-engineer", offer(Role.PLATFORM, WorkMode.REMOTE, 0, "barcelona",
                    List.of("Kubernetes", "Terraform", "Linux"), 4, ExperienceScope.ROLE, "Infrastructure or platform work"),
            "sap-developer", offer(Role.SAP, WorkMode.ONSITE, 5, "barcelona",
                    List.of("ABAP", "SAP S/4HANA", "SQL"), 1, ExperienceScope.TECHNOLOGY, "ABAP"));

    static Opportunity find(String id) { return OFFERS.get(id); }

    private static Opportunity offer(Role role, WorkMode mode, int days, String city,
                                     List<String> technologies, int years, ExperienceScope scope, String subject) {
        return new Opportunity(role, technologies, new Experience(years, scope, subject), mode, days,
                new Place("ES", city), new ResidencePolicy(ResidenceScope.UNKNOWN, null));
    }
}
