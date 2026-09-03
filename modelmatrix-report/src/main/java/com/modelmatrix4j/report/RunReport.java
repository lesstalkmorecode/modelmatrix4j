package com.modelmatrix4j.report;

import java.util.Objects;

/**
 * Persistence-safe facts for one run. Model output and diagnostics are intentionally absent.
 *
 * @param runId stable run identity
 * @param scenarioId scenario identifier
 * @param configurationId model configuration identifier
 * @param repetition zero-based repetition index
 * @param status schema-local terminal run status
 * @param durationNanos measured run duration in nanoseconds
 */
public record RunReport(
        String runId,
        String scenarioId,
        String configurationId,
        int repetition,
        ReportRunStatus status,
        long durationNanos) {

    /**
     * @throws IllegalArgumentException if an identifier is blank, repetition is negative, or
     *         {@code durationNanos} is negative
     */
    public RunReport {
        runId = requireText(runId, "runId");
        scenarioId = requireText(scenarioId, "scenarioId");
        configurationId = requireText(configurationId, "configurationId");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be zero or greater");
        }
        Objects.requireNonNull(status, "status");
        if (durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
