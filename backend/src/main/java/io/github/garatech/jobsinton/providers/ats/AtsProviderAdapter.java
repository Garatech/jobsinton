package io.github.garatech.jobsinton.providers.ats;

import java.util.List;

/** Modular contract implemented by each public ATS JSON job-board adapter. */
public interface AtsProviderAdapter {
    AtsProviderType type();

    List<RawAtsJob> fetch(CompanyBoard board);
}
