package com.modelmatrix4j.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deterministic fixed-corpus retrieval fixture used to prove provider-neutral retrieval semantics. */
final class FixedCorpusFixture {

    record CorpusDocument(String documentId, String content, Optional<String> citation) {
        CorpusDocument {
            documentId = requireText(documentId, "documentId");
            content = requireText(content, "content");
            citation = Objects.requireNonNull(citation, "citation")
                    .map(value -> requireText(value, "citation"));
        }

        CorpusDocument(String documentId, String content) {
            this(documentId, content, Optional.empty());
        }
    }

    private final List<CorpusDocument> corpus;

    FixedCorpusFixture(List<CorpusDocument> corpus) {
        this.corpus = List.copyOf(Objects.requireNonNull(corpus, "corpus"));
        if (this.corpus.isEmpty()) {
            throw new IllegalArgumentException("corpus must not be empty");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (CorpusDocument document : this.corpus) {
            Objects.requireNonNull(document, "document");
            if (!ids.add(document.documentId())) {
                throw new IllegalArgumentException("duplicate documentId: " + document.documentId());
            }
        }
    }

    RetrievalObservation observe(
            String runId,
            String configurationId,
            int repetition,
            String query
    ) {
        return new RetrievalObservation(runId, configurationId, repetition, retrieve(query));
    }

    List<RetrievedDocument> retrieve(String query) {
        Set<String> queryTokens = tokens(requireText(query, "query"));
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        List<ScoredDocument> matches = new ArrayList<>();
        for (int index = 0; index < corpus.size(); index++) {
            CorpusDocument document = corpus.get(index);
            int score = overlap(queryTokens, tokens(document.content()));
            if (score > 0) {
                matches.add(new ScoredDocument(document, score, index));
            }
        }

        matches.sort(Comparator
                .comparingInt(ScoredDocument::score).reversed()
                .thenComparingInt(ScoredDocument::corpusIndex));

        return matches.stream()
                .map(match -> new RetrievedDocument(
                        match.document().documentId(),
                        match.document().citation()))
                .toList();
    }

    private static int overlap(Set<String> left, Set<String> right) {
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private static Set<String> tokens(String value) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : value.toLowerCase(Locale.ROOT).split("[^\\p{Alnum}]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private record ScoredDocument(CorpusDocument document, int score, int corpusIndex) {
    }
}
