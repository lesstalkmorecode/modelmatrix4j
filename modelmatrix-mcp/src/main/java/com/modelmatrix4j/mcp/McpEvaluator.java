package com.modelmatrix4j.mcp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates MCP compatibility independently within each repetition. The first configuration in
 * observation order is the reference; invalid evidence takes precedence over mismatch.
 */
public final class McpEvaluator {
    private final McpComparator comparator;

    public McpEvaluator() {
        this(new McpComparator());
    }

    McpEvaluator(McpComparator comparator) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    /**
     * Requires at least two configurations and the same configuration set in every repetition.
     *
     * @param observations MCP observations to evaluate
     * @return compatibility status and safe per-run summaries
     * @throws NullPointerException if the list or an observation is {@code null}
     * @throws IllegalArgumentException if observations are empty, duplicated by configuration and
     *         repetition, contain fewer than two configurations, or repetitions have different
     *         configuration sets
     */
    public McpResult evaluate(List<McpObservation> observations) {
        Objects.requireNonNull(observations, "observations");
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("observations must not be empty");
        }

        Map<Integer, List<McpObservation>> byRepetition = new LinkedHashMap<>();
        Set<ObservationKey> seen = new HashSet<>();
        for (McpObservation observation : observations) {
            Objects.requireNonNull(observation, "observations must not contain null");
            ObservationKey key = new ObservationKey(observation.configurationId(), observation.repetition());
            if (!seen.add(key)) {
                throw new IllegalArgumentException("duplicate configuration/repetition observation");
            }
            byRepetition.computeIfAbsent(observation.repetition(), ignored -> new ArrayList<>()).add(observation);
        }

        Set<String> expectedConfigurations = configurationIds(byRepetition.values().iterator().next());
        if (expectedConfigurations.size() < 2) {
            throw new IllegalArgumentException("at least two configurations are required for MCP compatibility");
        }

        McpResult.Status overall = McpResult.Status.COMPATIBLE;
        for (List<McpObservation> repetition : byRepetition.values()) {
            if (!configurationIds(repetition).equals(expectedConfigurations)) {
                throw new IllegalArgumentException("each repetition must contain the same configuration set");
            }

            McpObservation reference = repetition.getFirst();
            for (int index = 1; index < repetition.size(); index++) {
                McpResult.Status status = comparator.compare(reference, repetition.get(index));
                if (status == McpResult.Status.INVALID) {
                    overall = McpResult.Status.INVALID;
                } else if (status == McpResult.Status.MISMATCH && overall != McpResult.Status.INVALID) {
                    overall = McpResult.Status.MISMATCH;
                }
            }
        }

        return new McpResult(overall, observations.stream().map(McpResult.ObservationSummary::from).toList());
    }

    private static Set<String> configurationIds(List<McpObservation> observations) {
        Set<String> ids = new LinkedHashSet<>();
        for (McpObservation observation : observations) {
            ids.add(observation.configurationId());
        }
        return ids;
    }

    private record ObservationKey(String configurationId, int repetition) {
    }
}
