package io.github.garatech.jobsinton.providers.ats;

/** Checked-free exception indicating a single ATS board fetch failed, timed out, or returned unreadable data. */
public final class AtsFetchException extends RuntimeException {
    public AtsFetchException(String message) {
        super(message);
    }

    public AtsFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
