package com.modelmatrix4j.rag;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void retrievedDocumentRejectsNullStableIdentity() {
        assertThrows(NullPointerException.class, () -> new RetrievedDocument(null));
    }

    @Test
    void retrievalInvocationRequiresConsistentEvidence() {
        RetrievedDocument document = new RetrievedDocument("doc");

        assertThrows(NullPointerException.class,
                () -> new RetrievalInvocation(null, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalInvocation("output", null, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalInvocation("output", RetrievalInvocation.EvidenceStatus.VALID, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalInvocation(
                        "output", RetrievalInvocation.EvidenceStatus.INVALID, List.of(document)));
    }

    @Test
    void retrievalModelAndObservationRejectMissingComponents() {
        assertThrows(NullPointerException.class,
                () -> new RetrievalModel(null, scenario -> RetrievalInvocation.invalidEvidence("output")));
        assertThrows(NullPointerException.class,
                () -> new RetrievalModel(new ModelDescriptor("model"), null));
        assertThrows(NullPointerException.class,
                () -> new RetrievalObservation(null, "model", 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalObservation("run", null, 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalObservation(
                        "run", "model", 0, (RetrievalInvocation.EvidenceStatus) null, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalObservation("run", "model", 0, (List<RetrievedDocument>) null));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalObservation(
                        "run", "model", 0, RetrievalInvocation.EvidenceStatus.INVALID,
                        List.of(new RetrievedDocument("doc"))));
    }

    @Test
    void retrievalResultRequiresConsistentNonEmptySummaries() {
        RetrievalResult.Observation valid = new RetrievalResult.Observation(
                "run", "model", 0, RetrievalInvocation.EvidenceStatus.VALID, 0);
        RetrievalResult.Observation invalid = new RetrievalResult.Observation(
                "run", "model", 0, RetrievalInvocation.EvidenceStatus.INVALID, 0);

        assertThrows(NullPointerException.class,
                () -> new RetrievalResult(null, List.of(valid)));
        assertThrows(NullPointerException.class,
                () -> new RetrievalResult(RetrievalResult.Status.COMPATIBLE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult(RetrievalResult.Status.COMPATIBLE, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalResult.Observation(
                        null, "model", 0, RetrievalInvocation.EvidenceStatus.VALID, 0));
        assertThrows(NullPointerException.class,
                () -> new RetrievalResult.Observation(
                        "run", null, 0, RetrievalInvocation.EvidenceStatus.VALID, 0));
        assertThrows(NullPointerException.class,
                () -> new RetrievalResult.Observation("run", "model", 0, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult.Observation(
                        "run", "model", -1, RetrievalInvocation.EvidenceStatus.VALID, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult.Observation(
                        "run", "model", 0, RetrievalInvocation.EvidenceStatus.VALID, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult.Observation(
                        "run", "model", 0, RetrievalInvocation.EvidenceStatus.INVALID, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult(RetrievalResult.Status.INVALID, List.of(valid)));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult(RetrievalResult.Status.COMPATIBLE, List.of(invalid)));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalResult(RetrievalResult.Status.MISMATCH, List.of(valid)));
    }

    @Test
    void publicRetrievalOperationsRejectInvalidProgrammingInputs() {
        RetrievalComparator comparator = new RetrievalComparator();
        RetrievalEvaluator evaluator = new RetrievalEvaluator();
        RetrievalObservation observation = new RetrievalObservation("run", "model", 0, List.of());

        assertThrows(NullPointerException.class, () -> comparator.compare(null, observation));
        assertThrows(NullPointerException.class, () -> comparator.compare(observation, null));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(null));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of()));
    }

    @Test
    void executionPreparationRejectsMissingOrEmptyModelsAndResult() {
        RetrievalModel model = new RetrievalModel(
                new ModelDescriptor("model"), scenario -> new RetrievalInvocation("output", List.of()));

        assertThrows(NullPointerException.class, () -> RetrievalExecution.prepare(null));
        assertThrows(IllegalArgumentException.class, () -> RetrievalExecution.prepare(List.of()));
        assertThrows(NullPointerException.class,
                () -> RetrievalExecution.prepare(java.util.Arrays.asList(model, null)));
        assertThrows(NullPointerException.class,
                () -> RetrievalExecution.prepare(List.of(model)).observations(null));
    }
}
