package com.modelmatrix4j.report;

/** Stable schema-1 run vocabulary, intentionally independent of core enums. */
public enum ReportRunStatus {
    /** Invocation completed successfully. */
    COMPLETED,
    /** Invocation terminated with a non-unavailability execution failure. */
    FAILED,
    /** Configured model/provider was unavailable. */
    UNAVAILABLE,
    /** Invocation exceeded its timeout budget. */
    TIMED_OUT,
    /** Invocation was cancelled or interrupted. */
    CANCELLED
}
