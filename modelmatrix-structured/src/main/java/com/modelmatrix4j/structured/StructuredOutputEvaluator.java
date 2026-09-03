package com.modelmatrix4j.structured;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Evaluates structured observations within the same repetition across configurations. */
public final class StructuredOutputEvaluator {

    private final JsonValueComparator comparator = new JsonValueComparator();

    /**
     * Validates every observation against the schema and compares valid JSON within each repetition.
     *
     * <p>Every repetition must contain the same configuration set. Invalid schema evidence takes
     * precedence over semantic mismatch. Repetitions are independent: output may vary between
     * repetitions as long as configurations agree within each repetition.</p>
     *
     * @param outputs structured payloads correlated to completed core runs
     * @param schema schema required of every payload
     * @return compatibility status and per-run validation summaries
     * @throws NullPointerException if an argument or observation is {@code null}
     * @throws IllegalArgumentException if observations are empty, duplicated by
     *         configuration/repetition, or repetitions contain different configuration sets
     */
    public StructuredOutputResult evaluate(
            List<StructuredOutputObservation> outputs,
            JsonObjectSchema schema
    ) {
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        Objects.requireNonNull(schema, "schema");
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("outputs must not be empty");
        }

        Map<Integer, List<StructuredOutputObservation>> byRepetition = new LinkedHashMap<>();
        Set<ObservationKey> seen = new HashSet<>();
        List<StructuredOutputResult.Observation> observations = new ArrayList<>(outputs.size());
        boolean invalid = false;

        for (StructuredOutputObservation output : outputs) {
            Objects.requireNonNull(output, "output");
            ObservationKey key = new ObservationKey(output.configurationId(), output.repetition());
            if (!seen.add(key)) {
                throw new IllegalArgumentException("duplicate configuration/repetition observation");
            }
            byRepetition.computeIfAbsent(output.repetition(), ignored -> new ArrayList<>()).add(output);

            JsonObjectSchema.Validation validation = schema.validate(output.output());
            observations.add(new StructuredOutputResult.Observation(
                    output.runId(),
                    output.configurationId(),
                    output.repetition(),
                    validation.valid(),
                    validation.diagnostic()
            ));
            invalid |= !validation.valid();
        }

        Set<String> expectedConfigurations = configurationIds(byRepetition.values().iterator().next());
        for (List<StructuredOutputObservation> repetition : byRepetition.values()) {
            if (!configurationIds(repetition).equals(expectedConfigurations)) {
                throw new IllegalArgumentException("each repetition must contain the same configuration set");
            }
        }

        if (invalid) {
            return new StructuredOutputResult(StructuredOutputResult.Status.INVALID, observations);
        }

        boolean mismatch = false;
        for (List<StructuredOutputObservation> repetition : byRepetition.values()) {
            String reference = repetition.getFirst().output();
            for (int index = 1; index < repetition.size(); index++) {
                if (comparator.compare(reference, repetition.get(index).output())
                        != JsonValueComparator.Outcome.EQUIVALENT) {
                    mismatch = true;
                }
            }
        }

        return new StructuredOutputResult(
                mismatch
                        ? StructuredOutputResult.Status.MISMATCH
                        : StructuredOutputResult.Status.COMPATIBLE,
                observations
        );
    }

    private static Set<String> configurationIds(List<StructuredOutputObservation> observations) {
        Set<String> ids = new LinkedHashSet<>();
        for (StructuredOutputObservation observation : observations) {
            ids.add(observation.configurationId());
        }
        return ids;
    }

    private record ObservationKey(String configurationId, int repetition) {
    }
}
