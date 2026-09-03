package com.modelmatrix4j.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FixedCorpusFixtureTest {

    private final FixedCorpusFixture fixture = new FixedCorpusFixture(List.of(
            new FixedCorpusFixture.CorpusDocument(
                    "java-virtual-threads",
                    "Java virtual threads simplify high throughput concurrent applications",
                    Optional.of("java-guide#virtual-threads")),
            new FixedCorpusFixture.CorpusDocument(
                    "spring-ai-tools",
                    "Spring AI supports Java tool calling for model applications",
                    Optional.of("spring-ai-guide#tools")),
            new FixedCorpusFixture.CorpusDocument(
                    "java-records",
                    "Java records provide concise immutable data carriers",
                    Optional.of("java-guide#records"))));

    @Test
    void ranksByDeterministicTokenOverlap() {
        RetrievalObservation observation = fixture.observe(
                "run-1", "baseline", 0, "java concurrent virtual threads");

        assertEquals(List.of("java-virtual-threads", "spring-ai-tools", "java-records"),
                observation.documents().stream().map(RetrievedDocument::documentId).toList());
    }

    @Test
    void resolvesEqualScoresByCorpusOrder() {
        RetrievalObservation observation = fixture.observe(
                "run-1", "baseline", 0, "java");

        assertEquals(List.of("java-virtual-threads", "spring-ai-tools", "java-records"),
                observation.documents().stream().map(RetrievedDocument::documentId).toList());
    }

    @Test
    void producesExplicitNoResultEvidence() {
        RetrievalObservation observation = fixture.observe(
                "run-2", "candidate", 1, "kubernetes ingress");

        assertEquals(List.of(), observation.documents());
    }

    @Test
    void carriesStableIdentityAndOptionalCitationFromCorpus() {
        RetrievalObservation observation = fixture.observe(
                "run-3", "candidate", 2, "spring tools");

        assertEquals("spring-ai-tools", observation.documents().getFirst().documentId());
        assertEquals(Optional.of("spring-ai-guide#tools"), observation.documents().getFirst().citation());
    }

    @Test
    void rejectsDuplicateLogicalDocumentIdentities() {
        assertThrows(IllegalArgumentException.class, () -> new FixedCorpusFixture(List.of(
                new FixedCorpusFixture.CorpusDocument("doc-1", "first"),
                new FixedCorpusFixture.CorpusDocument("doc-1", "second"))));
    }
}
