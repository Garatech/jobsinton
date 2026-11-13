package io.github.garatech.jobsinton.opportunities;

import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;

/** Source metadata for a listing, separate from evidence usable by the matching engine. */
public record OpportunityListing(String id, String title, URI referral, String company,
        String locationLabel, Instant createdAt, String excerpt, String categoryLabel,
        String contractType, String contractTime, Salary salary, Opportunity matchingFacts) {

    /** Provider amounts only: currency, pay period and gross/net basis have not been established. */
    public record Salary(BigDecimal minimum, BigDecimal maximum, SalaryOrigin origin) {}
    public enum SalaryOrigin { ADVERTISED, PREDICTED, UNSPECIFIED }
}
