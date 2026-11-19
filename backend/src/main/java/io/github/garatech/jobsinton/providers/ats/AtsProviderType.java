package io.github.garatech.jobsinton.providers.ats;

import java.util.Locale;

/** Supported public Applicant Tracking System (ATS) job board providers across JSON, XML, RSS, and HTML endpoints. */
public enum AtsProviderType {
    GREENHOUSE("greenhouse", "Greenhouse"),
    LEVER("lever", "Lever"),
    SMARTRECRUITERS("smartrecruiters", "SmartRecruiters"),
    WORKABLE("workable", "Workable"),
    ASHBY("ashby", "Ashby"),
    PERSONIO("personio", "Personio"),
    RECRUITEE("recruitee", "Recruitee"),
    TEAMTAILOR("teamtailor", "Teamtailor"),
    FACTORIAL("factorial", "Factorial HR");

    private final String id;
    private final String displayName;

    AtsProviderType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static AtsProviderType fromId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ATS provider id must not be blank");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        for (AtsProviderType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported ATS provider: " + value);
    }
}
