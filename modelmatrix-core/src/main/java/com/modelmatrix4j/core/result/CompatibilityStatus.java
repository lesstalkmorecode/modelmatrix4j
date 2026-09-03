package com.modelmatrix4j.core.result;

/** Overall compatibility classification for a completed matrix evaluation. */
public enum CompatibilityStatus {
    /** All comparable completed runs agree. */
    COMPATIBLE,
    /** Comparable completed runs disagree behaviorally. */
    MISMATCH,
    /** At least one required configuration was unavailable and no stronger execution failure occurred. */
    UNAVAILABLE,
    /** At least one run failed, timed out, or was cancelled. */
    EXECUTION_FAILURE
}
