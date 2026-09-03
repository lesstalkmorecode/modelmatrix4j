package com.modelmatrix4j.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Evaluates retrieval compatibility across configurations independently within each repetition. */
public final class RetrievalEvaluator {

    private final RetrievalComparator comparator = new RetrievalComparator();

    /**
     * Requires the same configuration set in every repetition and at least two configurations.
     * Invalid retrieval evidence takes precedence over semantic mismatch.
     *
     * @param inputs retrieval observations to evaluate
     * @return compatibility status and safe per-run summaries
     * @throws NullPointerException if the list or an observation is {@code null}
     * @throws IllegalArgumentException if observations are empty, duplicated by configuration and
     *         repetition, contain fewer than two configurations, or repetitions have different
     *         configuration sets
     */
    public RetrievalResult evaluate(List<RetrievalObservation> inputs) {
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }

        Map<Integer, LinkedHashMap<String, RetrievalObservation>> byRepetition = new LinkedHashMap<>();
        List<RetrievalResult.Observation> observations = new ArrayList<>(inputs.size());
        for (RetrievalObservation input : inputs) {
            Objects.requireNonNull(input, "input");
            LinkedHashMap<String, RetrievalObservation> configurations =
                    byRepetition.computeIfAbsent(input.repetition(), ignored -> new LinkedHashMap<>());
            if (configurations.putIfAbsent(input.configurationId(), input) != null) {
                throw new IllegalArgumentException(
                        "duplicate retrieval observation for configurationId=" + input.configurationId()
                                + ", repetition=" + input.repetition());
            }
            observations.add(new RetrievalResult.Observation(
                    input.runId(), input.configurationId(), input.repetition(),
                    input.evidenceStatus(), input.documents().size()));
        }

        Set<String> expectedConfigurations = null;
        boolean mismatch = false;
        boolean invalid = false;
        for (Map.Entry<Integer, LinkedHashMap<String, RetrievalObservation>> entry : byRepetition.entrySet()) {
            LinkedHashMap<String, RetrievalObservation> configurations = entry.getValue();
            Set<String> actualConfigurations = new LinkedHashSet<>(configurations.keySet());
            if (expectedConfigurations == null) {
                expectedConfigurations = actualConfigurations;
                if (expectedConfigurations.size() < 2) {
                    throw new IllegalArgumentException(
                            "at least two configurations are required for retrieval compatibility");
                }
            } else if (!expectedConfigurations.equals(actualConfigurations)) {
                throw new IllegalArgumentException(
                        "configuration set must be identical for every repetition");
            }

            List<RetrievalObservation> sameRepetition = List.copyOf(configurations.values());
            if (sameRepetition.stream().anyMatch(observation ->
                    observation.evidenceStatus() == RetrievalInvocation.EvidenceStatus.INVALID)) {
                invalid = true;
                continue;
            }
            RetrievalObservation reference = sameRepetition.getFirst();
            mismatch |= sameRepetition.stream()
                    .skip(1)
                    .anyMatch(candidate -> comparator.compare(reference, candidate)
                            == RetrievalComparator.Outcome.MISMATCH);
        }

        RetrievalResult.Status status = invalid
                ? RetrievalResult.Status.INVALID
                : mismatch ? RetrievalResult.Status.MISMATCH : RetrievalResult.Status.COMPATIBLE;
        return new RetrievalResult(status, observations);
    }
}
