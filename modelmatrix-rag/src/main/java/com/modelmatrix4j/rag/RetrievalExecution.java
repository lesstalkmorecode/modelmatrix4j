package com.modelmatrix4j.rag;

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
 * Captures retrieval evidence while core owns physical model execution. Each wrapper invocation
 * delegates exactly once to the underlying adapter; evaluation does not trigger a second model call.
 */
public final class RetrievalExecution {
    private RetrievalExecution() {
    }

    /**
     * @param models ordered retrieval-aware configurations
     * @return core model targets and their one-shot retrieval evidence handle
     * @throws NullPointerException if the list or a model is {@code null}
     * @throws IllegalArgumentException if the list is empty
     */
    public static PreparedModels prepare(List<RetrievalModel> models) {
        models = List.copyOf(Objects.requireNonNull(models, "models"));
        if (models.isEmpty()) {
            throw new IllegalArgumentException("models must not be empty");
        }

        EvidenceStore evidence = new EvidenceStore();
        List<ModelUnderTest> wrapped = new ArrayList<>(models.size());
        for (RetrievalModel model : models) {
            Objects.requireNonNull(model, "model");
            String configurationId = model.descriptor().configurationId();
            wrapped.add(new ModelUnderTest(
                    model.descriptor(),
                    evidence.wrap(configurationId, model.adapter())));
        }
        return new PreparedModels(wrapped, evidence);
    }

    /** Core model targets paired with one-shot capability-local retrieval evidence. */
    public static final class PreparedModels {
        private final List<ModelUnderTest> models;
        private final EvidenceStore evidence;
        private boolean consumed;

        private PreparedModels(List<ModelUnderTest> models, EvidenceStore evidence) {
            this.models = List.copyOf(models);
            this.evidence = evidence;
        }

        public List<ModelUnderTest> models() {
            return models;
        }

        /**
         * Correlates only completed core runs with evidence from the same physical invocation.
         * Evidence can be consumed only once; late evidence is ignored after consumption begins.
         *
         * @throws NullPointerException if {@code coreResult} is {@code null}
         * @throws IllegalStateException if evidence was already consumed or required completed-run
         *         evidence is missing
         */
        public synchronized List<RetrievalObservation> observations(CompatibilityResult coreResult) {
            Objects.requireNonNull(coreResult, "coreResult");
            if (consumed) {
                throw new IllegalStateException("retrieval evidence has already been consumed");
            }
            consumed = true;
            evidence.close();

            List<RetrievalObservation> observations = new ArrayList<>();
            for (RunResult run : coreResult.runs()) {
                if (run.status() != RunStatus.COMPLETED) {
                    continue;
                }
                String configurationId = run.model().configurationId();
                CapturedEvidence captured = evidence.remove(configurationId, run.repetition());
                if (captured == null) {
                    throw new IllegalStateException("missing retrieval evidence for run " + run.runId());
                }
                observations.add(new RetrievalObservation(
                        run.runId(), configurationId, run.repetition(),
                        captured.status(), captured.documents()));
            }
            evidence.clear();
            return List.copyOf(observations);
        }
    }

    private static final class EvidenceStore {
        private final Map<String, AtomicInteger> nextInvocation = new ConcurrentHashMap<>();
        private final Map<Key, CapturedEvidence> evidence = new ConcurrentHashMap<>();
        private boolean accepting = true;

        private ModelAdapter wrap(String configurationId, RetrievalAdapter delegate) {
            Objects.requireNonNull(delegate, "delegate");
            return scenario -> {
                int invocationIndex = nextInvocation
                        .computeIfAbsent(configurationId, ignored -> new AtomicInteger())
                        .getAndIncrement();
                RetrievalInvocation invocation = Objects.requireNonNull(delegate.invoke(scenario), "invocation");
                publish(new Key(configurationId, invocationIndex),
                        new CapturedEvidence(invocation.evidenceStatus(), invocation.documents()));
                return invocation.output();
            };
        }

        private synchronized void publish(Key key, CapturedEvidence captured) {
            if (accepting) {
                evidence.put(key, captured);
            }
        }

        private CapturedEvidence remove(String configurationId, int repetition) {
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

    private record CapturedEvidence(
            RetrievalInvocation.EvidenceStatus status,
            List<RetrievedDocument> documents
    ) {
        private CapturedEvidence {
            Objects.requireNonNull(status, "status");
            documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        }
    }

    private record Key(String configurationId, int invocationIndex) {
    }
}
