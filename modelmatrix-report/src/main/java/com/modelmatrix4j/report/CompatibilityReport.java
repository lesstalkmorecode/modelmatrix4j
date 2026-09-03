package com.modelmatrix4j.report;

import java.util.List;
import java.util.Objects;

/**
 * Versioned, persistence-safe projection of a compatibility result.
 *
 * <p>This durable contract intentionally excludes model output, diagnostics, raw provider payloads,
 * and capability-local evidence. Its schema version evolves independently from the Java library
 * version.</p>
 *
 * @param schemaVersion durable report schema version; currently {@value #CURRENT_SCHEMA_VERSION}
 * @param status schema-local overall compatibility status
 * @param runs persistence-safe run facts in core result order
 */
public record CompatibilityReport(String schemaVersion, ReportCompatibilityStatus status, List<RunReport> runs) {
    /** Current durable report schema emitted by this library. */
    public static final String CURRENT_SCHEMA_VERSION = "1";

    /**
     * @throws IllegalArgumentException if {@code schemaVersion} is unsupported or {@code runs} is empty
     */
    public CompatibilityReport {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        if (!CURRENT_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported report schema version: " + schemaVersion);
        }
        Objects.requireNonNull(status, "status");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs must not be empty");
        }
    }
}
