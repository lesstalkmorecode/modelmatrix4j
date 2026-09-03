package com.modelmatrix4j.tool;

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
 * Captures tool-call evidence while core owns physical model execution. Each wrapper invocation
 * delegates exactly once to the underlying adapter.
 */
public final class ToolExecution {
    private ToolExecution() {
    }

    /**
     * Prepares tool-aware models for core execution while retaining tool evidence locally.
     *
     * @param models ordered tool-aware model configurations
     * @return wrapped core models and their one-shot evidence handle
     * @throws NullPointerException if the list or a model is {@code null}
     * @throws IllegalArgumentException if the list is empty
     */
    public static PreparedModels prepare(List<ToolModel> models) {
        models = List.copyOf(Objects.requireNonNull(models, "models"));
        if (models.isEmpty()) {
            throw new IllegalArgumentException("models must not be empty");
        }

        EvidenceStore evidence = new EvidenceStore();
        List<ModelUnderTest> wrapped = new ArrayList<>(models.size());
        for (ToolModel model : models) {
            Objects.requireNonNull(model, "model");
            String configurationId = model.descriptor().configurationId();
            wrapped.add(new ModelUnderTest(model.descriptor(), evidence.wrap(configurationId, model.adapter())));
        }
        return new PreparedModels(wrapped, evidence);
    }

    /** Core model targets paired with one-shot capability-local tool evidence. */
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
         * Correlates completed core runs with tool evidence from the same physical invocations.
         * Non-completed runs do not produce observations, and evidence can be consumed only once.
         *
         * @param coreResult result produced by executing {@link #models()}
         * @return tool observations in core run order
         * @throws NullPointerException if {@code coreResult} is {@code null}
         * @throws IllegalStateException if evidence was already consumed or required evidence is missing
         */
        public synchronized List<ToolObservation> observations(CompatibilityResult coreResult) {
            Objects.requireNonNull(coreResult, "coreResult");
            if (consumed) {
                throw new IllegalStateException("tool evidence has already been consumed");
            }
            consumed = true;
            evidence.close();

            List<ToolObservation> observations = new ArrayList<>();
            for (RunResult run : coreResult.runs()) {
                if (run.status() != RunStatus.COMPLETED) {
                    continue;
                }
                String configurationId = run.model().configurationId();
                List<ToolCallObservation> calls = evidence.remove(configurationId, run.repetition());
                if (calls == null) {
                    throw new IllegalStateException("missing tool evidence for run " + run.runId());
                }
                observations.add(new ToolObservation(
                        run.runId(), configurationId, run.repetition(), calls));
            }
            evidence.clear();
            return List.copyOf(observations);
        }
    }

    private static final class EvidenceStore {
        private final Map<String, AtomicInteger> nextInvocation = new ConcurrentHashMap<>();
        private final Map<Key, List<ToolCallObservation>> evidence = new ConcurrentHashMap<>();
        private boolean accepting = true;

        private ModelAdapter wrap(String configurationId, ToolAdapter delegate) {
            Objects.requireNonNull(delegate, "delegate");
            return scenario -> {
                int invocationIndex = nextInvocation
                        .computeIfAbsent(configurationId, ignored -> new AtomicInteger())
                        .getAndIncrement();
                ToolInvocation invocation = Objects.requireNonNull(delegate.invoke(scenario), "invocation");
                publish(new Key(configurationId, invocationIndex), invocation.calls());
                return invocation.output();
            };
        }

        private synchronized void publish(Key key, List<ToolCallObservation> calls) {
            if (accepting) {
                evidence.put(key, calls);
            }
        }

        private List<ToolCallObservation> remove(String configurationId, int repetition) {
            return evidence.remove(new Key(configurationId, repetition));
        }

        private synchronized void close() {
            accepting = false;
        }

        private void clear() {
            evidence.clear();
            nextInvocation.clear();
        }
    }

    private record Key(String configurationId, int invocationIndex) {
    }
}
