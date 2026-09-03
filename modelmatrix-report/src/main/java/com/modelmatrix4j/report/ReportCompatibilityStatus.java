package com.modelmatrix4j.report;

/** Stable schema-1 compatibility vocabulary, intentionally independent of core enums. */
public enum ReportCompatibilityStatus {
    /** All comparable completed runs agree. */
    COMPATIBLE,
    /** Comparable completed runs disagree behaviorally. */
    MISMATCH,
    /** At least one required configuration was unavailable without a stronger execution failure. */
    UNAVAILABLE,
    /** At least one run failed, timed out, or was cancelled. */
    EXECUTION_FAILURE
}
