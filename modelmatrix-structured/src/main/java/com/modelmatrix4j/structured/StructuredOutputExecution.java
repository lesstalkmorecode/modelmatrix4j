package com.modelmatrix4j.structured;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures structured output as capability-local evidence while core owns physical model execution.
 * Each wrapper invocation delegates exactly once to the underlying adapter. Raw structured payloads
 * are not exposed through public core output.
 */
public final class StructuredOutputExecution {
    private static final String CORE_PLACEHOLDER = "[structured output captured]";

    private StructuredOutputExecution() {
    }

    /**
     * Prepares core model targets for structured-output evidence capture.
     *
     * @param models ordered core model targets
     * @return wrapped models and their one-shot evidence handle
     * @throws NullPointerException if the list or a model is {@code null}
     * @throws IllegalArgumentException if the list is empty
     */
    public static PreparedModels prepare(List<ModelUnderTest> models) {
        models = List.copyOf(Objects.requireNonNull(models, "models"));
        if (models.isEmpty()) {
            throw new IllegalArgumentException("models must not be empty");
        }

        EvidenceStore evidence = new EvidenceStore();
        List<ModelUnderTest> wrapped = new ArrayList<>(models.size());
        for (ModelUnderTest model : models) {
            Objects.requireNonNull(model, "model");
            String configurationId = model.descriptor().configurationId();
            wrapped.add(new ModelUnderTest(
                    model.descriptor(),
                    evidence.wrap(configurationId, model.adapter())
            ));
        }
        return new PreparedModels(wrapped, evidence);
    }

    /** Core model targets paired with one-shot structured-output evidence. */
    public static final class PreparedModels {
        private final List<ModelUnderTest> models;
        private final EvidenceStore evidence;
        private boolean consumed;

        private PreparedModels(List<ModelUnderTest> models, EvidenceStore evidence) {
            this.models = List.copyOf(models);
            this.evidence = evidence;
        }

        /** Returns the ordered models to execute with {@code ModelMatrix}. */
        public List<ModelUnderTest> models() {
            return models;
        }

        /**
         * Correlates completed core runs with raw evidence from the same physical invocations.
         * Non-completed runs do not produce observations, and evidence can be consumed only once.
         *
         * @param coreResult result produced by executing {@link #models()}
         * @return structured observations in core run order
         * @throws NullPointerException if {@code coreResult} is {@code null}
         * @throws IllegalStateException if evidence was already consumed or required evidence is missing
         */
        public synchronized List<StructuredOutputObservation> observations(
                CompatibilityResult coreResult
        ) {
            Objects.requireNonNull(coreResult, "coreResult");
            if (consumed) {
                throw new IllegalStateException("structured evidence has already been consumed");
            }
            consumed = true;
            evidence.close();

            List<StructuredOutputObservation> observations = new ArrayList<>();
            for (RunResult run : coreResult.runs()) {
                if (run.status() != RunStatus.COMPLETED) {
                    continue;
                }
                String configurationId = run.model().configurationId();
                String raw = evidence.remove(configurationId, run.repetition());
                if (raw == null) {
                    throw new IllegalStateException("missing structured evidence for run " + run.runId());
                }
                observations.add(new StructuredOutputObservation(
                        run.runId(), configurationId, run.repetition(), raw
                ));
            }
            evidence.clear();
            return List.copyOf(observations);
        }
    }

    private static final class EvidenceStore {
        private final Map<String, AtomicInteger> nextInvocation = new ConcurrentHashMap<>();
        private final Map<Key, String> payloads = new ConcurrentHashMap<>();
        private boolean accepting = true;

        private ModelAdapter wrap(String configurationId, ModelAdapter delegate) {
            Objects.requireNonNull(delegate, "delegate");
            return scenario -> {
                int invocationIndex = nextInvocation
                        .computeIfAbsent(configurationId, ignored -> new AtomicInteger())
                        .getAndIncrement();
                String raw = Objects.requireNonNull(delegate.invoke(scenario), "output");
                publish(new Key(configurationId, invocationIndex), raw);
                return CORE_PLACEHOLDER;
            };
        }

        private synchronized void publish(Key key, String raw) {
            if (accepting) {
                payloads.put(key, raw);
            }
        }

        private String remove(String configurationId, int invocationIndex) {
            return payloads.remove(new Key(configurationId, invocationIndex));
        }

        private synchronized void close() {
            accepting = false;
        }

        private void clear() {
            payloads.clear();
            nextInvocation.clear();
        }
    }

    private record Key(String configurationId, int invocationIndex) {
    }
}
