package com.modelmatrix4j.core.result;

/** Terminal lifecycle status of one model/repetition run. */
public enum RunStatus {
    /** Invocation completed successfully and may contain application-visible output. */
    COMPLETED,
    /** Invocation terminated with a non-unavailability execution failure. */
    FAILED,
    /** Adapter reported that the configured model/provider was unavailable. */
    UNAVAILABLE,
    /** Invocation did not complete within its timeout budget. */
    TIMED_OUT,
    /** Invocation was cancelled or interrupted before successful completion. */
    CANCELLED
}
