package io.github.garatech.jobsinton.providers.ats;

/** Per-company ATS fetch diagnostics so partial upstream failures never break the job feed. */
public record BoardStatus(
        String companySlug,
        String companyName,
        String provider,
        String providerLabel,
        boolean ok,
        int jobCount,
        long durationMs,
        String error
) {}
