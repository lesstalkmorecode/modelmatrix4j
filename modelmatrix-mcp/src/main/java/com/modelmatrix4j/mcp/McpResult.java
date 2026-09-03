package com.modelmatrix4j.mcp;

import java.util.List;
import java.util.Objects;

/** Safe public MCP compatibility summary; tool identifiers and argument payloads are not included. */
public record McpResult(Status status, List<ObservationSummary> observations) {
    public McpResult {
        status = Objects.requireNonNull(status, "status");
        observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
    }

    public enum Status {
        /** Ordered tool identities and semantic arguments agree within every repetition. */
        COMPATIBLE,
        /** Valid MCP evidence differs within at least one repetition. */
        MISMATCH,
        /** At least one compared tool argument is invalid JSON. */
        INVALID
    }

    /** Per-run summary that excludes tool identifiers and argument payloads. */
    public record ObservationSummary(
            String runId,
            String configurationId,
            int repetition,
            int toolCount
    ) {
        static ObservationSummary from(McpObservation observation) {
            return new ObservationSummary(
                    observation.runId(),
                    observation.configurationId(),
                    observation.repetition(),
                    observation.tools().size());
        }
    }
}
