package com.modelmatrix4j.report;

import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes compact JSON for the durable report contract. Only fields present in
 * {@link CompatibilityReport} are serialized; model output, diagnostics, provider payloads, and
 * capability-local evidence are outside this projection.
 */
public final class JsonReportWriter {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public JsonReportWriter() {
    }

    /** @throws IllegalStateException if the report cannot be serialized */
    public String write(CompatibilityReport report) {
        Objects.requireNonNull(report, "report");
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JacksonException exception) {
            throw new IllegalStateException("failed to serialize compatibility report", exception);
        }
    }
}
