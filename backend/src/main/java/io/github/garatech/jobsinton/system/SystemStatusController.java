package io.github.garatech.jobsinton.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes a minimal public contract for verifying frontend-to-backend connectivity. */
@RestController
public class SystemStatusController {

    /**
     * Reports that this HTTP service can answer requests.
     * This is not a database, provider, or production readiness check.
     *
     * @return the stable application name and current service status
     */
    @GetMapping("/api/status")
    public SystemStatus status() {
        return new SystemStatus("JobSinton", "UP");
    }

    /** Public response; deliberately excludes machine details, versions, and credentials. */
    public record SystemStatus(String name, String status) {
    }
}
