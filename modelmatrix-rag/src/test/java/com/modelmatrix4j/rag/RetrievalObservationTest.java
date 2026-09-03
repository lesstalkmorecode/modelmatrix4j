package com.modelmatrix4j.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RetrievalObservationTest {

    @Test
    void preservesOrderedDocumentsAndRunIdentity() {
        RetrievalObservation observation = new RetrievalObservation(
                "run-1",
                "baseline",
                2,
                List.of(
                        new RetrievedDocument("doc-a", Optional.of("source-a")),
                        new RetrievedDocument("doc-b")));

        assertEquals("run-1", observation.runId());
        assertEquals("baseline", observation.configurationId());
        assertEquals(2, observation.repetition());
        assertEquals(List.of("doc-a", "doc-b"),
                observation.documents().stream().map(RetrievedDocument::documentId).toList());
    }

    @Test
    void defensivelyCopiesDocuments() {
        List<RetrievedDocument> documents = new ArrayList<>();
        documents.add(new RetrievedDocument("doc-a"));

        RetrievalObservation observation = new RetrievalObservation("run-1", "candidate", 0, documents);
        documents.add(new RetrievedDocument("doc-b"));

        assertEquals(1, observation.documents().size());
        assertThrows(UnsupportedOperationException.class,
                () -> observation.documents().add(new RetrievedDocument("doc-c")));
    }

    @Test
    void acceptsNoResultAsEmptyOrderedEvidence() {
        RetrievalObservation observation = new RetrievalObservation("run-1", "candidate", 0, List.of());

        assertEquals(List.of(), observation.documents());
    }

    @Test
    void rejectsInvalidIdentityAndRepetition() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalObservation(" ", "candidate", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalObservation("run-1", " ", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievalObservation("run-1", "candidate", -1, List.of()));
        assertThrows(NullPointerException.class,
                () -> new RetrievalObservation("run-1", "candidate", 0, List.of((RetrievedDocument) null)));
    }

    @Test
    void retrievedDocumentRequiresStableIdentityAndValidOptionalCitation() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievedDocument(" "));
        assertThrows(NullPointerException.class, () -> new RetrievedDocument("doc-a", null));
        assertThrows(IllegalArgumentException.class,
                () -> new RetrievedDocument("doc-a", Optional.of(" ")));
    }

    @Test
    void toStringDoesNotExposeCitationEvidence() {
        RetrievedDocument document = new RetrievedDocument("doc-a", Optional.of("secret-source"));
        RetrievalObservation observation = new RetrievalObservation("run-1", "candidate", 0, List.of(document));

        assertEquals("RetrievedDocument[documentId=doc-a]", document.toString());
        assertEquals(
                "RetrievalObservation[runId=run-1, configurationId=candidate, repetition=0, evidenceStatus=VALID, documentCount=1]",
                observation.toString());
    }
}
