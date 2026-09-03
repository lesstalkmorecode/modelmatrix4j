package com.modelmatrix4j.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RetrievalEvaluatorTest {

    private final RetrievalEvaluator evaluator = new RetrievalEvaluator();

    @Test
    void sameOrderedDocumentIdsAreCompatible() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1", "doc-2"),
                observation("b0", "b", 0, "doc-1", "doc-2")));
        assertEquals(RetrievalResult.Status.COMPATIBLE, result.status());
    }

    @Test
    void differentDocumentIdsAreMismatch() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("b0", "b", 0, "doc-2")));
        assertEquals(RetrievalResult.Status.MISMATCH, result.status());
    }

    @Test
    void differentOrderIsMismatch() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1", "doc-2"),
                observation("b0", "b", 0, "doc-2", "doc-1")));
        assertEquals(RetrievalResult.Status.MISMATCH, result.status());
    }

    @Test
    void emptyResultsCompareSemantically() {
        assertEquals(RetrievalResult.Status.COMPATIBLE, evaluator.evaluate(List.of(
                observation("a0", "a", 0), observation("b0", "b", 0))).status());
        assertEquals(RetrievalResult.Status.MISMATCH, evaluator.evaluate(List.of(
                observation("a0", "a", 0), observation("b0", "b", 0, "doc-1"))).status());
    }

    @Test
    void citationDifferencesDoNotAffectDefaultCompatibility() {
        RetrievalObservation a = new RetrievalObservation("a0", "a", 0,
                List.of(new RetrievedDocument("doc-1", Optional.of("manual.pdf#p12"))));
        RetrievalObservation b = new RetrievalObservation("b0", "b", 0,
                List.of(new RetrievedDocument("doc-1", Optional.of("manual.pdf?page=12"))));
        assertEquals(RetrievalResult.Status.COMPATIBLE, evaluator.evaluate(List.of(a, b)).status());
    }

    @Test
    void comparesConfigurationsWithinTheSameRepetition() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("b0", "b", 0, "doc-1"),
                observation("a1", "a", 1, "doc-2"), observation("b1", "b", 1, "doc-2")));
        assertEquals(RetrievalResult.Status.COMPATIBLE, result.status());
    }

    @Test
    void mismatchInAnyRepetitionMakesResultMismatch() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("b0", "b", 0, "doc-1"),
                observation("a1", "a", 1, "doc-2"), observation("b1", "b", 1, "doc-3")));
        assertEquals(RetrievalResult.Status.MISMATCH, result.status());
    }

    @Test
    void invalidEvidenceIsNotBehavioralMismatch() {
        RetrievalObservation invalid = invalid("b0", "b", 0);
        RetrievalResult result = evaluator.evaluate(List.of(observation("a0", "a", 0, "doc-1"), invalid));
        assertEquals(RetrievalResult.Status.INVALID, result.status());
        assertEquals(RetrievalInvocation.EvidenceStatus.INVALID, result.observations().get(1).evidenceStatus());
    }

    @Test
    void invalidEvidenceTakesPrecedenceOverMismatchAcrossRepetitions() {
        RetrievalResult result = evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("b0", "b", 0, "doc-2"),
                observation("a1", "a", 1, "doc-1"), invalid("b1", "b", 1)));
        assertEquals(RetrievalResult.Status.INVALID, result.status());
    }

    @Test
    void rejectsSingleConfigurationCompatibility() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("a1", "a", 1, "doc-2"))));
    }

    @Test
    void rejectsDuplicateConfigurationRepetitionPairs() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("a0-duplicate", "a", 0, "doc-1"))));
    }

    @Test
    void rejectsDifferentConfigurationSetsAcrossRepetitions() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(
                observation("a0", "a", 0, "doc-1"), observation("b0", "b", 0, "doc-1"),
                observation("a1", "a", 1, "doc-2"))));
    }

    private static RetrievalObservation observation(
            String runId, String configurationId, int repetition, String... documentIds) {
        return new RetrievalObservation(runId, configurationId, repetition,
                java.util.Arrays.stream(documentIds).map(RetrievedDocument::new).toList());
    }

    private static RetrievalObservation invalid(String runId, String configurationId, int repetition) {
        return new RetrievalObservation(
                runId, configurationId, repetition, RetrievalInvocation.EvidenceStatus.INVALID, List.of());
    }
}
